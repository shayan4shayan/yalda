package com.shdarv.yalda.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.db.HomeActivityDay
import com.shdarv.yalda.db.HomeStatistics
import com.shdarv.yalda.db.auditNow
import com.shdarv.yalda.db.database
import com.shdarv.yalda.localization.LocalAppStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

private sealed interface HomeStatisticsState {
    data object Loading : HomeStatisticsState
    data object Failed : HomeStatisticsState
    data class Ready(val totals: HomeStatistics, val days: List<HomeActivityDay>) : HomeStatisticsState
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
internal fun HomeStatisticsSection(profileId: Long) {
    val strings = LocalAppStrings.current
    var retry by remember { mutableStateOf(0) }
    val state by produceState<HomeStatisticsState>(HomeStatisticsState.Loading, profileId, retry) {
        value = HomeStatisticsState.Loading
        val dao = database.get().auditDao()
        // Refresh the day window after midnight/resume even when no new events are written.
        val days = flow {
            while (true) { emit(auditNow()); delay(60_000) }
        }.flatMapLatest { dao.homeActivity(profileId, it) }
        combine(dao.homeStatistics(profileId), days) { totals, activity ->
            HomeStatisticsState.Ready(totals, activity)
        }.catch { value = HomeStatisticsState.Failed }
            .collect { value = it }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings("Your activity"), style = MaterialTheme.typography.titleLarge)
            Text(strings("All time"), style = MaterialTheme.typography.labelMedium)
        }
        when (val current = state) {
            HomeStatisticsState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            HomeStatisticsState.Failed -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(strings("Could not load activity."))
                    TextButton(onClick = { retry++ }) { Text(strings("Retry")) }
                }
            }
            is HomeStatisticsState.Ready -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatisticCard(strings("Reminders sent"), current.totals.remindersSent, Modifier.weight(1f))
                    StatisticCard(strings("Questions answered"), current.totals.questionsAnswered, Modifier.weight(1f))
                }
                AnswerBalance(current.totals)
                DailyActivityChart(current.days)
            }
        }
    }
}

@Composable
private fun StatisticCard(label: String, count: Long, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AnswerBalance(totals: HomeStatistics) {
    val strings = LocalAppStrings.current
    val correctColor = MaterialTheme.colorScheme.primary
    val incorrectColor = MaterialTheme.colorScheme.error
    val fraction = totals.correctFraction
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings("Answer accuracy"), style = MaterialTheme.typography.titleMedium)
            Text(
                if (fraction == null) "—" else strings("{0}% correct", (fraction * 100).roundToInt()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).clearAndSetSemantics { }) {
                if (fraction != null) {
                    if (totals.correctAnswers > 0) Box(Modifier.weight(totals.correctAnswers.toFloat()).fillMaxHeight().background(correctColor))
                    if (totals.incorrectAnswers > 0) Box(Modifier.weight(totals.incorrectAnswers.toFloat()).fillMaxHeight().background(incorrectColor))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings("Correct: {0}", totals.correctAnswers), Modifier.weight(1f), color = correctColor)
                Text(strings("Incorrect: {0}", totals.incorrectAnswers), Modifier.weight(1f), color = incorrectColor)
            }
            Text(
                strings(if (fraction == null) "Answer your first quiz question to see your accuracy." else "Based on your first answer to each question."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyActivityChart(days: List<HomeActivityDay>) {
    if (days.isEmpty()) return
    val strings = LocalAppStrings.current
    val questionColor = MaterialTheme.colorScheme.primary
    val reminderColor = MaterialTheme.colorScheme.tertiary
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = days.firstOrNull { it.localDate == selectedDate } ?: days.last()
    val maxCount = days.maxOf { maxOf(it.questionsAnswered, it.remindersSent) }.coerceAtLeast(1)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings("Daily activity"), style = MaterialTheme.typography.titleMedium)
            Text(strings("Last 7 days"), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartLegend(strings("Questions"), questionColor, Modifier.weight(1f))
                ChartLegend(strings("Reminders"), reminderColor, Modifier.weight(1f))
            }
            Text(maxCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Time runs left to right in every locale; surrounding text retains its own direction.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    days.forEach { day ->
                        val label = if (day == days.last()) strings("Today") else day.localDate.substring(5).replace('-', '/')
                        val description = strings("{0}: {1} questions, {2} reminders", day.localDate, day.questionsAnswered, day.remindersSent)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (day == selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .selectable(day == selected, role = Role.Tab, onClick = { selectedDate = day.localDate })
                                .padding(horizontal = 3.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                Modifier.fillMaxWidth().height(112.dp).clearAndSetSemantics { contentDescription = description },
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                ActivityBar(day.questionsAnswered, maxCount, questionColor, Modifier.weight(1f))
                                ActivityBar(day.remindersSent, maxCount, reminderColor, Modifier.weight(1f))
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(6.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Text(
                strings("{0}: {1} questions, {2} reminders", selected.localDate, selected.questionsAnswered, selected.remindersSent),
                style = MaterialTheme.typography.bodySmall
            )
            if (days.all { it.questionsAnswered == 0L && it.remindersSent == 0L }) {
                Text(strings("No activity yet. Your reminders and quiz answers will appear here."), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActivityBar(count: Long, maxCount: Long, color: Color, modifier: Modifier) {
    Box(modifier.height(if (count == 0L) 0.dp else (112 * count.toFloat() / maxCount).coerceAtLeast(2f).dp)
        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(color))
}

@Composable
private fun ChartLegend(label: String, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
