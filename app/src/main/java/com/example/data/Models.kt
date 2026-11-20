package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppMode(val key: String) {
    PERSONAL("PERSONAL"),
    GROUP("GROUP");

    companion object {
        fun fromKey(key: String?): AppMode =
            entries.firstOrNull { it.key == key } ?: PERSONAL
    }
}

enum class UserRole(val key: String) {
    EMPLOYEE("EMPLOYEE"),
    ADMIN("ADMIN");

    companion object {
        fun fromKey(key: String?): UserRole =
            entries.firstOrNull { it.key == key } ?: EMPLOYEE
    }
}

enum class AppThemeMode(val key: String) {
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.key == key } ?: LIGHT
    }
}

enum class AppLanguage(val key: String) {
    INDONESIAN("ID"),
    ENGLISH("EN");

    companion object {
        fun fromKey(key: String?): AppLanguage =
            entries.firstOrNull { it.key == key } ?: INDONESIAN
    }
}

enum class OvertimeStatus(val key: String) {
    APPROVED("APPROVED"),
    PENDING("PENDING"),
    REJECTED("REJECTED");

    companion object {
        fun fromKey(key: String?): OvertimeStatus =
            entries.firstOrNull { it.key == key } ?: APPROVED
    }
}

// --- Firestore Type-Safe Documents (@Keep + @IgnoreExtraProperties + no-arg defaults) ---

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val mode: String = AppMode.PERSONAL.key,
    val role: String = UserRole.EMPLOYEE.key,
    val companyName: String = "",
    val companyCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PersonalOvertimeDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeName: String = "",
    val dateIso: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: Long = 0L,
    val overnight: Boolean = false,
    val notes: String = "",
    val status: String = OvertimeStatus.APPROVED.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CompanyDoc(
    @DocumentId val id: String = "",
    val companyCode: String = "",
    val companyName: String = "",
    val adminUid: String = "",
    val adminUids: List<String> = emptyList(),
    val adminName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveAdminUids(): List<String> =
        (listOf(adminUid) + adminUids).filter { it.isNotBlank() }.distinct()
}

@Keep
@IgnoreExtraProperties
data class CompanyMemberDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeName: String = "",
    val companyCode: String = "",
    val adminUid: String = "",
    val role: String = UserRole.EMPLOYEE.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CompanyOvertimeDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeName: String = "",
    val companyCode: String = "",
    val adminUid: String = "",
    val dateIso: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: Long = 0L,
    val overnight: Boolean = false,
    val notes: String = "",
    val status: String = OvertimeStatus.PENDING.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- Unified UI Domain Models ---

data class OvertimeRecord(
    val id: String,
    val userId: String,
    val employeeName: String,
    val companyCode: String,
    val adminUid: String,
    val personal: Boolean,
    val dateIso: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val overnight: Boolean,
    val notes: String,
    val status: OvertimeStatus,
    val createdAtMillis: Long
)

data class UserProfile(
    val name: String = "Overtime User",
    val email: String = "",
    val mode: AppMode = AppMode.PERSONAL,
    val role: UserRole = UserRole.EMPLOYEE,
    val companyName: String = "",
    val companyCode: String = "",
    val connectedAdminUid: String = "",
    val connectedAdminUids: List<String> = emptyList()
) {
    val isConnectedToCompany: Boolean
        get() = mode == AppMode.GROUP && companyCode.isNotBlank() && connectedAdminUid.isNotBlank()

    fun isAdminUser(uid: String): Boolean =
        uid.isNotBlank() && (connectedAdminUid == uid || uid in connectedAdminUids)

    companion object {
        const val DEFAULT_COMPANY_CODE = "PTMAJU-882"
        const val MAX_ADMINS = 8
        const val BROWSER_APP_URL = "https://ais-pre-edudyto27pemjpusprz25i-1008478148900.asia-east1.run.app"
    }
}

data class LocalAppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val language: AppLanguage = AppLanguage.INDONESIAN,
    val activeOvertimeStartMillis: Long? = null
)

data class EmployeeRecap(
    val userId: String,
    val employeeName: String,
    val totalMinutes: Int,
    val approvedMinutes: Int,
    val totalDays: Int,
    val pendingCount: Int,
    val totalEntries: Int
)

fun PersonalOvertimeDoc.toRecord(): OvertimeRecord = OvertimeRecord(
    id = id,
    userId = userId,
    employeeName = employeeName,
    companyCode = "",
    adminUid = "",
    personal = true,
    dateIso = dateIso,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes.toInt(),
    overnight = overnight,
    notes = notes,
    status = OvertimeStatus.fromKey(status),
    createdAtMillis = createdAt?.toDate()?.time ?: 0L
)

fun CompanyOvertimeDoc.toRecord(): OvertimeRecord = OvertimeRecord(
    id = id,
    userId = userId,
    employeeName = employeeName,
    companyCode = companyCode,
    adminUid = adminUid,
    personal = false,
    dateIso = dateIso,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes.toInt(),
    overnight = overnight,
    notes = notes,
    status = OvertimeStatus.fromKey(status),
    createdAtMillis = createdAt?.toDate()?.time ?: 0L
)
