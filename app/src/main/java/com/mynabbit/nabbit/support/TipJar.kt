package com.mynabbit.nabbit.support

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.mynabbit.nabbit.copy.Copy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Optional tips in Settings > Keep Nabbit going, through Google Play. They're consumable:
 * nothing unlocks and there's nothing to restore, since every feature is already free.
 * The section hides until Play returns the products (they're set up in the Play Console).
 */
object TipJar {
    /** Set up in the Play Console as one-time products with these IDs. */
    val productIds = listOf("tip_small", "tip_medium", "tip_large")

    data class Tip(val id: String, val name: String, val price: String, val details: ProductDetails)

    private val _tips = MutableStateFlow<List<Tip>>(emptyList())
    val tips: StateFlow<List<Tip>> = _tips
    private val _buying = MutableStateFlow<String?>(null)
    val buying: StateFlow<String?> = _buying
    private val _thanks = MutableStateFlow<String?>(null)
    val thanks: StateFlow<String?> = _thanks

    private var client: BillingClient? = null

    /** Connects to Play, loads the tips, and finishes any tip left from an interrupted purchase. */
    fun start(context: Context) {
        if (client != null) return
        val billing = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases -> onPurchases(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        client = billing
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                load(billing)
                billing.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                ) { _, purchases -> purchases.forEach(::consume) }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun load(billing: BillingClient) {
        val products = productIds.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP).build()
        }
        billing.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(products).build(),
        ) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            _tips.value = details.productDetailsList
                .mapNotNull { d ->
                    val offer = d.oneTimePurchaseOfferDetails ?: return@mapNotNull null
                    Tip(d.productId, d.name, offer.formattedPrice, d) to offer.priceAmountMicros
                }
                .sortedBy { it.second }
                .map { it.first }
        }
    }

    fun buy(activity: Activity, tip: Tip) {
        val billing = client ?: return
        _buying.value = tip.id
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(tip.details).build()),
        ).build()
        val result = billing.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) _buying.value = null
    }

    private fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        _buying.value = null
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases?.forEach(::consume)
    }

    /** A tip that went through is used up right away, so it can be given again. */
    private fun consume(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        client?.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _thanks.value = Copy.tipThanks
        }
    }
}
