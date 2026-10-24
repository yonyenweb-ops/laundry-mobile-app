package com.laundryhub.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laundryhub.mobile.ui.theme.Amber
import com.laundryhub.mobile.ui.theme.AmberSoft
import com.laundryhub.mobile.ui.theme.Blue
import com.laundryhub.mobile.ui.theme.BlueSoft
import com.laundryhub.mobile.ui.theme.Green
import com.laundryhub.mobile.ui.theme.GreenSoft
import com.laundryhub.mobile.ui.theme.Ink
import com.laundryhub.mobile.ui.theme.Line
import com.laundryhub.mobile.ui.theme.Muted
import com.laundryhub.mobile.ui.theme.Red
import com.laundryhub.mobile.ui.theme.RedSoft

private val PanelShape = RoundedCornerShape(16.dp)

/** White rounded box with a soft shadow, used everywhere. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, PanelShape, ambientColor = Ink.copy(alpha = 0.08f), spotColor = Ink.copy(alpha = 0.08f))
            .background(Color.White, PanelShape)
            .border(1.dp, Line.copy(alpha = 0.6f), PanelShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Small colored label, e.g. order status. */
@Composable
fun Chip(text: String, tone: Tone = Tone.Blue) {
    val (bg, fg) = when (tone) {
        Tone.Blue -> BlueSoft to Blue
        Tone.Green -> GreenSoft to Green
        Tone.Red -> RedSoft to Red
        Tone.Amber -> AmberSoft to Amber
        Tone.Gray -> Color(0xFFF2F4F7) to Muted
    }
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

enum class Tone { Blue, Green, Red, Amber, Gray }

fun statusTone(status: String): Tone = when (status) {
    "DELIVERED" -> Tone.Green
    "CANCELLED" -> Tone.Red
    "AWAITING_SHOP" -> Tone.Amber
    else -> Tone.Blue
}

@Composable
fun StatusChip(status: String) = Chip(statusLabel(status), statusTone(status))

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/** Label on the left, value on the right. */
@Composable
fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Error message with a "Try again" button. */
@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = Red, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun EmptyBox(text: String) {
    Text(
        text,
        color = Muted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    )
}

/** Yellow banner, e.g. "waiting for admin verification". */
@Composable
fun Banner(text: String, tone: Tone = Tone.Amber) {
    val (bg, fg) = when (tone) {
        Tone.Red -> RedSoft to Red
        Tone.Green -> GreenSoft to Green
        Tone.Blue -> BlueSoft to Blue
        else -> AmberSoft to Amber
    }
    Text(
        text,
        color = fg,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}

/** Full-width main button that shows a spinner while busy. */
@Composable
fun BigButton(text: String, busy: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 5 stars you can tap (FR-09). */
@Composable
fun StarPicker(label: String, stars: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, color = Muted)
        Row {
            for (i in 1..5) {
                Text(
                    text = if (i <= stars) "★" else "☆",
                    fontSize = 32.sp,
                    color = if (i <= stars) Color(0xFFF59E0B) else Muted,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickableNoRipple { onChange(i) },
                )
            }
        }
    }
}

val ScreenPadding = PaddingValues(16.dp)
