package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.AppMode
import com.example.data.AppThemeMode
import com.example.data.CompanyDoc
import com.example.data.CompanyMemberDoc
import com.example.data.EmployeeRecap
import com.example.data.FirestoreOvertimeRepository
import com.example.data.LocalAppPreferences
import com.example.data.OvertimeRecord
import com.example.data.OvertimeStatus
import com.example.data.PreferencesStorage
import com.example.data.UserProfile
import com.example.data.UserProfileDoc
import com.example.data.UserRole
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.stringsFor
import com.example.util.CsvExporter
import com.example.util.DateTimeUtils
import com.example.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainNavTab {
    DASHBOARD,
    ADD_OVERTIME,
    MEMBERS,
    SETTINGS
}

enum class DashboardSubTab {
    MY_OVERVIEW,
    TEAM_MONITOR
}

enum class TeamFilter {
    ALL,
    PENDING,
    APPROVED,
    REJECTED
}

data class OvertimeUiState(
    val currentUserId: String = "",
    val profile: UserProfile = UserProfile(),
    val localPrefs: LocalAppPreferences = LocalAppPreferences(),
    val strings: AppStrings = stringsFor(AppLanguage.INDONESIAN),
    val isOnline: Boolean = true,
    val isBusy: Boolean = false,
    val syncError: String? = null,
    val activeNavTab: MainNavTab = MainNavTab.DASHBOARD,
    val dashboardSubTab: DashboardSubTab = DashboardSubTab.MY_OVERVIEW,
    val teamFilter: TeamFilter = TeamFilter.ALL,
    val displayedYear: Int = DateTimeUtils.currentYearMonth().first,
    val displayedMonth: Int = DateTimeUtils.currentYearMonth().second,
    val selectedDateIso: String = DateTimeUtils.todayIsoDate(),
    val draftDateIso: String = DateTimeUtils.todayIsoDate(),
    val myEntries: List<OvertimeRecord> = emptyList(),
    val myMonthEntries: List<OvertimeRecord> = emptyList(),
    val loggedDatesInMonth: Set<String> = emptySet(),
    val selectedDateEntries: List<OvertimeRecord> = emptyList(),
    val monthlyTotalMinutes: Int = 0,
    val monthlyTotalDays: Int = 0,
    val monthlyPendingCount: Int = 0,
    // Real-time Company Admin Data
    val companyMembers: List<CompanyMemberDoc> = emptyList(),
    val companyEntries: List<OvertimeRecord> = emptyList(),
    val filteredTeamEntries: List<OvertimeRecord> = emptyList(),
    val employeeRecaps: List<EmployeeRecap> = emptyList(),
    val teamPendingCount: Int = 0,
    // Admin New Member Live Alert
    val newlyJoinedMemberAlert: CompanyMemberDoc? = null,
    val recentJoinedMemberIds: Set<String> = emptySet()
) {
    val isAdminGroupMode: Boolean
        get() = profile.mode == AppMode.GROUP &&
            profile.companyCode.isNotBlank() &&
            profile.isAdminUser(currentUserId)

    val activeOvertimeStartMillis: Long?
        get() = localPrefs.activeOvertimeStartMillis
}

data class MemberJoinedEvent(
    val memberId: String,
    val memberName: String,
    val companyName: String,
    val companyCode: String
)

private data class UiLocalState(
    val activeNavTab: MainNavTab = MainNavTab.DASHBOARD,
    val previousNavTab: MainNavTab = MainNavTab.DASHBOARD,
    val dashboardSubTab: DashboardSubTab = DashboardSubTab.MY_OVERVIEW,
    val teamFilter: TeamFilter = TeamFilter.ALL,
    val displayedYear: Int = DateTimeUtils.currentYearMonth().first,
    val displayedMonth: Int = DateTimeUtils.currentYearMonth().second,
    val selectedDateIso: String = DateTimeUtils.todayIsoDate(),
    val draftDateIso: String = DateTimeUtils.todayIsoDate(),
    val isBusy: Boolean = false,
    val newlyJoinedMemberAlert: CompanyMemberDoc? = null,
    val recentJoinedMemberIds: Set<String> = emptySet()
)

