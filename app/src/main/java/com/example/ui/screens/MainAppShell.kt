package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppLanguage
import com.example.data.UserProfile
import com.example.ui.MainNavTab
import com.example.ui.OvertimeUiState
import com.example.ui.OvertimeViewModel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.StatusRejected
import com.example.util.AdminNotificationHelper

@Composable
fun MainAppShell(
    viewModel: OvertimeViewModel,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var pendingCsvContent by remember { mutableStateOf<String?>(null) }

    val createCsvDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val csvText = pendingCsvContent
        pendingCsvContent = null
        if (uri != null && csvText != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvText.toByteArray(Charsets.UTF_8))
                }
                viewModel.emitFeedback(state.strings.csvExportSuccess)
            } catch (_: Exception) {
                viewModel.emitFeedback(state.strings.csvExportFailed)
            }
        }
    }

    var hasRequestedNotifPerm by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled gracefully in AdminNotificationHelper */ }

    LaunchedEffect(state.isAdminGroupMode) {
        if (state.isAdminGroupMode) {
            AdminNotificationHelper.ensureNotificationChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !AdminNotificationHelper.hasNotificationPermission(context) &&
                !hasRequestedNotifPerm
            ) {
                hasRequestedNotifPerm = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.memberJoinedEvents.collect { event ->
            val currentStrings = viewModel.uiState.value.strings
            AdminNotificationHelper.notifyMemberJoined(
                context = context,
                title = currentStrings.newMemberJoinedTitle,
                body = "${event.memberName} ${currentStrings.newMemberJoinedBodySuffix} (${event.companyName} • ${event.companyCode})",
                notificationKey = "${event.companyCode}_${event.memberId}"
            )
        }
    }

    val handleExportCsv: () -> Unit = {
        val (fileName, csvContent) = viewModel.buildExportCsv()
        pendingCsvContent = csvContent
        createCsvDocumentLauncher.launch(fileName)
    }

    val handleShareCsv: () -> Unit = {
        val (fileName, csvContent) = viewModel.buildExportCsv()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, csvContent)
        }
        context.startActivity(Intent.createChooser(shareIntent, state.strings.exportCsvButton))
    }

    val handleShareBrowserLinkWhatsApp: () -> Unit = {
        shareBrowserLinkToWhatsApp(
            context = context,
            profile = state.profile,
            language = state.localPrefs.language,
            chooserTitle = state.strings.shareWhatsAppButton
        )
    }

    val isTimerRunning = state.activeOvertimeStartMillis != null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 840.dp

        if (isExpandedWidth) {
            // Adaptive Tablet / Expanded Layout with Side NavigationRail
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(contentAlignment = Alignment.TopEnd) {
                                FloatingActionButton(
                                    onClick = { viewModel.selectNavTab(MainNavTab.ADD_OVERTIME) },
                                    containerColor = if (isTimerRunning) AmberAccent else EmeraldTeal,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("nav_add_overtime_button")
                                ) {
                                    Icon(
                                        imageVector = if (isTimerRunning) Icons.Rounded.Timer else Icons.Rounded.Add,
                                        contentDescription = state.strings.navAdd
                                    )
                                }
                                if (isTimerRunning) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(StatusRejected)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NavigationRailItem(
                            selected = state.activeNavTab == MainNavTab.DASHBOARD,
                            onClick = { viewModel.selectNavTab(MainNavTab.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (state.activeNavTab == MainNavTab.DASHBOARD) {
                                        Icons.Rounded.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = state.strings.navDashboard
                                )
                            },
                            label = { Text(state.strings.navDashboard) },
                            modifier = Modifier.testTag("nav_dashboard_button")
                        )
                        NavigationRailItem(
                            selected = state.activeNavTab == MainNavTab.MEMBERS,
                            onClick = { viewModel.selectNavTab(MainNavTab.MEMBERS) },
                            icon = {
                                Icon(
                                    imageVector = if (state.activeNavTab == MainNavTab.MEMBERS) {
                                        Icons.Rounded.Groups
                                    } else {
                                        Icons.Outlined.Groups
                                    },
                                    contentDescription = state.strings.navMembers
                                )
                            },
                            label = { Text(state.strings.navMembers) },
                            modifier = Modifier.testTag("nav_members_button")
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ActiveScreenContent(
                            state = state,
                            viewModel = viewModel,
                            onExportCsv = handleExportCsv,
                            onShareCsv = handleShareCsv,
                            onShareBrowserLinkWhatsApp = handleShareBrowserLinkWhatsApp,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        } else {
            // Mobile-First Layout: Dashboard | + (Catat Lembur) | Anggota (Settings moved to Top-Right)
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    OvertimeBottomNavBar(
                        activeTab = state.activeNavTab,
                        isTimerRunning = isTimerRunning,
                        dashboardLabel = state.strings.navDashboard,
                        addLabel = state.strings.navAdd,
                        membersLabel = state.strings.navMembers,
                        onSelectTab = viewModel::selectNavTab
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    ActiveScreenContent(
                        state = state,
                        viewModel = viewModel,
                        onExportCsv = handleExportCsv,
                        onShareCsv = handleShareCsv,
                        onShareBrowserLinkWhatsApp = handleShareBrowserLinkWhatsApp,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveScreenContent(
    state: OvertimeUiState,
    viewModel: OvertimeViewModel,
    onExportCsv: () -> Unit,
    onShareCsv: () -> Unit,
    onShareBrowserLinkWhatsApp: () -> Unit,
    onSignOut: () -> Unit
) {
    when (state.activeNavTab) {
        MainNavTab.DASHBOARD -> {
            DashboardScreen(
                state = state,
                onSelectSubTab = viewModel::selectDashboardSubTab,
                onSelectCalendarDate = viewModel::selectCalendarDate,
                onShiftMonth = viewModel::shiftCalendarMonth,
                onQuickAddForDate = viewModel::openAddForDate,
                onDeleteEntry = viewModel::deleteEntry,
                onExportCsv = onExportCsv,
                onShareCsv = onShareCsv,
                onSelectTeamFilter = viewModel::selectTeamFilter,
                onApproveTeamEntry = viewModel::approveEntry,
                onRejectTeamEntry = viewModel::rejectEntry,
                onCopyCompanyCode = { viewModel.emitFeedback(state.strings.codeCopiedMsg) },
                onOpenSettings = viewModel::openSettings,
                onOpenSettingsToConnect = viewModel::openSettings,
                onScanQrToJoin = viewModel::connectViaScannedQrCode,
                onDismissNewMemberAlert = viewModel::dismissNewMemberAlert
            )
        }
        MainNavTab.ADD_OVERTIME -> {
            AddOvertimeScreen(
                state = state,
                onNavigateBack = { viewModel.selectNavTab(MainNavTab.DASHBOARD) },
                onOpenSettings = viewModel::openSettings,
                onStartLiveTimer = viewModel::startLiveOvertimeTimer,
                onCancelLiveTimer = viewModel::cancelLiveOvertimeTimer,
                onStopAndSaveLiveTimer = viewModel::stopAndSaveLiveOvertime,
                onSaveManual = viewModel::saveOvertimeEntry
            )
        }
        MainNavTab.MEMBERS -> {
            MembersScreen(
                state = state,
                onNavigateBack = { viewModel.selectNavTab(MainNavTab.DASHBOARD) },
                onOpenSettings = viewModel::openSettings,
                onScanQrToJoin = viewModel::connectViaScannedQrCode,
                onCopyCompanyCode = { viewModel.emitFeedback(state.strings.codeCopiedMsg) },
                onInviteViaWhatsApp = onShareBrowserLinkWhatsApp,
                onPromoteToAdmin = viewModel::promoteMemberToAdmin,
                onDemoteToMember = viewModel::demoteAdminToMember
            )
        }
        MainNavTab.SETTINGS -> {
            SettingsScreen(
                state = state,
                onNavigateBack = viewModel::navigateBackFromSettings,
                onSaveProfileAndCompany = viewModel::saveProfileAndCompany,
                onSelectTheme = viewModel::setThemeMode,
                onSelectLanguage = viewModel::setLanguage,
                onShareBrowserLinkWhatsApp = onShareBrowserLinkWhatsApp,
                onBrowserLinkCopied = { viewModel.emitFeedback(state.strings.linkCopiedMsg) },
                onSignOut = onSignOut
            )
        }
    }
}

private fun shareBrowserLinkToWhatsApp(
    context: Context,
    profile: UserProfile,
    language: AppLanguage,
    chooserTitle: String
) {
    val browserUrl = UserProfile.BROWSER_APP_URL
    val message = if (language == AppLanguage.INDONESIAN) {
        buildString {
            appendLine("Halo! Yuk gabung ke *Absen Lemburku* untuk mencatat & memantau jam lembur tim secara real-time.")
            appendLine()
            appendLine("🌐 Buka lewat Browser (Android / iOS / PC):")
            appendLine(browserUrl)
            if (profile.isConnectedToCompany) {
                appendLine()
                val companyDisplay = profile.companyName.ifBlank { profile.companyCode }
                appendLine("🏢 Perusahaan: *$companyDisplay*")
                appendLine("🔑 Kode Perusahaan: *${profile.companyCode}*")
                appendLine("Login dengan Google lalu masukkan Kode Perusahaan di atas pada menu Pengaturan → Profil & Koneksi Perusahaan.")
            }
        }.trim()
    } else {
        buildString {
            appendLine("Hi! Join *Absen Lemburku* to track and sync team overtime in real time.")
            appendLine()
            appendLine("🌐 Open in Browser (Android / iOS / Desktop):")
            appendLine(browserUrl)
            if (profile.isConnectedToCompany) {
                appendLine()
                val companyDisplay = profile.companyName.ifBlank { profile.companyCode }
                appendLine("🏢 Company: *$companyDisplay*")
                appendLine("🔑 Company Code: *${profile.companyCode}*")
                appendLine("Sign in with Google and enter this Company Code in Settings → Profile & Company Sync.")
            }
        }.trim()
    }

    val baseIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }

    // Try standard WhatsApp, then WhatsApp Business, then system share sheet
    val launchedWhatsApp = listOf("com.whatsapp", "com.whatsapp.w4b").any { pkg ->
        runCatching {
            val waIntent = Intent(baseIntent).setPackage(pkg)
            context.startActivity(waIntent)
            true
        }.getOrDefault(false)
    }

    if (!launchedWhatsApp) {
        runCatching {
            context.startActivity(Intent.createChooser(baseIntent, chooserTitle))
        }
    }
}

@Composable
private fun OvertimeBottomNavBar(
    activeTab: MainNavTab,
    isTimerRunning: Boolean,
    dashboardLabel: String,
    addLabel: String,
    membersLabel: String,
    onSelectTab: (MainNavTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTabItem(
                selected = activeTab == MainNavTab.DASHBOARD,
                label = dashboardLabel,
                selectedIcon = Icons.Rounded.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                onClick = { onSelectTab(MainNavTab.DASHBOARD) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_dashboard_button")
            )

            // Prominent Center (+ Catat Lembur / Active Timer) Action Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val isAddActive = activeTab == MainNavTab.ADD_OVERTIME
                Box(contentAlignment = Alignment.TopEnd) {
                    FloatingActionButton(
                        onClick = { onSelectTab(MainNavTab.ADD_OVERTIME) },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = when {
                            isTimerRunning -> AmberAccent
                            isAddActive -> MaterialTheme.colorScheme.secondary
                            else -> EmeraldTeal
                        },
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("nav_add_overtime_button")
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Rounded.Timer else Icons.Rounded.Add,
                            contentDescription = addLabel,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (isTimerRunning) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(StatusRejected)
                        )
                    }
                }
            }

            BottomNavTabItem(
                selected = activeTab == MainNavTab.MEMBERS,
                label = membersLabel,
                selectedIcon = Icons.Rounded.Groups,
                unselectedIcon = Icons.Outlined.Groups,
                onClick = { onSelectTab(MainNavTab.MEMBERS) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_members_button")
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (selected) activeColor.copy(alpha = 0.14f) else Color.Transparent
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else inactiveColor
        )
    }
}
