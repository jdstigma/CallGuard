package com.callguard.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A wedge of the pie / a segment for the legend. */
data class Slice(val label: String, val value: Int, val color: Color)

/** One horizontal bar. */
data class Bar(val label: String, val value: Int, val color: Color)

/** Simple pie chart drawn with Compose Canvas — no external library. */
@Composable
fun PieChart(slices: List<Slice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.toFloat()
    Canvas(modifier) {
        var start = -90f
        slices.forEach { s ->
            val sweep = if (total > 0f) 360f * (s.value / total) else 0f
            drawArc(
                color = s.color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
            )
            start += sweep
        }
    }
}

/** Colored-square legend to sit beside a pie chart. */
@Composable
fun ChartLegend(slices: List<Slice>) {
    Column {
        slices.forEach { s ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Box(Modifier.size(12.dp).background(s.color))
                Spacer(Modifier.width(8.dp))
                Text("${s.label}: ${s.value}", fontSize = 13.sp)
            }
        }
    }
}

/** Horizontal bar chart — bar width is proportional to the largest value. */
@Composable
fun BarChart(bars: List<Bar>) {
    val max = (bars.maxOfOrNull { it.value } ?: 0).toFloat()
    Column(Modifier.fillMaxWidth()) {
        bars.forEach { b ->
            Text("${b.label} — ${b.value}", fontSize = 13.sp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(Color(0xFFEEEEEE))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (max > 0f) b.value / max else 0f)
                        .height(18.dp)
                        .background(b.color)
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