private data class CloudSyncBundle(
    val profileDoc: UserProfileDoc? = null,
    val companyDoc: CompanyDoc? = null,
    val personalEntries: List<OvertimeRecord> = emptyList(),
    val myCompanyEntries: List<OvertimeRecord> = emptyList(),
    val adminTeamEntries: List<OvertimeRecord> = emptyList(),
    val companyMembers: List<CompanyMemberDoc> = emptyList(),
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class OvertimeViewModel(
    private val repository: FirestoreOvertimeRepository,
    private val preferencesStorage: PreferencesStorage,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val currentUserDisplayName: String,
    private val currentUserEmail: String
) : ViewModel() {

    private val localState = MutableStateFlow(UiLocalState())

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _memberJoinedEvents = MutableSharedFlow<MemberJoinedEvent>(extraBufferCapacity = 8)
    val memberJoinedEvents = _memberJoinedEvents.asSharedFlow()

    private fun <T> Flow<T>.safeFirestoreStream(fallback: T, onError: (String) -> Unit): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Firestore stream error: ${root.message}", root)
            onError(root.localizedMessage ?: "Firestore sync error")
            emit(fallback)
        }

    private val streamErrorFlow = MutableStateFlow<String?>(null)

    private val userProfileFlow: Flow<UserProfileDoc?> =
        repository.observeUserProfile(currentUserId)
            .safeFirestoreStream(null) { streamErrorFlow.value = it }

    private val personalEntriesFlow: Flow<List<OvertimeRecord>> =
        repository.observePersonalOvertime(currentUserId)
            .safeFirestoreStream(emptyList()) { streamErrorFlow.value = it }

    private val companyDocFlow: Flow<CompanyDoc?> = userProfileFlow.flatMapLatest { doc ->
        val code = doc?.companyCode.orEmpty()
        if (doc?.mode == AppMode.GROUP.key && code.length >= 3) {
            repository.observeCompany(code, currentUserId)
                .safeFirestoreStream(null) { streamErrorFlow.value = it }
        } else {
            flowOf(null)
        }
    }

    private val myCompanyEntriesFlow: Flow<List<OvertimeRecord>> =
        combine(userProfileFlow, companyDocFlow) { profileDoc, companyDoc ->
            profileDoc to companyDoc
        }.flatMapLatest { (profileDoc, companyDoc) ->
            val code = companyDoc?.companyCode.orEmpty()
            if (profileDoc?.mode == AppMode.GROUP.key && code.isNotBlank()) {
                repository.observeMyCompanyOvertime(code, currentUserId)
                    .safeFirestoreStream(emptyList()) { streamErrorFlow.value = it }
            } else {
                flowOf(emptyList())
            }
        }

    private val adminTeamEntriesFlow: Flow<List<OvertimeRecord>> =
        combine(userProfileFlow, companyDocFlow) { profileDoc, companyDoc ->
            profileDoc to companyDoc
        }.flatMapLatest { (profileDoc, companyDoc) ->
            val code = companyDoc?.companyCode.orEmpty()
            val isRealAdmin = profileDoc?.mode == AppMode.GROUP.key &&
                companyDoc?.effectiveAdminUids()?.contains(currentUserId) == true &&
                code.isNotBlank()
            if (isRealAdmin) {
                repository.observeAllCompanyOvertimeForAdmin(code, currentUserId)
                    .safeFirestoreStream(emptyList()) { streamErrorFlow.value = it }
            } else {
                flowOf(emptyList())
            }
        }

    private val companyMembersFlow: Flow<List<CompanyMemberDoc>> =
        combine(userProfileFlow, companyDocFlow) { profileDoc, companyDoc ->
            profileDoc to companyDoc
        }.flatMapLatest { (profileDoc, companyDoc) ->
            val code = companyDoc?.companyCode.orEmpty()
            val isConnectedGroup = profileDoc?.mode == AppMode.GROUP.key && code.isNotBlank()
            if (isConnectedGroup) {
                repository.observeCompanyMembers(code, currentUserId)
                    .safeFirestoreStream(emptyList()) { streamErrorFlow.value = it }
            } else {
                flowOf(emptyList())
            }
        }

    private val cloudSyncFlow: Flow<CloudSyncBundle> = combine(
        userProfileFlow,
        companyDocFlow,
        personalEntriesFlow,
        myCompanyEntriesFlow,
        adminTeamEntriesFlow
    ) { profileDoc, companyDoc, personalEntries, myCompanyEntries, adminTeamEntries ->
        CloudSyncBundle(
            profileDoc = profileDoc,
            companyDoc = companyDoc,
            personalEntries = personalEntries,
            myCompanyEntries = myCompanyEntries,
            adminTeamEntries = adminTeamEntries
        )
    }.combine(companyMembersFlow) { bundle, members ->
        bundle.copy(companyMembers = members)
    }.combine(streamErrorFlow) { bundle, err ->
        bundle.copy(error = err)
    }

    val uiState: StateFlow<OvertimeUiState> = combine(
        preferencesStorage.preferencesFlow,
        networkMonitor.isOnline,
        localState,
        cloudSyncFlow
    ) { localPrefs, isOnline, local, cloud ->
        val strings = stringsFor(localPrefs.language)
        val pDoc = cloud.profileDoc
        val cDoc = cloud.companyDoc

        val resolvedMode = AppMode.fromKey(pDoc?.mode)
        val effectiveAdminUids = cDoc?.effectiveAdminUids().orEmpty()
        val isCompanyAdmin = resolvedMode == AppMode.GROUP &&
            cDoc != null &&
            currentUserId in effectiveAdminUids
        val resolvedRole = if (isCompanyAdmin) UserRole.ADMIN else UserRole.fromKey(pDoc?.role)
        val profile = UserProfile(
            name = pDoc?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUserDisplayName.ifBlank { "Overtime User" },
            email = currentUserEmail,
            mode = resolvedMode,
            role = resolvedRole,
            companyName = cDoc?.companyName?.takeIf { it.isNotBlank() }
                ?: pDoc?.companyName.orEmpty(),
            companyCode = pDoc?.companyCode.orEmpty(),
            connectedAdminUid = cDoc?.adminUid.orEmpty(),
            connectedAdminUids = effectiveAdminUids
        )

        val isAdminGroup = profile.mode == AppMode.GROUP &&
            profile.companyCode.isNotBlank() &&
            profile.isAdminUser(currentUserId)

        val effectiveSubTab = if (isAdminGroup) local.dashboardSubTab else DashboardSubTab.MY_OVERVIEW

        val myEntries = if (profile.mode == AppMode.PERSONAL) {
            cloud.personalEntries
        } else {
            cloud.myCompanyEntries
        }

        val monthPrefix = DateTimeUtils.yearMonthPrefix(local.displayedYear, local.displayedMonth)
        val myMonthEntries = myEntries.filter { it.dateIso.startsWith(monthPrefix) }
        val validMonthEntries = myMonthEntries.filter { it.status != OvertimeStatus.REJECTED }
        val monthlyTotalMinutes = validMonthEntries.sumOf { it.durationMinutes }
        val monthlyTotalDays = validMonthEntries.map { it.dateIso }.distinct().size
        val monthlyPendingCount = myMonthEntries.count { it.status == OvertimeStatus.PENDING }
        val loggedDatesInMonth = myMonthEntries.map { it.dateIso }.toSet()
        val selectedDateEntries = myEntries.filter { it.dateIso == local.selectedDateIso }

        val companyEntries = cloud.adminTeamEntries
        val teamPendingCount = companyEntries.count { it.status == OvertimeStatus.PENDING }

        val filteredTeamEntries = when (local.teamFilter) {
            TeamFilter.ALL -> companyEntries
            TeamFilter.PENDING -> companyEntries.filter { it.status == OvertimeStatus.PENDING }
            TeamFilter.APPROVED -> companyEntries.filter { it.status == OvertimeStatus.APPROVED }
            TeamFilter.REJECTED -> companyEntries.filter { it.status == OvertimeStatus.REJECTED }
        }

        val employeeRecaps = companyEntries
            .groupBy { it.userId }
            .map { (uid, entries) ->
                val latestName = entries.firstOrNull()?.employeeName ?: "Karyawan"
                val nonRejected = entries.filter { it.status != OvertimeStatus.REJECTED }
                EmployeeRecap(
                    userId = uid,
                    employeeName = latestName,
                    totalMinutes = nonRejected.sumOf { it.durationMinutes },
                    approvedMinutes = entries
                        .filter { it.status == OvertimeStatus.APPROVED }
                        .sumOf { it.durationMinutes },
                    totalDays = nonRejected.map { it.dateIso }.distinct().size,
                    pendingCount = entries.count { it.status == OvertimeStatus.PENDING },
                    totalEntries = entries.size
                )
            }
            .sortedByDescending { it.totalMinutes }

        OvertimeUiState(
            currentUserId = currentUserId,
            profile = profile,
            localPrefs = localPrefs,
            strings = strings,
            isOnline = isOnline,
            isBusy = local.isBusy,
            syncError = cloud.error,
            activeNavTab = local.activeNavTab,
            dashboardSubTab = effectiveSubTab,
            teamFilter = local.teamFilter,
            displayedYear = local.displayedYear,
            displayedMonth = local.displayedMonth,
            selectedDateIso = local.selectedDateIso,
            draftDateIso = local.draftDateIso,
            myEntries = myEntries,
            myMonthEntries = myMonthEntries,
            loggedDatesInMonth = loggedDatesInMonth,
            selectedDateEntries = selectedDateEntries,
            monthlyTotalMinutes = monthlyTotalMinutes,
            monthlyTotalDays = monthlyTotalDays,
            monthlyPendingCount = monthlyPendingCount,
            companyMembers = cloud.companyMembers,
            companyEntries = companyEntries,
            filteredTeamEntries = filteredTeamEntries,
            employeeRecaps = employeeRecaps,
            teamPendingCount = teamPendingCount,
            newlyJoinedMemberAlert = if (isAdminGroup) local.newlyJoinedMemberAlert else null,
            recentJoinedMemberIds = if (isAdminGroup) local.recentJoinedMemberIds else emptySet()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OvertimeUiState(
            currentUserId = currentUserId,
            profile = UserProfile(
                name = currentUserDisplayName.ifBlank { "Overtime User" },
                email = currentUserEmail
            )
        )
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfileExists(currentUserDisplayName)
        }
        observeNewMemberJoinsForAdmin()
    }

    private fun observeNewMemberJoinsForAdmin() {
        viewModelScope.launch {
            var trackedCompanyCode: String? = null
            var baselineInitialized = false
            val knownMemberIds = mutableSetOf<String>()

            combine(userProfileFlow, companyDocFlow, companyMembersFlow) { pDoc, cDoc, members ->
                Triple(pDoc, cDoc, members)
            }.collect { (pDoc, cDoc, members) ->
                val code = cDoc?.companyCode.orEmpty()
                val isRealAdmin = pDoc?.mode == AppMode.GROUP.key &&
                    cDoc?.effectiveAdminUids()?.contains(currentUserId) == true &&
                    code.isNotBlank()

                if (!isRealAdmin) {
                    trackedCompanyCode = null
                    baselineInitialized = false
                    knownMemberIds.clear()
                    return@collect
                }

                if (trackedCompanyCode != code) {
                    trackedCompanyCode = code
                    baselineInitialized = false
                    knownMemberIds.clear()
                }

                if (!baselineInitialized) {
                    if (members.isNotEmpty()) {
                        knownMemberIds.addAll(members.map { it.userId })
                        baselineInitialized = true
                    }
                    return@collect
                }

                val newlyConnected = members.filter { member ->
                    member.userId.isNotBlank() &&
                        member.userId != currentUserId &&
                        member.userId !in knownMemberIds
                }

                if (newlyConnected.isNotEmpty()) {
                    newlyConnected.forEach { member ->
                        knownMemberIds.add(member.userId)
                        val companyName = cDoc?.companyName?.ifBlank { code } ?: code
                        val strings = uiState.value.strings
                        localState.value = localState.value.copy(
                            newlyJoinedMemberAlert = member,
                            recentJoinedMemberIds = localState.value.recentJoinedMemberIds + member.userId
                        )
                        _memberJoinedEvents.tryEmit(
                            MemberJoinedEvent(
                                memberId = member.userId,
                                memberName = member.employeeName,
                                companyName = companyName,
                                companyCode = code
                            )
                        )
                        _snackbarEvents.tryEmit(
                            "${strings.newMemberJoinedTitle} ${member.employeeName} ${strings.newMemberJoinedBodySuffix}"
                        )
                    }
                }
            }
        }
    }

    fun dismissNewMemberAlert() {
        localState.value = localState.value.copy(newlyJoinedMemberAlert = null)
    }

    fun connectViaScannedQrCode(rawCompanyCode: String) {
        val currentName = uiState.value.profile.name
        saveProfileAndCompany(
            displayName = currentName,
            mode = AppMode.GROUP,
            role = UserRole.EMPLOYEE,
            companyName = "",
            companyCode = rawCompanyCode,
            onComplete = {}
        )
    }

    fun selectNavTab(tab: MainNavTab) {
        val current = localState.value
        val prev = if (tab == MainNavTab.SETTINGS && current.activeNavTab != MainNavTab.SETTINGS) {
            current.activeNavTab
        } else {
            current.previousNavTab
        }
        localState.value = current.copy(
            activeNavTab = tab,
            previousNavTab = prev,
            draftDateIso = if (tab == MainNavTab.ADD_OVERTIME) DateTimeUtils.todayIsoDate() else current.draftDateIso
        )
    }

    fun openSettings() {
        selectNavTab(MainNavTab.SETTINGS)
    }

    fun navigateBackFromSettings() {
        val prev = localState.value.previousNavTab
        val target = if (prev == MainNavTab.SETTINGS) MainNavTab.DASHBOARD else prev
        localState.value = localState.value.copy(activeNavTab = target)
    }

    fun openAddForDate(dateIso: String) {
        localState.value = localState.value.copy(
            activeNavTab = MainNavTab.ADD_OVERTIME,
            draftDateIso = dateIso
        )
    }

    fun selectDashboardSubTab(subTab: DashboardSubTab) {
        localState.value = localState.value.copy(dashboardSubTab = subTab)
    }

    fun selectTeamFilter(filter: TeamFilter) {
        localState.value = localState.value.copy(teamFilter = filter)
    }

    fun selectCalendarDate(isoDate: String) {
        localState.value = localState.value.copy(selectedDateIso = isoDate)
    }

    fun shiftCalendarMonth(delta: Int) {
        val (newY, newM) = DateTimeUtils.shiftMonth(
            localState.value.displayedYear,
            localState.value.displayedMonth,
            delta
        )
        localState.value = localState.value.copy(
            displayedYear = newY,
            displayedMonth = newM,
            selectedDateIso = DateTimeUtils.formatIsoDate(newY, newM, 1)
        )
    }

    // --- Live Running Overtime Timer Actions ---

    fun startLiveOvertimeTimer() {
        val msg = uiState.value.strings.timerStartedMsg
        viewModelScope.launch {
            preferencesStorage.setActiveOvertimeStartMillis(System.currentTimeMillis())
            _snackbarEvents.tryEmit(msg)
        }
    }

    fun cancelLiveOvertimeTimer() {
        val msg = uiState.value.strings.timerCancelledMsg
        viewModelScope.launch {
            preferencesStorage.setActiveOvertimeStartMillis(null)
            _snackbarEvents.tryEmit(msg)
        }
    }

    fun stopAndSaveLiveOvertime(notes: String) {
        val startMillis = uiState.value.activeOvertimeStartMillis ?: return
        val endMillis = System.currentTimeMillis()
        val (startDateIso, startH, startM) = DateTimeUtils.epochMillisToLocalDateAndTime(startMillis)
        val (endDateIso, endH, endM) = DateTimeUtils.epochMillisToLocalDateAndTime(endMillis)
        val elapsedMinutes = ((endMillis - startMillis) / 60_000L).toInt().coerceIn(1, 1440)
        val isOvernight = endDateIso != startDateIso || (endH * 60 + endM < startH * 60 + startM)

        saveOvertimeInternal(
            dateIso = startDateIso,
            startTime = DateTimeUtils.formatTime24(startH, startM),
            endTime = DateTimeUtils.formatTime24(endH, endM),
            durationMinutes = elapsedMinutes,
            overnight = isOvernight,
            notes = notes,
            clearTimerOnSuccess = true
        )
    }

    // --- Manual Overtime Save ---

    fun saveOvertimeEntry(
        dateIso: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        notes: String
    ) {
        val calc = DateTimeUtils.calculateDuration(startHour, startMinute, endHour, endMinute)
        if (calc.totalMinutes <= 0) return

        saveOvertimeInternal(
            dateIso = dateIso,
            startTime = DateTimeUtils.formatTime24(startHour, startMinute),
            endTime = DateTimeUtils.formatTime24(endHour, endMinute),
            durationMinutes = calc.totalMinutes,
            overnight = calc.isOvernight,
            notes = notes,
            clearTimerOnSuccess = false
        )
    }

    private fun saveOvertimeInternal(
        dateIso: String,
        startTime: String,
        endTime: String,
        durationMinutes: Int,
        overnight: Boolean,
        notes: String,
        clearTimerOnSuccess: Boolean
    ) {
        val state = uiState.value
        val profile = state.profile
        val strings = state.strings

        if (profile.mode == AppMode.GROUP && !profile.isConnectedToCompany) {
            _snackbarEvents.tryEmit(strings.errConnectCompanyFirst)
            return
        }

        viewModelScope.launch {
            localState.value = localState.value.copy(isBusy = true)
            val result = if (profile.mode == AppMode.PERSONAL) {
                repository.addPersonalOvertime(
                    employeeName = profile.name,
                    dateIso = dateIso,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    overnight = overnight,
                    notes = notes
                )
            } else {
                repository.addCompanyOvertime(
                    companyCode = profile.companyCode,
                    adminUid = profile.connectedAdminUid,
                    employeeName = profile.name,
                    isAdminSelf = profile.isAdminUser(currentUserId),
                    dateIso = dateIso,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    overnight = overnight,
                    notes = notes
                )
            }
            localState.value = localState.value.copy(isBusy = false)

            result.onSuccess {
                if (clearTimerOnSuccess) {
                    preferencesStorage.setActiveOvertimeStartMillis(null)
                }
                val parsed = DateTimeUtils.parseIsoDate(dateIso)
                localState.value = localState.value.copy(
                    activeNavTab = MainNavTab.DASHBOARD,
                    dashboardSubTab = DashboardSubTab.MY_OVERVIEW,
                    displayedYear = parsed?.first ?: localState.value.displayedYear,
                    displayedMonth = parsed?.second ?: localState.value.displayedMonth,
                    selectedDateIso = dateIso
                )
                val msg = when {
                    profile.mode == AppMode.PERSONAL -> strings.savedSuccessPersonal
                    profile.isAdminUser(currentUserId) -> strings.savedSuccessAdmin
                    else -> strings.savedSuccessPending
                }
                _snackbarEvents.tryEmit(msg)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: strings.errGenericSave)
            }
        }
    }

    fun deleteEntry(record: OvertimeRecord) {
        val strings = uiState.value.strings
        viewModelScope.launch {
            repository.deleteOvertime(record)
                .onSuccess { _snackbarEvents.tryEmit(strings.entryDeletedMsg) }
                .onFailure { err -> _snackbarEvents.tryEmit(err.localizedMessage ?: strings.errGenericSave) }
        }
    }

    fun approveEntry(entryId: String) {
        val state = uiState.value
        val code = state.profile.companyCode
        if (code.isBlank()) return
        viewModelScope.launch {
            repository.reviewCompanyOvertime(code, entryId, OvertimeStatus.APPROVED)
                .onSuccess { _snackbarEvents.tryEmit(state.strings.approvedFeedback) }
                .onFailure { err -> _snackbarEvents.tryEmit(err.localizedMessage ?: state.strings.errGenericSave) }
        }
    }

    fun rejectEntry(entryId: String) {
        val state = uiState.value
        val code = state.profile.companyCode
        if (code.isBlank()) return
        viewModelScope.launch {
            repository.reviewCompanyOvertime(code, entryId, OvertimeStatus.REJECTED)
                .onSuccess { _snackbarEvents.tryEmit(state.strings.rejectedFeedback) }
                .onFailure { err -> _snackbarEvents.tryEmit(err.localizedMessage ?: state.strings.errGenericSave) }
        }
    }

    fun promoteMemberToAdmin(targetUid: String) {
        val state = uiState.value
        val code = state.profile.companyCode
        if (!state.isAdminGroupMode || code.isBlank()) return
        viewModelScope.launch {
            localState.value = localState.value.copy(isBusy = true)
            val result = repository.promoteMemberToAdmin(code, targetUid)
            localState.value = localState.value.copy(isBusy = false)
            result.onSuccess {
                _snackbarEvents.tryEmit(state.strings.promotedToAdminMsg)
            }.onFailure { err ->
                val msg = when (err.message) {
                    "ADMIN_LIMIT_REACHED" -> state.strings.errAdminLimitReached
                    else -> err.localizedMessage ?: state.strings.errGenericSave
                }
                _snackbarEvents.tryEmit(msg)
            }
        }
    }

    fun demoteAdminToMember(targetUid: String) {
        val state = uiState.value
        val code = state.profile.companyCode
        if (!state.isAdminGroupMode || code.isBlank()) return
        viewModelScope.launch {
            localState.value = localState.value.copy(isBusy = true)
            val result = repository.demoteAdminToMember(code, targetUid)
            localState.value = localState.value.copy(isBusy = false)
            result.onSuccess {
                _snackbarEvents.tryEmit(state.strings.demotedToMemberMsg)
            }.onFailure { err ->
                val msg = when (err.message) {
                    "CANNOT_DEMOTE_PRIMARY_ADMIN" -> state.strings.errCannotDemotePrimaryAdmin
                    else -> err.localizedMessage ?: state.strings.errGenericSave
                }
                _snackbarEvents.tryEmit(msg)
            }
        }
    }

    // --- Profile & Real Company Sync Actions ---

    fun saveProfileAndCompany(
        displayName: String,
        mode: AppMode,
        role: UserRole,
        companyName: String,
        companyCode: String,
        onComplete: (Boolean) -> Unit
    ) {
        val strings = uiState.value.strings
        viewModelScope.launch {
            localState.value = localState.value.copy(isBusy = true)
            val result: Result<String> = when {
                mode == AppMode.PERSONAL -> {
                    repository.savePersonalModeProfile(
                        displayName = displayName,
                        keepCompanyName = companyName,
                        keepCompanyCode = companyCode
                    ).map { strings.profileSavedMsg }
                }
                role == UserRole.ADMIN -> {
                    repository.setupCompanyAsAdmin(
                        displayName = displayName,
                        companyName = companyName,
                        rawCompanyCode = companyCode
                    ).map { strings.companyCreatedMsg }
                }
                else -> {
                    repository.joinCompanyAsEmployee(
                        displayName = displayName,
                        rawCompanyCode = companyCode
                    ).map { strings.companyJoinedMsg }
                }
            }
            localState.value = localState.value.copy(isBusy = false)

            result.onSuccess { successMsg ->
                if (mode == AppMode.PERSONAL || role == UserRole.EMPLOYEE) {
                    localState.value = localState.value.copy(dashboardSubTab = DashboardSubTab.MY_OVERVIEW)
                }
                _snackbarEvents.tryEmit(successMsg)
                onComplete(true)
            }.onFailure { err ->
                val friendly = when (err.message) {
                    "INVALID_CODE" -> strings.errInvalidCompanyCode
                    "COMPANY_NOT_FOUND" -> strings.errCompanyNotFound
                    "CODE_OWNED_BY_OTHER_ADMIN" -> strings.errCodeOwnedByOtherAdmin
                    else -> err.localizedMessage ?: strings.errGenericSave
                }
                _snackbarEvents.tryEmit(friendly)
                onComplete(false)
            }
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            preferencesStorage.saveThemeMode(themeMode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesStorage.saveLanguage(language)
        }
    }

    fun buildExportCsv(): Pair<String, String> {
        val state = uiState.value
        val entriesToExport = if (state.isAdminGroupMode && state.dashboardSubTab == DashboardSubTab.TEAM_MONITOR) {
            state.companyEntries
        } else {
            state.myEntries
        }
        val content = CsvExporter.buildCsvContent(
            entries = entriesToExport,
            profile = state.profile,
            language = state.localPrefs.language
        )
        val fileName = CsvExporter.suggestedFileName(state.displayedYear, state.displayedMonth)
        return fileName to content
    }

    fun emitFeedback(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }

    private companion object {
        const val TAG = "OvertimeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
