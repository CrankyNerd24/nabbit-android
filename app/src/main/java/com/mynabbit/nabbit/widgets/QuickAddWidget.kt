package com.mynabbit.nabbit.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import com.mynabbit.nabbit.MainActivity
import com.mynabbit.nabbit.NabbitApp
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.data.VerbStore
import com.mynabbit.nabbit.ui.NabbitTheme
import com.mynabbit.nabbit.ui.ThemeColor

/**
 * Catch the thought before it's gone: hot keys and verbs as notes on the home screen.
 * A verb opens Nabbit on its "Call who?"; a hot key opens right on When.
 */
class QuickAddWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(NextUpWidget.SMALL, NextUpWidget.MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        NabbitApp.ensureStarted(context)
        provideContent { QuickAdd() }
    }
}

class QuickAddReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

private data class Tile(val verbRaw: String, val title: String, val thing: String?)

@Composable
private fun QuickAdd() {
    val data by Store.data.collectAsState()
    val layout by Prefs.verbLayout.collectAsState()
    val theme = NabbitTheme.named(Prefs.themeId.collectAsState().value)
    val columns = if (LocalSize.current.width >= NextUpWidget.MEDIUM.width) 4 else 2
    val count = columns * 2
    // Hot keys take up to half, so the verbs are always there too.
    val hotKeys = data.things.filter { it.hotKey }.sortedByDescending { it.useCount }.take(count / 2)
        .map { Tile(it.verbRaw, "${it.verb.title} ${it.name}", it.name) }
    val tiles = (hotKeys + VerbStore.visible(layout).map { Tile(it.raw, it.title, null) }).take(count)

    Column(GlanceModifier.fillMaxSize().background(theme.alert.color).cornerRadius(16.dp).padding(6.dp)) {
        tiles.chunked(columns).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                row.forEachIndexed { index, tile ->
                    if (index > 0) Spacer(GlanceModifier.width(6.dp))
                    TileView(tile, if (tile.thing == null) theme.tap1 else theme.tap2)
                }
            }
        }
    }
}

@Composable
private fun androidx.glance.layout.RowScope.TileView(tile: Tile, fill: ThemeColor) {
    // A distinct data URI per tile, so each opens its own step.
    val intent = Intent(MainActivity.ACTION_QUICK_ADD)
        .setClassName("com.mynabbit.nabbit", MainActivity::class.java.name)
        .setData("nabbit://quickadd/${tile.verbRaw}/${tile.thing.orEmpty()}".toUri())
        .putExtra(MainActivity.EXTRA_VERB, tile.verbRaw)
        .putExtra(MainActivity.EXTRA_THING, tile.thing)
    Box(
        GlanceModifier.defaultWeight().fillMaxSize().background(fill.color).cornerRadius(8.dp).padding(4.dp)
            .clickable(actionStartActivity(intent))
            .semantics { contentDescription = if (tile.thing == null) "New ${tile.title} reminder" else tile.title },
        contentAlignment = Alignment.Center,
    ) {
        Text(tile.title, style = text(fill, 15, bold = true), maxLines = 2)
    }
}
