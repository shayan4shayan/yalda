package com.shdarv.yalda.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shdarv.yalda.platform.AppSettingsStore
import com.shdarv.yalda.platform.NotificationRange
import com.shdarv.yalda.platform.NotificationSchedule
import com.shdarv.yalda.platform.NotificationStrategy
import com.shdarv.yalda.platform.NotificationTime
import com.shdarv.yalda.platform.appSettings
import com.shdarv.yalda.platform.rememberNotificationScheduler
import com.shdarv.yalda.ui.YaldaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    val strategySummary = when (strategy) {
        NotificationStrategy.ExactTimes -> if (times.isEmpty()) {
            "No reminder times"
        } else {
            times.joinToString(" | ") { it.displayLabel() }
        }
        NotificationStrategy.TimeRanges -> when (ranges.size) {
            0 -> "No time windows"
            1 -> ranges.first().displayLabel()
            else -> "${ranges.size} reminder windows"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .imePadding()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReminderHeader(
            enabled = enabled,
            strategy = strategy,
            summary = strategySummary,
            onEnabledChange = { enabled = it }
        )

        if (!scheduler.isSupported) {
            NoticeCard(
                icon = Icons.Outlined.NotificationsOff,
                title = "Notifications unavailable",
                message = "This platform does not support learning reminders."
            )
            return@Column
        }

        if (!hasPermission && scheduler.isSupported) {
            NoticeCard(
                icon = Icons.Outlined.Notifications,
                title = "Permission required",
                message = "Allow reminder permissions so scheduled words can arrive on time.",
                actionLabel = "Allow",
                onAction = { scheduler.requestPermission() }
            )
        }

        StrategySelector(
            strategy = strategy,
            onStrategyChange = { strategy = it }
        )

        if (strategy == NotificationStrategy.ExactTimes) {
            ExactTimesEditor(
                times = times,
                onAddTime = { times.add(NotificationTime(9, 0)) },
                onRemoveTime = { index -> times.removeAt(index) },
                onTimeChange = { index, time -> times[index] = time },
                onClearFocus = { focusManager.clearFocus() }
            )
        } else {
            TimeRangesEditor(
                ranges = ranges,
                onAddRange = { ranges.add(NotificationRange(9, 0, 10, 0, 15)) },
                onRemoveRange = { index -> ranges.removeAt(index) },
                onRangeChange = { index, range -> ranges[index] = range },
                onClearFocus = { focusManager.clearFocus() }
            )
        }

        if (profileId == null) {
            NoticeCard(
                icon = Icons.Outlined.NotificationsOff,
                title = "Profile required",
                message = "Select or create a profile before applying reminders."
            )
        }

        ReminderActions(
            enabled = profileId != null,
            onApply = {
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
            onSendSample = {
                if (profileId != null) {
                    scheduler.sendSample(profileId)
                }
            }
        )
    }
}

@Composable
private fun ReminderHeader(
    enabled: Boolean,
    strategy: NotificationStrategy,
    summary: String,
    onEnabledChange: (Boolean) -> Unit
) {
    val accent = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }

    ReminderCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "Daily learning reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (enabled) "Word reminders are active" else "Word reminders are paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                text = if (enabled) "On" else "Off",
                selected = enabled
            )
            StatusPill(
                text = strategy.label,
                selected = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoticeCard(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (actionLabel == null) 0.dp else 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun StrategySelector(
    strategy: NotificationStrategy,
    onStrategyChange: (NotificationStrategy) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(
            title = "Schedule strategy",
            subtitle = "Choose when a daily word can be sent"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StrategySegment(
                label = NotificationStrategy.ExactTimes.label,
                icon = Icons.Outlined.Schedule,
                selected = strategy == NotificationStrategy.ExactTimes,
                onClick = { onStrategyChange(NotificationStrategy.ExactTimes) },
                modifier = Modifier.weight(1f)
            )
            StrategySegment(
                label = NotificationStrategy.TimeRanges.label,
                icon = Icons.Outlined.Tune,
                selected = strategy == NotificationStrategy.TimeRanges,
                onClick = { onStrategyChange(NotificationStrategy.TimeRanges) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StrategySegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExactTimesEditor(
    times: List<NotificationTime>,
    onAddTime: () -> Unit,
    onRemoveTime: (Int) -> Unit,
    onTimeChange: (Int, NotificationTime) -> Unit,
    onClearFocus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorHeader(
            title = "Reminder times",
            subtitle = "Specific moments during the day",
            actionLabel = "Add",
            onAction = onAddTime
        )
        if (times.isEmpty()) {
            EmptyScheduleState(
                title = "No reminder times",
                message = "Add a time to receive a word reminder."
            )
        } else {
            times.forEachIndexed { index, time ->
                TimeCard(
                    index = index,
                    time = time,
                    onRemove = { onRemoveTime(index) },
                    onTimeChange = { onTimeChange(index, it) },
                    onClearFocus = onClearFocus
                )
            }
        }
    }
}

@Composable
private fun TimeRangesEditor(
    ranges: List<NotificationRange>,
    onAddRange: () -> Unit,
    onRemoveRange: (Int) -> Unit,
    onRangeChange: (Int, NotificationRange) -> Unit,
    onClearFocus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorHeader(
            title = "Reminder windows",
            subtitle = "Repeated reminders inside time ranges",
            actionLabel = "Add",
            onAction = onAddRange
        )
        if (ranges.isEmpty()) {
            EmptyScheduleState(
                title = "No time windows",
                message = "Add a window to repeat reminders during a part of the day."
            )
        } else {
            ranges.forEachIndexed { index, range ->
                RangeCard(
                    index = index,
                    range = range,
                    onRemove = { onRemoveRange(index) },
                    onRangeChange = { onRangeChange(index, it) },
                    onClearFocus = onClearFocus
                )
            }
        }
    }
}

@Composable
private fun TimeCard(
    index: Int,
    time: NotificationTime,
    onRemove: () -> Unit,
    onTimeChange: (NotificationTime) -> Unit,
    onClearFocus: () -> Unit
) {
    ReminderCard {
        ItemHeader(
            icon = Icons.Outlined.Schedule,
            title = "Reminder ${index + 1}",
            subtitle = time.displayLabel(),
            onRemove = onRemove
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = time.hour,
                label = "Hour",
                range = 0..23,
                imeAction = ImeAction.Next,
                onValueChange = { onTimeChange(time.copy(hour = it)) },
                onClearFocus = onClearFocus,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = time.minute,
                label = "Minute",
                range = 0..59,
                imeAction = ImeAction.Done,
                onValueChange = { onTimeChange(time.copy(minute = it)) },
                onClearFocus = onClearFocus,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RangeCard(
    index: Int,
    range: NotificationRange,
    onRemove: () -> Unit,
    onRangeChange: (NotificationRange) -> Unit,
    onClearFocus: () -> Unit
) {
    ReminderCard {
        ItemHeader(
            icon = Icons.Outlined.Tune,
            title = "Window ${index + 1}",
            subtitle = range.displayLabel(),
            onRemove = onRemove
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = range.startHour,
                label = "Start hour",
                range = 0..23,
                imeAction = ImeAction.Next,
                onValueChange = { onRangeChange(range.copy(startHour = it)) },
                onClearFocus = onClearFocus,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = range.startMinute,
                label = "Minute",
                range = 0..59,
                imeAction = ImeAction.Next,
                onValueChange = { onRangeChange(range.copy(startMinute = it)) },
                onClearFocus = onClearFocus,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = range.endHour,
                label = "End hour",
                range = 0..23,
                imeAction = ImeAction.Next,
                onValueChange = { onRangeChange(range.copy(endHour = it)) },
                onClearFocus = onClearFocus,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = range.endMinute,
                label = "Minute",
                range = 0..59,
                imeAction = ImeAction.Next,
                onValueChange = { onRangeChange(range.copy(endMinute = it)) },
                onClearFocus = onClearFocus,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        NumberField(
            value = range.intervalMinutes,
            label = "Every (min)",
            range = 1..Int.MAX_VALUE,
            imeAction = ImeAction.Done,
            onValueChange = { onRangeChange(range.copy(intervalMinutes = it)) },
            onClearFocus = onClearFocus,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditorHeader(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            title = title,
            subtitle = subtitle,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        TextButton(
            onClick = onAction,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(actionLabel)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ItemHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NumberField(
    value: Int,
    label: String,
    range: IntRange,
    imeAction: ImeAction,
    onValueChange: (Int) -> Unit,
    onClearFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    fun commitValue(nextValue: Int) {
        val coerced = nextValue.coerceIn(range)
        text = coerced.toString()
        onValueChange(coerced)
    }

    fun restoreIfBlank() {
        if (text.isBlank()) {
            text = value.toString()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { nextText ->
            val digits = nextText.filter { it.isDigit() }
            if (digits.isEmpty()) {
                text = ""
                return@OutlinedTextField
            }
            val parsed = digits.toIntOrNull() ?: return@OutlinedTextField
            val coerced = parsed.coerceIn(range)
            text = if (parsed == coerced) digits else coerced.toString()
            onValueChange(coerced)
        },
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) restoreIfBlank()
        },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            IconButton(
                onClick = { commitValue(value - 1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Decrease $label",
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { commitValue(value + 1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Increase $label",
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                restoreIfBlank()
                onClearFocus()
            },
            onDone = {
                restoreIfBlank()
                onClearFocus()
            }
        )
    )
}

@Composable
private fun EmptyScheduleState(
    title: String,
    message: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun ReminderActions(
    enabled: Boolean,
    onApply: () -> Unit,
    onSendSample: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onApply,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply", maxLines = 1)
        }
        OutlinedButton(
            onClick = onSendSample,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sample", maxLines = 1)
        }
    }
}

@Composable
private fun ReminderCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    selected: Boolean
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private val NotificationStrategy.label: String
    get() = when (this) {
        NotificationStrategy.ExactTimes -> "Exact times"
        NotificationStrategy.TimeRanges -> "Time ranges"
    }

private fun NotificationTime.displayLabel(): String = formatTime(hour, minute)

private fun NotificationRange.displayLabel(): String {
    return "${formatTime(startHour, startMinute)}-${formatTime(endHour, endMinute)}, every $intervalMinutes min"
}

private fun formatTime(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
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

@Preview
@Composable
fun PreviewLearnPage() {
    YaldaTheme {
        LearnPage(0)
    }
}
