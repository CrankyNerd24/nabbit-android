package com.mynabbit.nabbit.celebration

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynabbit.nabbit.ui.NabbitTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Whether the person turned off animations (Settings > Accessibility > Remove animations). */
fun reduceMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * Confetti in the theme's colors, sized to the effort. With animations off it's a color
 * flash instead. Never takes taps, so the next tap always goes through.
 */
@Composable
fun CelebrationOverlay(celebration: Celebration, theme: NabbitTheme) {
    val context = LocalContext.current
    val progress = remember(celebration.id) { Animatable(0f) }
    LaunchedEffect(celebration.id) {
        progress.animateTo(1f, tween(celebration.size.millis.toInt(), easing = LinearEasing))
    }
    val colors = listOf(theme.tap1, theme.tap2, theme.tap3, theme.done).map { it.color }

    if (celebration.size == Celebration.Size.CLEAR_BOARD) {
        // The empty fridge: nothing left today.
        Box(
            Modifier.fillMaxSize().background(theme.done.color.copy(alpha = 0.94f * (1f - progress.value * progress.value))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text(
                    celebration.line, color = theme.done.text.color, fontSize = 34.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, lineHeight = 40.sp,
                )
            }
        }
        return
    }

    if (reduceMotion(context)) {
        Box(Modifier.fillMaxSize().background(theme.done.color.copy(alpha = 0.35f * (1f - progress.value))))
        return
    }

    val count = when (celebration.size) {
        Celebration.Size.SMALL -> 24
        Celebration.Size.MEDIUM -> 60
        else -> 140
    }
    val pieces = remember(celebration.id) {
        List(count) {
            val angle = Random.nextDouble(-Math.PI * 0.95, -Math.PI * 0.05)
            val speed = Random.nextDouble(0.5, 1.2)
            Piece(
                dx = (cos(angle) * speed).toFloat(),
                dy = (sin(angle) * speed).toFloat(),
                color = it % colors.size,
                spin = Random.nextFloat() * 720f - 360f,
                width = Random.nextFloat() * 8f + 6f,
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        val origin = if (celebration.size == Celebration.Size.BIG) Offset(size.width / 2, size.height * 0.75f)
        else Offset(size.width / 2, size.height * 0.55f)
        val reach = if (celebration.size == Celebration.Size.BIG) size.height * 0.9f else size.height * 0.45f
        for (piece in pieces) {
            val x = origin.x + piece.dx * reach * t
            val y = origin.y + piece.dy * reach * t + size.height * 0.9f * t * t
            rotate(piece.spin * t, Offset(x, y)) {
                drawRect(
                    colors[piece.color].copy(alpha = 1f - t * t),
                    topLeft = Offset(x, y),
                    size = Size(piece.width * density / 2, piece.width * density),
                )
            }
        }
    }
}

private data class Piece(val dx: Float, val dy: Float, val color: Int, val spin: Float, val width: Float)

/** The firm thump and double tap on Done, lighter for routine things. */
object Haptics {
    fun play(context: Context, size: Celebration.Size) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: return
        if (!vibrator.hasVibrator()) return
        val effect = when (size) {
            Celebration.Size.SMALL -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            else -> VibrationEffect.createWaveform(longArrayOf(0, 40, 90, 25, 60, 25), intArrayOf(0, 255, 0, 160, 0, 160), -1)
        }
        vibrator.vibrate(effect)
    }

    fun tap(context: Context) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

}
