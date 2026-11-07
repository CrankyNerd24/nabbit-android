package com.mynabbit.nabbit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynabbit.nabbit.copy.Copy
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.ReminderAction

/**
 * The full-screen alert: theme-colored, the task in huge text, three buttons only.
 * Not now quiets it without doing anything, so the nudges still come.
 */
@Composable
fun AlertScreen(reminder: Reminder, theme: NabbitTheme, onAction: (ReminderAction) -> Unit, onQuiet: () -> Unit) {
    val fill = theme.alert
    val step = NudgeTiming.step(reminder.dueAt)
    val large = LocalDensity.current.fontScale >= 1.3f
    Column(
        Modifier
            .fillMaxSize()
            .background(fill.color)
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            reminder.text, color = fill.text.color, fontSize = 56.sp, lineHeight = 60.sp,
            fontWeight = FontWeight.Black, maxLines = 4, modifier = Modifier.semantics { heading() },
        )
        Text(Copy.nudge(reminder, step), color = fill.text.color, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))

        AlertButton(ReminderAction.DONE.title, theme.done, outlineOn = fill, tall = true) { onAction(ReminderAction.DONE) }
        val secondary = @Composable { action: ReminderAction, color: ThemeColor, modifier: Modifier ->
            AlertButton(action.title, color, outlineOn = fill, modifier = modifier) { onAction(action) }
        }
        if (large) {
            secondary(ReminderAction.SNOOZE_15, theme.tap1, Modifier.fillMaxWidth())
            secondary(ReminderAction.TONIGHT, theme.tap2, Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                secondary(ReminderAction.SNOOZE_15, theme.tap1, Modifier.weight(1f))
                secondary(ReminderAction.TONIGHT, theme.tap2, Modifier.weight(1f))
            }
        }
        TextButton(onClick = onQuiet, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Not now", color = fill.text.color, fontSize = 17.sp)
        }
    }
}

@Composable
private fun AlertButton(
    title: String,
    color: ThemeColor,
    outlineOn: ThemeColor,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = if (tall) 76.dp else 60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.color, contentColor = color.text.color),
        // Outlined when the button color sits too close to the alert color.
        border = if (color.contrast(outlineOn) < 3) BorderStroke(2.dp, outlineOn.text.color) else null,
    ) {
        Text(title, fontSize = if (tall) 30.sp else 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
