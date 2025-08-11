package com.vinnovateit.latch.features.stats.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.common.util.NoDataCard
import com.vinnovateit.latch.common.util.formatBytes
import com.vinnovateit.latch.domain.model.DataUsage
import com.vinnovateit.latch.ui.theme.ColorGraphDownload
import com.vinnovateit.latch.ui.theme.ColorGraphUpload
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Immutable
sealed class HistoryChartItem {
    data class BarData(val usage: DataUsage, val label: String, val timestamp: Long) : HistoryChartItem()
    data class MonthSeparator(val monthName: String) : HistoryChartItem()
}

@Composable
fun HistoryBarChart(history: List<HistoryChartItem>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Daily Usage",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 8.dp)
        )
        if (history.isNotEmpty()) {
            HistoryBarChartContent(chartItems = history)
        } else {
            NoDataCard("No session history available.")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryBarChartContent(chartItems: List<HistoryChartItem>) {

    if (chartItems.filterIsInstance<HistoryChartItem.BarData>().all { it.usage.rxBytes + it.usage.txBytes == 0L }) {
        NoDataCard("No history data yet.")
        return
    }

    val todayIdx = chartItems.indexOfLast { it is HistoryChartItem.BarData }
    var selectedIndex by remember { mutableIntStateOf(todayIdx) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dateFormatter = remember { SimpleDateFormat("E, dd MMM", Locale.getDefault()) }

    val totalUsageData = remember(chartItems) {
        val totalRx = chartItems.filterIsInstance<HistoryChartItem.BarData>().sumOf { it.usage.rxBytes }
        val totalTx = chartItems.filterIsInstance<HistoryChartItem.BarData>().sumOf { it.usage.txBytes }
        DataUsage(totalRx, totalTx)
    }
    val totalUsageLabel = "Total Data Usage"
    var displayedData by remember { mutableStateOf(totalUsageData to totalUsageLabel) }
    var revertJob by remember { mutableStateOf<Job?>(null) }

    val maxDailyUsage = remember(chartItems) {
        chartItems.filterIsInstance<HistoryChartItem.BarData>()
            .maxOfOrNull { it.usage.rxBytes + it.usage.txBytes }
            ?.coerceAtLeast(1L) ?: 1L
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo }
            .map { layoutInfo ->
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull {
                    val itemCenter = it.offset + it.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index ?: -1
            }
            .distinctUntilChanged()
            .collect { centerIndex ->
                if (centerIndex != -1 && selectedIndex != centerIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    selectedIndex = centerIndex

                    val item = chartItems[centerIndex]
                    if (item is HistoryChartItem.BarData) {
                        displayedData = item.usage to dateFormatter.format(Date(item.timestamp))
                        revertJob?.cancel()
                        revertJob = coroutineScope.launch {
                            delay(7000)
                            displayedData = totalUsageData to totalUsageLabel
                        }
                    }
                }
            }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress && selectedIndex != -1) {
            delay(100)
            lazyListState.scrollToItem(selectedIndex)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val halfScreenWidth = this.maxWidth / 2
            val barWidth = 35.dp
            val rowHeight = 170.dp
            val barAreaHeight = rowHeight * 0.8f
            val horizontalPadding = halfScreenWidth - (barWidth / 2)

            LaunchedEffect(Unit) {
                if (todayIdx != -1) {
                    lazyListState.scrollToItem(todayIdx)
                }
                displayedData = totalUsageData to totalUsageLabel
            }

            LazyRow(
                state = lazyListState,
                modifier = Modifier.height(rowHeight),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                itemsIndexed(chartItems, key = { index, item ->
                    when (item) {
                        is HistoryChartItem.BarData -> "bar_${item.timestamp}"
                        is HistoryChartItem.MonthSeparator -> "month_${item.monthName}_$index"
                    }
                }) { idx, item ->
                    when (item) {
                        is HistoryChartItem.BarData -> {
                            Bar(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(),
                                usage = item.usage,
                                maxUsage = maxDailyUsage,
                                dayLabel = item.label,
                                isSelected = (idx == selectedIndex),
                                barAreaHeight = barAreaHeight,
                                onTap = {
                                    // Tapping now just instantly scrolls to the item.
                                    if (selectedIndex != idx) {
                                        coroutineScope.launch {
                                            lazyListState.scrollToItem(idx)
                                        }
                                    }
                                }
                            )
                        }
                        is HistoryChartItem.MonthSeparator -> {
                            MonthSeparator(monthName = item.monthName)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = displayedData,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "UsageDetails"
        ) { data ->
            StatDetailRow(data = data)
        }
    }
}

@Composable
private fun MonthSeparator(monthName: String) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monthName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(-90f)
        )
    }
}


@Composable
private fun Bar(
    modifier: Modifier = Modifier,
    usage: DataUsage,
    maxUsage: Long,
    dayLabel: String,
    isSelected: Boolean,
    barAreaHeight: Dp,
    onTap: () -> Unit
) {
    val total = usage.rxBytes + usage.txBytes
    val rawFrac = if (maxUsage > 0) total.toFloat() / maxUsage else 0f

    val heightFrac by animateFloatAsState(
        targetValue = rawFrac.coerceAtLeast(0.1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BarHeight"
    )

    val uploadFrac = if (total > 0) usage.txBytes.toFloat() / total else 0f
    val downloadFrac = 1f - uploadFrac
    val density = LocalDensity.current
    val barHeightInDp = with(density) { (barAreaHeight.toPx() * heightFrac).toDp() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barAreaHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeightInDp)
                    .clip(RoundedCornerShape(25.dp))
            ) {
                if (total > 0) {
                    val w = size.width
                    val h = size.height
                    val dlH = h * downloadFrac
                    val ulH = h * uploadFrac

                    if (dlH > 0) {
                        drawRect(
                            color = ColorGraphDownload,
                            topLeft = Offset(0f, 0f),
                            size = Size(w, dlH)
                        )
                    }
                    if (ulH > 0) {
                        drawRect(
                            color = ColorGraphUpload,
                            topLeft = Offset(0f, dlH),
                            size = Size(w, ulH)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StatDetailRow(data: Pair<DataUsage, String>) {
    val (currentUsage, label) = data

    val (totalFmt, dlFmt, ulFmt) = remember(currentUsage) {
        Triple(
            formatBytes(currentUsage.rxBytes + currentUsage.txBytes),
            formatBytes(currentUsage.rxBytes),
            formatBytes(currentUsage.txBytes)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = totalFmt,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith
                  (slideOutVertically { -it } + fadeOut())
            },
            label = "TotalUsageSwitch"
        ) { (v, u) ->
            Text(
                "$v $u",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedContent(dlFmt, label = "DLStat", transitionSpec = { fadeIn() togetherWith fadeOut() }) { (value, unit) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = ColorGraphDownload, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$value $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedContent(ulFmt, label = "ULStat", transitionSpec = { fadeIn() togetherWith fadeOut() }) { (value, unit) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, null, tint = ColorGraphUpload, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$value $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
