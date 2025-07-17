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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import com.vinnovateit.autonetconnector.screen.stats.ui.StatItem
import com.vinnovateit.autonetconnector.screen.stats.utils.formatBytes
import com.vinnovateit.autonetconnector.screen.stats.utils.formatDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
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
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val maxDailyUsage = remember(usableDays) {
        usableDays.maxOf { it.first.rxBytes + it.first.txBytes }.coerceAtLeast(1L)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val halfScreenWidth = this.maxWidth / 2
            val barWidth = 35.dp
            val horizontalPadding = halfScreenWidth - (barWidth / 2)

            // Perform initial scroll to the last item (today)
            LaunchedEffect(Unit) {
                lazyListState.scrollToItem(todayIdx)
            }

            // Haptic feedback during casual scroll
            var currentlyCenteredItem by remember { mutableIntStateOf(-1) }
            LaunchedEffect(lazyListState) {
                snapshotFlow {
                    val layoutInfo = lazyListState.layoutInfo
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    layoutInfo.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index ?: -1
                }
                    .distinctUntilChanged()
                    .collect { centeredIndex ->
                        if (centeredIndex != -1 && currentlyCenteredItem != centeredIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentlyCenteredItem = centeredIndex
                        }
                    }
            }

            // Magnetic snap after scrolling stops
            LaunchedEffect(lazyListState) {
                snapshotFlow { lazyListState.isScrollInProgress }
                    .filter { !it }
                    .collect {
                        val layoutInfo = lazyListState.layoutInfo
                        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                        val closestItem = layoutInfo.visibleItemsInfo.minByOrNull {
                            abs((it.offset + it.size / 2) - viewportCenter)
                        }
                        if (closestItem != null) {
                            if (selectedIndex != closestItem.index) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Haptic on lock
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
                itemsIndexed(usableDays) { idx, (usage, label, _) ->
                    Bar(
                        modifier = Modifier
                            .width(barWidth)
                            .fillMaxHeight(),
                        usage = usage,
                        maxUsage = maxDailyUsage,
                        dayLabel = label,
                        isSelected = idx == selectedIndex,
                        onTap = {
                            selectedIndex = idx
                            scope.launch {
                                lazyListState.animateScrollToItem(idx)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = usableDays.getOrNull(selectedIndex),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "UsageDetails"
        ) { selectedTriple ->
            if (selectedTriple != null) {
                val (usage, _, timestamp) = selectedTriple
                val formattedDate = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
                StatDetailRow(data = usage to formattedDate)
            } else {
                Box(modifier = Modifier.height(60.dp))
            }
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
fun StatDetailRow(data: Pair<DataUsage, String>) {
    val (currentUsage, label) = data

    // This state is completely internal to StatDetailRow.
    // It's initialized once when a new day is selected.
    val displayedUsage = remember(currentUsage) { mutableStateOf(currentUsage) }

    // This effect runs the 5-second lazy update loop.
    // It is keyed to `displayedUsage`, so it only restarts when the user selects a DIFFERENT day,
    // not from the 1-second live data updates.
    LaunchedEffect(displayedUsage) {
        while (isActive) {
            // Check if the rounded value of the LIVE data is different from the DISPLAYED data.
            val liveFormattedTotal = formatBytes(displayedUsage.value.rxBytes + displayedUsage.value.txBytes)
            val displayedFormattedTotal = formatBytes(currentUsage.rxBytes + currentUsage.txBytes)

            // ONLY update the internal state if the formatted, visible text has changed.
            if (liveFormattedTotal != displayedFormattedTotal) {
                displayedUsage.value = currentUsage
            }
            delay(5000)
        }
    }

    // The animations are now driven by the stable, lazily-updated 'displayedUsage' state.
    val totalFmt by remember { derivedStateOf { formatBytes(displayedUsage.value.rxBytes + displayedUsage.value.txBytes) } }
    val dlFmt by remember { derivedStateOf { formatBytes(displayedUsage.value.rxBytes) } }
    val ulFmt by remember { derivedStateOf { formatBytes(displayedUsage.value.txBytes) } }


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