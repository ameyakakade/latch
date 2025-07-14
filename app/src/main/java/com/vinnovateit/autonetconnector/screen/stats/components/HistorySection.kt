// path: com/vinnovateit/autonetconnector/screen/stats/components/HistorySection.kt
package com.vinnovateit.autonetconnector.screen.stats.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.DataUsage
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.ui.NoDataCard
import com.vinnovateit.autonetconnector.screen.stats.ui.StatItem
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import com.vinnovateit.autonetconnector.screen.stats.utils.formatDate
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistorySection(history: List<SessionSummary>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        if (history.isNotEmpty()) {
            HistoryBarChart(sessions = history)
        } else {
            NoDataCard("No history data yet.")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryBarChart(sessions: List<SessionSummary>) {
    val calendar = remember { Calendar.getInstance() }
    val todayCal = remember { Calendar.getInstance().apply { time = Date() } }
    val groupedByDay = remember(sessions) {
        sessions.groupBy {
            calendar.timeInMillis = it.startTimestamp
            formatDate(calendar.timeInMillis, "dd/MM/yyyy")
        }.mapValues { (_, list) ->
            DataUsage(
                rxBytes = list.sumOf { it.totalData.rxBytes },
                txBytes = list.sumOf { it.totalData.txBytes }
            )
        }
    }

    val usableDays = remember(groupedByDay) {
        (0..29).mapNotNull { i ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val key = formatDate(calendar.timeInMillis, "dd/MM/yyyy")
            val usage = groupedByDay[key] ?: DataUsage(0, 0)
            val isToday = i == 0
            val isCurrentWeek = (todayCal.get(Calendar.WEEK_OF_YEAR) == calendar.get(Calendar.WEEK_OF_YEAR)) &&
                    (todayCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR))
            val label = when {
                isToday -> "Today"
                isCurrentWeek -> formatDate(calendar.timeInMillis, "EEE")
                else -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            }
            if (usage.rxBytes + usage.txBytes > 0) usage to label else null
        }.reversed()
    }

    if (usableDays.isEmpty()) {
        NoDataCard("No history data yet.")
        return
    }

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val barWidthDp = 35.dp
    val spacingDp = 10.dp
    val edgePadding = barWidthDp + spacingDp
    val viewportWidthDp = configuration.screenWidthDp.dp

    val todayIdx = usableDays.lastIndex

    var centeredIndex by remember { mutableIntStateOf(todayIdx) }
    var showTotalUsage by remember { mutableStateOf(false) }
    var timerKey by remember { mutableIntStateOf(0) }

    val barWidthPx = with(density) { barWidthDp.toPx() }
    val spacingPx = with(density) { spacingDp.toPx() }
    val edgePaddingPx = with(density) { edgePadding.toPx() }
    val viewportWidthPx = with(density) { viewportWidthDp.toPx() }

    val todayLazyRowIndex = todayIdx + 1
    val centerTodayOffsetPx = edgePaddingPx + todayIdx * (barWidthPx + spacingPx)
    val maxScrollPx = centerTodayOffsetPx - (viewportWidthPx / 2 - barWidthPx / 2)

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val layoutInfo = listState.layoutInfo
        val centerPx = layoutInfo.viewportSize.width / 2
        val visibleItems = layoutInfo.visibleItemsInfo
        val centered = visibleItems.minByOrNull { item ->
            kotlin.math.abs(item.offset + item.size / 2 - centerPx)
        }?.index?.minus(1) ?: centeredIndex

        if (centered != centeredIndex && centered in usableDays.indices) {
            centeredIndex = centered
            showTotalUsage = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            timerKey++
        }

        val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
        if (lastItem != null && listState.firstVisibleItemIndex + lastItem.index >= todayLazyRowIndex) {
            val totalPx = (listState.firstVisibleItemIndex * (barWidthPx + spacingPx)) + listState.firstVisibleItemScrollOffset
            if (totalPx > maxScrollPx + 1) {
                listState.animateScrollToItem(todayLazyRowIndex, maxScrollPx.toInt())
            }
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(todayLazyRowIndex, maxScrollPx.toInt())
    }

    LaunchedEffect(timerKey) {
        delay(3000)
        showTotalUsage = true
    }

    val maxDailyUsage = remember(usableDays) {
        usableDays.maxOf { it.first.rxBytes + it.first.txBytes }.coerceAtLeast(1L)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(spacingDp),
            verticalAlignment = Alignment.Bottom
        ) {
            item { Spacer(Modifier.width(edgePadding)) }
            itemsIndexed(usableDays) { idx, (usage, label) ->
                val isCentered = idx == centeredIndex
                val barScale by animateFloatAsState(
                    targetValue = if (isCentered) 1.1f else 1f,
                    animationSpec = tween(200),
                    label = "BarScaleAnimation"
                )
                Bar(
                    modifier = Modifier
                        .width(barWidthDp)
                        .fillMaxHeight()
                        .scale(barScale),
                    usage = usage,
                    maxUsage = maxDailyUsage,
                    dayLabel = label,
                    isSelected = isCentered,
                    onTap = { /* Optional */ }
                )
            }
            item { Spacer(Modifier.width(viewportWidthDp / 2 - barWidthDp / 2)) }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = if (showTotalUsage) null else usableDays.getOrNull(centeredIndex),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "UsageDetails"
        ) { selectedDay ->
            val totalUsage = DataUsage(
                rxBytes = sessions.sumOf { it.totalData.rxBytes },
                txBytes = sessions.sumOf { it.totalData.txBytes }
            )
            val displayDate = selectedDay?.let {
                val sessionIdx = sessions.indexOfFirst { s -> s.totalData == it.first }
                val timestamp = sessions.getOrNull(sessionIdx)?.startTimestamp
                timestamp?.let { ts ->
                    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
                }
            } ?: "Total"

            StatDetailRow(
                data = selectedDay?.let { it.first to displayDate },
                totalUsage = totalUsage
            )
        }
    }
}

