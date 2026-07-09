package com.shdarv.yalda.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.shdarv.yalda.platform.AppSettingsStore
import com.shdarv.yalda.platform.NotificationRange
import com.shdarv.yalda.platform.NotificationSchedule
import com.shdarv.yalda.platform.NotificationStrategy
import com.shdarv.yalda.platform.NotificationTime
import com.shdarv.yalda.platform.appSettings
import com.shdarv.yalda.platform.rememberNotificationScheduler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Send

@Composable
fun LearnPage(profileId: Long?) {
    val scheduler = rememberNotificationScheduler()
    val settings = appSettings.get()
    var enabled by remember { mutableStateOf(settings.getBoolean(ENABLED_KEY, false)) }
    var strategy by remember {
        mutableStateOf(
            NotificationStrategy.values().getOrElse(settings.getInt(STRATEGY_KEY, 0)) {
                NotificationStrategy.ExactTimes
            }
        )
    }
    val times = remember {
        mutableStateListOf<NotificationTime>().apply { addAll(loadTimes(settings)) }
    }
    val ranges = remember {
        mutableStateListOf<NotificationRange>().apply { addAll(loadRanges(settings)) }
    }
    val hasPermission = scheduler.hasPermission()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .imePadding()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .padding(8.dp)
    ) {
        Text("Daily learning reminder", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (!scheduler.isSupported) {
            Text("Notifications are not supported on this platform.")
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable notifications")
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (!hasPermission && scheduler.isSupported) {
            Text("Notifications permission is required to send reminders.")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(onClick = { scheduler.requestPermission() }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Grant permission")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text("Schedule strategy", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = strategy == NotificationStrategy.ExactTimes,
                    onClick = { strategy = NotificationStrategy.ExactTimes }
                )
                Text("Exact times")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = strategy == NotificationStrategy.TimeRanges,
                    onClick = { strategy = NotificationStrategy.TimeRanges }
                )
                Text("Time ranges")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (strategy == NotificationStrategy.ExactTimes) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                times.forEachIndexed { index, time ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = time.hour.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    times[index] = time.copy(hour = it.coerceIn(0, 23))
                                }
                            },
                            label = { Text("Hour") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = time.minute.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    times[index] = time.copy(minute = it.coerceIn(0, 59))
                                }
                            },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { times.removeAt(index) },
                            enabled = true
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { times.add(NotificationTime(9, 0)) }) {
                Text("Add time")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ranges.forEachIndexed { index, range ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = range.startHour.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let {
                                        ranges[index] = range.copy(startHour = it.coerceIn(0, 23))
                                    }
                                },
                                label = { Text("Start hour") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = range.startMinute.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let {
                                        ranges[index] = range.copy(startMinute = it.coerceIn(0, 59))
                                    }
                                },
                                label = { Text("Start minute") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = range.endHour.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let {
                                        ranges[index] = range.copy(endHour = it.coerceIn(0, 23))
                                    }
                                },
                                label = { Text("End hour") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = range.endMinute.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let {
                                        ranges[index] = range.copy(endMinute = it.coerceIn(0, 59))
                                    }
                                },
                                label = { Text("End minute") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = range.intervalMinutes.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let {
                                        ranges[index] = range.copy(intervalMinutes = it.coerceAtLeast(1))
                                    }
                                },
                                label = { Text("Interval (min)") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { ranges.removeAt(index) },
                                enabled = true
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { ranges.add(NotificationRange(9, 0, 10, 0, 15)) }) {
                Text("Add range")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                settings.putBoolean(ENABLED_KEY, enabled)
                settings.putInt(STRATEGY_KEY, strategy.ordinal)
                saveTimes(settings, times.toList())
                saveRanges(settings, ranges.toList())
                if (profileId != null) {
                    scheduler.scheduleDailyWord(
                        profileId,
                        NotificationSchedule(strategy, times.toList(), ranges.toList()),
                        enabled
                    )
                }
            },
            enabled = profileId != null
        ) {
            Icon(Icons.Outlined.Check, contentDescription = "Apply schedule")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (profileId != null) {
                    scheduler.sendSample(profileId)
                }
            },
            enabled = profileId != null
        ) {
            Icon(Icons.Outlined.Send, contentDescription = "Send sample")
        }
    }
}

private const val ENABLED_KEY = "notifications.enabled"
private const val HOUR_KEY = "notifications.hour"
private const val MINUTE_KEY = "notifications.minute"
private const val STRATEGY_KEY = "notifications.strategy"
private const val TIMES_COUNT_KEY = "notifications.times.count"
private const val TIME_PREFIX = "notifications.times"
private const val RANGES_COUNT_KEY = "notifications.ranges.count"
private const val RANGE_PREFIX = "notifications.ranges"

private fun loadTimes(settings: AppSettingsStore): List<NotificationTime> {
    val count = settings.getInt(TIMES_COUNT_KEY, -1)
    if (count >= 0) {
        return (0 until count).map { index ->
            val hour = settings.getInt("$TIME_PREFIX.$index.hour", 9).coerceIn(0, 23)
            val minute = settings.getInt("$TIME_PREFIX.$index.minute", 0).coerceIn(0, 59)
            NotificationTime(hour, minute)
        }
    }
    val hour = settings.getInt(HOUR_KEY, 9).coerceIn(0, 23)
    val minute = settings.getInt(MINUTE_KEY, 0).coerceIn(0, 59)
    return listOf(NotificationTime(hour, minute))
}

private fun loadRanges(settings: AppSettingsStore): List<NotificationRange> {
    val count = settings.getInt(RANGES_COUNT_KEY, -1)
    if (count <= 0) return emptyList()
    return (0 until count).map { index ->
        NotificationRange(
            startHour = settings.getInt("$RANGE_PREFIX.$index.startHour", 9).coerceIn(0, 23),
            startMinute = settings.getInt("$RANGE_PREFIX.$index.startMinute", 0).coerceIn(0, 59),
            endHour = settings.getInt("$RANGE_PREFIX.$index.endHour", 10).coerceIn(0, 23),
            endMinute = settings.getInt("$RANGE_PREFIX.$index.endMinute", 0).coerceIn(0, 59),
            intervalMinutes = settings.getInt("$RANGE_PREFIX.$index.interval", 15).coerceAtLeast(1)
        )
    }
}

private fun saveTimes(settings: AppSettingsStore, times: List<NotificationTime>) {
    settings.putInt(TIMES_COUNT_KEY, times.size)
    times.forEachIndexed { index, time ->
        settings.putInt("$TIME_PREFIX.$index.hour", time.hour)
        settings.putInt("$TIME_PREFIX.$index.minute", time.minute)
    }
    times.firstOrNull()?.let { time ->
        settings.putInt(HOUR_KEY, time.hour)
        settings.putInt(MINUTE_KEY, time.minute)
    }
}

private fun saveRanges(settings: AppSettingsStore, ranges: List<NotificationRange>) {
    settings.putInt(RANGES_COUNT_KEY, ranges.size)
    ranges.forEachIndexed { index, range ->
        settings.putInt("$RANGE_PREFIX.$index.startHour", range.startHour)
        settings.putInt("$RANGE_PREFIX.$index.startMinute", range.startMinute)
        settings.putInt("$RANGE_PREFIX.$index.endHour", range.endHour)
        settings.putInt("$RANGE_PREFIX.$index.endMinute", range.endMinute)
        settings.putInt("$RANGE_PREFIX.$index.interval", range.intervalMinutes)
    }
}
