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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.autonetconnector.functionality.DataUsage
import com.vinnovateit.autonetconnector.functionality.SessionSummary
import com.vinnovateit.autonetconnector.screen.stats.ui.NoDataCard
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import com.vinnovateit.autonetconnector.screen.stats.utils.formatDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
            color = MaterialTheme.colorScheme.onBackground,
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

@OptIn(ExperimentalFoundationApi::class)
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
            val dayTimestamp = calendar.timeInMillis
            val key = formatDate(dayTimestamp, "dd/MM/yyyy")
            val usage = groupedByDay[key] ?: DataUsage(0, 0)
            val isToday = i == 0

            val isCurrentWeek = (todayCal.get(Calendar.WEEK_OF_YEAR) == calendar.get(Calendar.WEEK_OF_YEAR)) &&
              (todayCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR))
            val label = when {
                isToday -> "Today"
                isCurrentWeek -> formatDate(dayTimestamp, "EEE")
                else -> calendar.get(Calendar.DAY_OF_MONTH).toString()
            }

            if (usage.rxBytes + usage.txBytes > 0 || isToday) Triple(usage, label, dayTimestamp) else null
        }.reversed()
    }

    if (usableDays.isEmpty()) {
        NoDataCard("No history data yet.")
        return
    }

    val todayIdx = usableDays.lastIndex
    var selectedIndex by remember { mutableIntStateOf(todayIdx) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val totalUsageData = remember(sessions) {
        val totalRx = sessions.sumOf { it.totalData.rxBytes }
        val totalTx = sessions.sumOf { it.totalData.txBytes }
        DataUsage(totalRx, totalTx)
    }
    val totalUsageLabel = "Total Data Usage" // Changed label
    var displayedData by remember { mutableStateOf(totalUsageData to totalUsageLabel) }
    var revertJob by remember { mutableStateOf<Job?>(null) }


    val maxDailyUsage = remember(usableDays) {
        usableDays.maxOf { it.first.rxBytes + it.first.txBytes }.coerceAtLeast(1L)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val halfScreenWidth = this.maxWidth / 2
            val barWidth = 35.dp
            val horizontalPadding = halfScreenWidth - (barWidth / 2)

            LaunchedEffect(Unit) {
                lazyListState.scrollToItem(todayIdx)
                displayedData = totalUsageData to totalUsageLabel
            }

            LaunchedEffect(lazyListState) {
                snapshotFlow { lazyListState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .filter { lazyListState.isScrollInProgress }
                    .collect {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
            }

            LaunchedEffect(lazyListState) {
                snapshotFlow { lazyListState.isScrollInProgress }
                    .filter { !it }
                    .collect {
                        delay(100)
                        val layoutInfo = lazyListState.layoutInfo
                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                        val closestItem = layoutInfo.visibleItemsInfo.minByOrNull {
                            abs((it.offset + it.size / 2) - viewportCenter)
                        }
                        if (closestItem != null) {
                            if (selectedIndex != closestItem.index) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            selectedIndex = closestItem.index
                            lazyListState.animateScrollToItem(closestItem.index)
                        }
                    }
            }

            LazyRow(
                state = lazyListState,
                modifier = Modifier.height(170.dp),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                itemsIndexed(usableDays) { idx, (usage, label, timestamp) ->
                    Bar(
                        modifier = Modifier
                            .width(barWidth)
                            .fillMaxHeight(),
                        usage = usage,
                        maxUsage = maxDailyUsage,
                        dayLabel = label,
                        isSelected = (idx == selectedIndex && !lazyListState.isScrollInProgress),
                        onTap = {
                            revertJob?.cancel()
                            val dateLabel = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
                            displayedData = usage to dateLabel
                            selectedIndex = idx

                            revertJob = coroutineScope.launch {
                                delay(7000)
                                displayedData = totalUsageData to totalUsageLabel
                            }

                            coroutineScope.launch {
                                lazyListState.animateScrollToItem(idx)
                            }
                        }
                    )
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
        Text(
            dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun StatDetailRow(data: Pair<DataUsage, String>) {
    val (currentUsage, label) = data

    val totalFmt by remember(currentUsage) { derivedStateOf { formatBytes(currentUsage.rxBytes + currentUsage.txBytes) } }
    val dlFmt by remember(currentUsage) { derivedStateOf { formatBytes(currentUsage.rxBytes) } }
    val ulFmt by remember(currentUsage) { derivedStateOf { formatBytes(currentUsage.txBytes) } }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnimatedContent(dlFmt, label = "DLStat") { (value, unit) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFF0089D0), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$value $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            AnimatedContent(ulFmt, label = "ULStat") { (value, unit) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFFFFA500), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$value $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