@Composable
fun Bar(
    modifier: Modifier = Modifier,
    usage: DataUsage,
    maxUsage: Long,
    dayLabel: String,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val total = usage.rxBytes + usage.txBytes
    val heightFrac by animateFloatAsState(
        targetValue = if (maxUsage > 0) total.toFloat() / maxUsage else 0f,
        animationSpec = tween(500),
        label = "BarHeight"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(200),
        label = "BarScale"
    )

    val ulPart = if (total > 0) usage.txBytes.toFloat() / total else 0f
    val dlColor = Color(0xFF0089D0)
    val ulColor = Color(0xFFFFA500)

    Column(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            val maxH = this.maxHeight * .8f
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(maxH * heightFrac)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (usage.rxBytes > 0) Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f - ulPart)
                        .background(dlColor)
                )
                if (usage.txBytes > 0) Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(ulPart)
                        .background(ulColor)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(dayLabel, style = MaterialTheme.typography.labelSmall)
    }
}


@Composable
fun StatDetailRow(
    data: Pair<DataUsage, String>?,
    totalUsage: DataUsage
) {
    val (usage, label) = data ?: (totalUsage to "Total Usage")

    val totalFmt by remember(usage) {
        mutableStateOf(formatBytes(usage.rxBytes + usage.txBytes))
    }
    val dlFmt by remember(usage) { mutableStateOf(formatBytes(usage.rxBytes)) }
    val ulFmt by remember(usage) { mutableStateOf(formatBytes(usage.txBytes)) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = totalFmt,
            transitionSpec = {
                slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut()
            },
            label = "TotalUsageSwitch"
        ) { (v, u) ->
            Text(
                "$v $u",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedContent(dlFmt, label = "DLStat") {
                StatItem(Icons.Default.ArrowDownward, it, Color(0xFF0089D0))
            }
            AnimatedContent(ulFmt, label = "ULStat") {
                StatItem(Icons.Default.ArrowUpward, it, Color(0xFFFFA500))
            }
        }
    }
}