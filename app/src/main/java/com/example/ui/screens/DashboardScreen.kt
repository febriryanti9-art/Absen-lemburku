package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppMode
import com.example.data.OvertimeRecord
import com.example.data.OvertimeStatus
import com.example.data.UserRole
import com.example.ui.DashboardSubTab
import com.example.ui.OvertimeUiState
import com.example.ui.TeamFilter
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.OvertimeEntryCard
import com.example.ui.components.QrCodeScannerDialog
import com.example.ui.components.TopRightSettingsButton
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.EmeraldTealDark
import com.example.ui.theme.EmeraldTealLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusOnline
import com.example.util.DateTimeUtils

@Composable
fun DashboardScreen(
    state: OvertimeUiState,
    onSelectSubTab: (DashboardSubTab) -> Unit,
    onSelectCalendarDate: (String) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onQuickAddForDate: (String) -> Unit,
    onDeleteEntry: (OvertimeRecord) -> Unit,
    onExportCsv: () -> Unit,
    onShareCsv: () -> Unit,
    onSelectTeamFilter: (TeamFilter) -> Unit,
    onApproveTeamEntry: (String) -> Unit,
    onRejectTeamEntry: (String) -> Unit,
    onCopyCompanyCode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSettingsToConnect: () -> Unit,
    onScanQrToJoin: (String) -> Unit,
    onDismissNewMemberAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = state.strings
    val profile = state.profile
    var showQrScanner by remember { mutableStateOf(false) }

    val greeting = remember(strings) {
        val hour = DateTimeUtils.currentHourOfDay()
        when {
            hour < 11 -> strings.greetingMorning
            hour < 17 -> strings.greetingAfternoon
            else -> strings.greetingEvening
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Greeting Header + Mode Badge + Live Connection Status Badge
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greeting,",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConnectionStatusBadge(
                                isOnline = state.isOnline,
                                strings = strings
                            )
                            TopRightSettingsButton(
                                contentDescription = strings.navSettings,
                                onClick = onOpenSettings
                            )
                        }
                    }

                    // Active Mode Pill Badge + Real-Time Sync Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPersonal = profile.mode == AppMode.PERSONAL
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPersonal) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                AmberAccent.copy(alpha = 0.16f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPersonal) Icons.Rounded.Person else Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = if (isPersonal) MaterialTheme.colorScheme.onPrimaryContainer else AmberAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isPersonal) {
                                        strings.modePersonalBadge
                                    } else {
                                        val roleLabel = if (profile.role == UserRole.ADMIN) strings.roleAdmin else strings.roleEmployee
                                        val compLabel = profile.companyName.ifBlank { strings.modeGroupLabel }
                                        "$compLabel • $roleLabel"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPersonal) MaterialTheme.colorScheme.onPrimaryContainer else AmberAccent
                                )
                            }
                        }

                        if (!isPersonal && profile.isConnectedToCompany) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusOnline.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = StatusOnline,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = profile.companyCode,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusOnline
                                    )
                                }
                            }
                        }

                        if (!state.isAdminGroupMode) {
                            Surface(
                                onClick = { showQrScanner = true },
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldTeal.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, EmeraldTeal.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("header_scan_qr_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = strings.scanQrButton,
                                        tint = EmeraldTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = strings.scanQrButton,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Admin Notification Banner when a new Employee connects
            val joinedMember = state.newlyJoinedMemberAlert
            if (state.isAdminGroupMode && joinedMember != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_new_member_banner"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldTeal.copy(alpha = 0.14f)),
                        border = BorderStroke(1.5.dp, EmeraldTeal.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldTeal,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.newMemberJoinedTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldTeal
                                )
                                Text(
                                    text = "${joinedMember.employeeName} ${strings.newMemberJoinedBodySuffix}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (state.dashboardSubTab != DashboardSubTab.TEAM_MONITOR) {
                                Button(
                                    onClick = {
                                        onSelectSubTab(DashboardSubTab.TEAM_MONITOR)
                                        onDismissNewMemberAlert()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
                                ) {
                                    Text(
                                        text = strings.viewTeamAction,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissNewMemberAlert,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.dismissNotificationDesc,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Prompt banner if in Group mode but not yet connected to a company
            if (profile.mode == AppMode.GROUP && !profile.isConnectedToCompany) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LinkOff,
                                    contentDescription = null,
                                    tint = AmberAccent
                                )
                                Text(
                                    text = strings.companyNotConnectedBannerTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = strings.companyNotConnectedBannerBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showQrScanner = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dashboard_scan_qr_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldTeal)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.scanQrButton, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onOpenSettingsToConnect,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AmberAccent)
                                ) {
                                    Text(strings.openProfileConnectButton, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Admin Dual-Tab Switcher ("Ringkasan Saya" vs "Pantau Tim")
            if (state.isAdminGroupMode) {
                item {
                    AdminDashboardTabBar(
                        activeTab = state.dashboardSubTab,
                        pendingCount = state.teamPendingCount,
                        myLabel = strings.tabMyOverview,
                        teamLabel = strings.tabTeamMonitor,
                        onSelect = onSelectSubTab
                    )
                }
            }

            if (state.isAdminGroupMode && state.dashboardSubTab == DashboardSubTab.TEAM_MONITOR) {
                item {
                    TeamMonitorSection(
                        state = state,
                        onSelectFilter = onSelectTeamFilter,
                        onApprove = onApproveTeamEntry,
                        onReject = onRejectTeamEntry,
                        onCopyCompanyCode = onCopyCompanyCode
                    )
                }
            } else {
                // Hero Monthly Summary Card
                item {
                    MonthlySummaryHeroCard(
                        state = state,
                        modifier = Modifier.testTag("monthly_summary_card")
                    )
                }

                // Interactive Monthly Calendar Card + Selected Day Detail
                item {
                    InteractiveCalendarCard(
                        state = state,
                        onShiftMonth = onShiftMonth,
                        onSelectDate = onSelectCalendarDate,
                        onQuickAddForDate = onQuickAddForDate,
                        onDeleteEntry = onDeleteEntry
                    )
                }

                // Recent History Section Header + Export CSV / Share Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.recentHistoryTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onShareCsv,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("share_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = strings.shareCsvButton,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            FilledTonalButton(
                                onClick = onExportCsv,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("export_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.exportCsvButton,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (state.myEntries.isEmpty()) {
                    item {
                        EmptyHistoryCard(
                            title = strings.emptyHistoryTitle,
                            subtitle = strings.emptyHistorySubtitle
                        )
                    }
                } else {
                    items(state.myEntries.take(20), key = { it.id }) { entry ->
                        val canDelete = entry.personal ||
                            entry.status == OvertimeStatus.PENDING ||
                            state.profile.isAdminUser(state.currentUserId)
                        OvertimeEntryCard(
                            entry = entry,
                            strings = strings,
                            canDelete = canDelete,
                            onDelete = { onDeleteEntry(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrCodeScannerDialog(
            strings = strings,
            onCodeScanned = { scannedCode ->
                showQrScanner = false
                onScanQrToJoin(scannedCode)
            },
            onDismiss = { showQrScanner = false }
        )
    }
}

@Composable
private fun AdminDashboardTabBar(
    activeTab: DashboardSubTab,
    pendingCount: Int,
    myLabel: String,
    teamLabel: String,
    onSelect: (DashboardSubTab) -> Unit
) {
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
            DashboardSegmentButton(
                selected = activeTab == DashboardSubTab.MY_OVERVIEW,
                label = myLabel,
                badgeCount = 0,
                onClick = { onSelect(DashboardSubTab.MY_OVERVIEW) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_my_overview")
            )
            DashboardSegmentButton(
                selected = activeTab == DashboardSubTab.TEAM_MONITOR,
                label = teamLabel,
                badgeCount = pendingCount,
                onClick = { onSelect(DashboardSubTab.TEAM_MONITOR) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_team_monitor")
            )
        }
    }
}

@Composable
private fun DashboardSegmentButton(
    selected: Boolean,
    label: String,
    badgeCount: Int,
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = AmberAccent
                ) {
                    Text(
                        text = badgeCount.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryHeroCard(
    state: OvertimeUiState,
    modifier: Modifier = Modifier
) {
    val strings = state.strings
    val monthTitle = DateTimeUtils.formatMonthYear(state.displayedYear, state.displayedMonth, strings)
    val totalH = state.monthlyTotalMinutes / 60
    val totalM = state.monthlyTotalMinutes % 60

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(EmeraldTealDark, EmeraldTeal, EmeraldTealLight)
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.monthlySummaryTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1.35f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = strings.totalOvertimeDuration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalH",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = " ${strings.hoursUnit} ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "$totalM",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = Color(0xFFFEF3C7)
                                )
                                Text(
                                    text = " ${strings.minutesUnit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(0.85f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = strings.totalOvertimeDays,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${state.monthlyTotalDays}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = " ${strings.daysUnit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveCalendarCard(
    state: OvertimeUiState,
    onShiftMonth: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    onQuickAddForDate: (String) -> Unit,
    onDeleteEntry: (OvertimeRecord) -> Unit
) {
    val strings = state.strings
    val todayIso = remember { DateTimeUtils.todayIsoDate() }
    val cells = remember(state.displayedYear, state.displayedMonth) {
        DateTimeUtils.buildMonthCalendarCells(state.displayedYear, state.displayedMonth)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_calendar_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = DateTimeUtils.formatMonthYear(state.displayedYear, state.displayedMonth, strings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onShiftMonth(-1) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("calendar_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { onShiftMonth(1) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("calendar_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                strings.weekdaysShort.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { cell ->
                            if (cell.dayOfMonth == null || cell.isoDate == null) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isSelected = cell.isoDate == state.selectedDateIso
                                val isToday = cell.isoDate == todayIso
                                val hasLog = cell.isoDate in state.loggedDatesInMonth

                                val bgColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    hasLog -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else -> Color.Transparent
                                }
                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    hasLog -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                val borderStroke = if (isToday && !isSelected) {
                                    BorderStroke(1.5.dp, AmberAccent)
                                } else null

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectDate(cell.isoDate) }
                                        .testTag("calendar_day_${cell.isoDate}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    border = borderStroke
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = cell.dayOfMonth.toString(),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || hasLog || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )
                                        if (hasLog) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = strings.calendarLegendHasLog,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(9.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, AmberAccent)
                    ) {}
                    Text(
                        text = strings.calendarLegendToday,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            AnimatedVisibility(visible = true) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                imageVector = Icons.Rounded.Today,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = DateTimeUtils.formatDisplayDate(state.selectedDateIso, strings),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (state.selectedDateEntries.isNotEmpty()) {
                            val dayMins = state.selectedDateEntries.sumOf { it.durationMinutes }
                            Text(
                                text = DateTimeUtils.formatDurationFull(dayMins, strings),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (state.selectedDateEntries.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.noEntriesOnDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { onQuickAddForDate(state.selectedDateIso) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("quick_add_for_date_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.quickLogForDate,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        state.selectedDateEntries.forEach { entry ->
                            val canDelete = entry.personal ||
                                entry.status == OvertimeStatus.PENDING ||
                                entry.adminUid == state.currentUserId
                            OvertimeEntryCard(
                                entry = entry,
                                strings = strings,
                                canDelete = canDelete,
                                onDelete = { onDeleteEntry(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
