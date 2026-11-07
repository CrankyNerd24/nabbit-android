package com.mynabbit.nabbit.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A big sticky note: the verbs, things, and when chips. Presses down a little so the tap registers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyNote(
    text: String,
    fill: ThemeColor,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .scale(if (pressed) 0.95f else 1f)
            .shadow(3.dp, RoundedCornerShape(6.dp))
            .background(fill.color, RoundedCornerShape(6.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .heightIn(min = if (compact) 52.dp else 92.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            color = fill.text.color,
            fontSize = if (compact) 17.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = if (compact) 21.sp else 28.sp,
        )
    }
}

/** Two notes to a row, or one at the largest text sizes so words don't break apart. */
@Composable
fun NoteGrid(count: Int, item: @Composable (Int, Modifier) -> Unit) {
    val columns = if (LocalDensity.current.fontScale >= 1.6f) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0 until (count + columns - 1) / columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (column in 0 until columns) {
                    val index = row * columns + column
                    if (index < count) item(index, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** A task with its time: side by side when both fit, otherwise the time goes under. */
@Composable
fun TaskAndTime(task: String, time: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val stacked = LocalDensity.current.fontScale >= 1.3f
    if (stacked) {
        Column(modifier) {
            Text(task, color = color, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(time, color = color, fontSize = 15.sp, maxLines = 1)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                task, color = color, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(time, color = color, fontSize = 15.sp, maxLines = 1)
        }
    }
}

/** A short line at the bottom of Home, with Undo where it helps. Read out by TalkBack. */
@Composable
fun BannerView(text: String, fill: ThemeColor, onUndo: (() -> Unit)?, onTap: () -> Unit) {
    val stacked = LocalDensity.current.fontScale >= 1.3f
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(fill.color, RoundedCornerShape(12.dp))
            .combinedClickableCompat(onTap)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val line = @Composable { mod: Modifier ->
            Text(
                text, color = fill.text.color, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                textAlign = if (onUndo == null) TextAlign.Center else TextAlign.Start, modifier = mod,
            )
        }
        val undo = @Composable {
            if (onUndo != null) {
                TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Undo", color = fill.text.color, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }
        }
        if (stacked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                line(Modifier.fillMaxWidth())
                undo()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                line(Modifier.weight(1f))
                undo()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick)
