package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppMode
import com.example.data.UserRole
import com.example.ui.OvertimeUiState
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.EmeraldTealDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusRejected
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay

private enum class LogInputMode {
    LIVE_TIMER,
    MANUAL
}

private data class TimePreset(
    val label: String,
    val startH: Int,
    val startM: Int,
    val endH: Int,
    val endM: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOvertimeScreen(
    state: OvertimeUiState,
    onNavigateBack: () -> Unit,
    onStartLiveTimer: () -> Unit,
    onCancelLiveTimer: () -> Unit,
    onStopAndSaveLiveTimer: (notes: String) -> Unit,
    onSaveManual: (dateIso: String, startH: Int, startM: Int, endH: Int, endM: Int, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val strings = state.strings
    val profile = state.profile

    // Live ticking wall clock (updates every 1 second so user sees real running time!)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val isTimerRunning = state.activeOvertimeStartMillis != null
    var inputMode by rememberSaveable { mutableStateOf(LogInputMode.LIVE_TIMER) }

    var selectedDateIso by rememberSaveable { mutableStateOf(state.draftDateIso) }
    LaunchedEffect(state.draftDateIso) {
        selectedDateIso = state.draftDateIso
    }

    val initialNowParts = remember { DateTimeUtils.currentTimeParts() }
    var startHour by rememberSaveable { mutableIntStateOf(initialNowParts.first) }
    var startMinute by rememberSaveable { mutableIntStateOf(initialNowParts.second) }
    var endHour by rememberSaveable { mutableIntStateOf((initialNowParts.first + 2) % 24) }
    var endMinute by rememberSaveable { mutableIntStateOf(initialNowParts.second) }
    var notes by rememberSaveable { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val manualDuration = remember(startHour, startMinute, endHour, endMinute) {
        DateTimeUtils.calculateDuration(startHour, startMinute, endHour, endMinute)
    }

    val presets = remember {
        listOf(
            TimePreset("17:30–19:30", 17, 30, 19, 30),
            TimePreset("18:00–20:30", 18, 0, 20, 30),
            TimePreset("18:00–21:00", 18, 0, 21, 0),
            TimePreset("22:00–01:30 (+1)", 22, 0, 1, 30)
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.addOvertimeTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.addOvertimeSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ConnectionStatusBadge(isOnline = state.isOnline, strings = strings)
                }
            }

            // Segmented Switcher: Timer Live | Input Manual
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InputModePill(
                            selected = inputMode == LogInputMode.LIVE_TIMER,
                            icon = Icons.Rounded.Timer,
                            label = strings.inputTabLiveTimer,
                            showLiveDot = isTimerRunning,
                            onClick = { inputMode = LogInputMode.LIVE_TIMER },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_live_timer")
                        )
                        InputModePill(
                            selected = inputMode == LogInputMode.MANUAL,
                            icon = Icons.Rounded.EditCalendar,
                            label = strings.inputTabManual,
                            showLiveDot = false,
                            onClick = { inputMode = LogInputMode.MANUAL },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_manual_input")
                        )
                    }
                }
            }

            if (inputMode == LogInputMode.LIVE_TIMER) {
                // LIVE RUNNING CLOCK + PERSISTENT OVERTIME STOPWATCH CARD
                item {
                    val startMillis = state.activeOvertimeStartMillis
                    val elapsedMillis = if (startMillis != null) (nowMillis - startMillis).coerceAtLeast(0L) else 0L
                    val liveClockStr = DateTimeUtils.formatClock24WithSeconds(nowMillis)
                    val stopwatchStr = DateTimeUtils.formatStopwatchHms(elapsedMillis)
                    val todayDisplay = DateTimeUtils.formatDisplayDate(DateTimeUtils.todayIsoDate(), strings)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_timer_hero_card"),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        val gradientColors = if (isTimerRunning) {
                            listOf(Color(0xFF0F766E), EmeraldTeal, Color(0xFFD97706))
                        } else {
                            listOf(EmeraldTealDark, EmeraldTeal)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(gradientColors))
                                .padding(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${strings.liveClockLabel} • $liveClockStr",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isTimerRunning) AmberAccent else Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isTimerRunning) Color.White else StatusOnline)
                                            )
                                            Text(
                                                text = if (isTimerRunning) strings.timerRunningBadge else strings.timerReadyBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Main Live Ticking Stopwatch / Clock Display
                                Text(
                                    text = if (isTimerRunning) stopwatchStr else liveClockStr,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 44.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )

                                if (startMillis != null) {
                                    val (sDate, sH, sM) = DateTimeUtils.epochMillisToLocalDateAndTime(startMillis)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "${strings.timerStartedAtLabel} ${DateTimeUtils.formatTime24(sH, sM)} • ${DateTimeUtils.formatDisplayDate(sDate, strings)}",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Text(
                                        text = todayDisplay,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes Field for Live Timer
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.fieldNotesLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.optionalTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("timer_notes_input"),
                                placeholder = {
                                    Text(
                                        text = strings.fieldNotesPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp)
                            )
                            Text(
                                text = strings.timerHintBody,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Primary Start / Finish & Save Buttons
                item {
                    if (!isTimerRunning) {
                        Button(
                            onClick = onStartLiveTimer,
                            enabled = !state.isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_live_timer_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.timerStartButton,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onStopAndSaveLiveTimer(notes)
                                    notes = ""
                                },
                                enabled = !state.isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("stop_save_live_timer_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.timerStopAndSaveButton,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onCancelLiveTimer,
                                enabled = !state.isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("cancel_live_timer_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, StatusRejected.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.timerCancelButton, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // MANUAL 24-HOUR ENTRY FORM
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_duration_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        val gradientColors = if (manualDuration.totalMinutes == 0) {
                            listOf(Color(0xFF475569), Color(0xFF64748B))
                        } else if (manualDuration.isOvernight) {
                            listOf(Color(0xFF0F766E), EmeraldTeal, Color(0xFFD97706))
                        } else {
                            listOf(EmeraldTealDark, EmeraldTeal)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(gradientColors))
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Timer,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = strings.liveDurationLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }

                                    if (manualDuration.isOvernight) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AmberAccent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.NightsStay,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = strings.overnightBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = if (manualDuration.totalMinutes > 0) {
                                        DateTimeUtils.formatDurationFull(manualDuration.totalMinutes, strings)
                                    } else {
                                        "0 ${strings.hoursUnit} 0 ${strings.minutesUnit}"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = Color.White
                                )

                                if (manualDuration.totalMinutes == 0) {
                                    Text(
                                        text = strings.invalidDurationHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFEE2E2)
                                    )
                                } else if (manualDuration.isOvernight) {
                                    Text(
                                        text = strings.overnightDetectedHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFEF3C7)
                                    )
                                } else {
                                    Text(
                                        text = "${DateTimeUtils.formatTime24(startHour, startMinute)} → ${DateTimeUtils.formatTime24(endHour, endMinute)} • ${DateTimeUtils.formatDisplayDate(selectedDateIso, strings)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = strings.fieldDateLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { showDatePicker = true }
                                        .testTag("date_picker_field"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CalendarToday,
                                                contentDescription = strings.fieldDateLabel,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = DateTimeUtils.formatDisplayDate(selectedDateIso, strings),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = selectedDateIso,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TimeFieldBox(
                                    label = strings.fieldStartTimeLabel,
                                    timeText = DateTimeUtils.formatTime24(startHour, startMinute),
                                    icon = Icons.Rounded.AccessTime,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = { showStartTimePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("start_time_picker_field")
                                )
                                TimeFieldBox(
                                    label = strings.fieldEndTimeLabel,
                                    timeText = DateTimeUtils.formatTime24(endHour, endMinute),
                                    icon = Icons.Rounded.Schedule,
                                    accentColor = AmberAccent,
                                    onClick = { showEndTimePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("end_time_picker_field")
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = strings.quickPresetsLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presets) { preset ->
                                        SuggestionChip(
                                            onClick = {
                                                startHour = preset.startH
                                                startMinute = preset.startM
                                                endHour = preset.endH
                                                endMinute = preset.endM
                                            },
                                            label = {
                                                Text(
                                                    text = preset.label,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 12.sp
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.fieldNotesLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.optionalTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("notes_input_field"),
                                    placeholder = {
                                        Text(
                                            text = strings.fieldNotesPlaceholder,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (manualDuration.totalMinutes > 0) {
                                onSaveManual(
                                    selectedDateIso,
                                    startHour,
                                    startMinute,
                                    endHour,
                                    endMinute,
                                    notes
                                )
                                notes = ""
                            }
                        },
                        enabled = manualDuration.totalMinutes > 0 && !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("save_overtime_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = StatusRejected.copy(alpha = 0.25f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (state.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.saveOvertimeButton,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Mode & Cloud Sync Info Banner
            item {
                val modeNotice = when {
                    profile.mode == AppMode.PERSONAL -> strings.savePersonalNotice
                    profile.role == UserRole.ADMIN && profile.connectedAdminUid == state.currentUserId -> strings.saveAdminNotice
                    else -> strings.saveEmployeeNotice
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = modeNotice,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.offlineReadyHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.isoDateToEpochMillis(selectedDateIso)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateIso = DateTimeUtils.epochMillisToIsoDate(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.confirmAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancelAction)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text(strings.fieldStartTimeLabel, style = MaterialTheme.typography.titleMedium) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        startHour = timeState.hour
                        startMinute = timeState.minute
                        showStartTimePicker = false
                    }
                ) {
                    Text(strings.confirmAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }

    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text(strings.fieldEndTimeLabel, style = MaterialTheme.typography.titleMedium) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        endHour = timeState.hour
                        endMinute = timeState.minute
                        showEndTimePicker = false
                    }
                ) {
                    Text(strings.confirmAction, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}

@Composable
private fun InputModePill(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    showLiveDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showLiveDot) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AmberAccent)
                )
            }
        }
    }
}

@Composable
private fun TimeFieldBox(
    label: String,
    timeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(18.dp)
                    )
                }
                Text(
                    text = timeText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
