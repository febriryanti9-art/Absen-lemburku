package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreOvertimeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna harus login dengan Google terlebih dahulu.")
    }

    fun normalizeCompanyCode(raw: String): String {
        return raw.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9_\\-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(24)
    }

    private fun isValidCompanyCode(code: String): Boolean {
        return code.length in 3..24 && Regex("^[A-Z0-9][A-Z0-9_\\-]+$").matches(code)
    }

    // --- Real-Time Snapshot Streams ---

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeCompany(companyCode: String, userId: String): Flow<CompanyDoc?> = flow {
        if (!isValidCompanyCode(companyCode)) {
            emit(null)
            return@flow
        }
        val path = "companies/$companyCode"
        emitAll(
            db.collection("companies").document(companyCode)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(CompanyDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observePersonalOvertime(userId: String): Flow<List<OvertimeRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/personal_overtime"
        emitAll(
            db.collection("users").document(userId)
                .collection("personal_overtime")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PersonalOvertimeDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toRecord() }
                        .sortedWith(compareByDescending<OvertimeRecord> { it.dateIso }.thenByDescending { it.createdAtMillis })
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMyCompanyOvertime(companyCode: String, userId: String): Flow<List<OvertimeRecord>> = flow {
        if (!isValidCompanyCode(companyCode) || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "companies/$companyCode/overtime_entries"
        emitAll(
            db.collection("companies").document(companyCode)
                .collection("overtime_entries")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CompanyOvertimeDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toRecord() }
                        .sortedWith(compareByDescending<OvertimeRecord> { it.dateIso }.thenByDescending { it.createdAtMillis })
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeAllCompanyOvertimeForAdmin(companyCode: String, adminUid: String): Flow<List<OvertimeRecord>> = flow {
        if (!isValidCompanyCode(companyCode) || adminUid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "companies/$companyCode/overtime_entries"
        emitAll(
            db.collection("companies").document(companyCode)
                .collection("overtime_entries")
                .whereEqualTo("adminUid", adminUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CompanyOvertimeDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .map { it.toRecord() }
                        .sortedWith(compareByDescending<OvertimeRecord> { it.dateIso }.thenByDescending { it.createdAtMillis })
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, adminUid)
                    throw error
                }
        )
    }

    fun observeCompanyMembersForAdmin(companyCode: String, adminUid: String): Flow<List<CompanyMemberDoc>> = flow {
        if (!isValidCompanyCode(companyCode) || adminUid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "companies/$companyCode/members"
        emitAll(
            db.collection("companies").document(companyCode)
                .collection("members")
                .whereEqualTo("adminUid", adminUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CompanyMemberDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.employeeName.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, OperationType.LIST, path, adminUid)
                    throw error
                }
        )
    }

    // --- Profile & Company Workspace Mutations ---

    suspend fun ensureUserProfileExists(defaultDisplayName: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = defaultDisplayName.trim().ifBlank { "Overtime User" }.take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "mode" to AppMode.PERSONAL.key,
                    "role" to UserRole.EMPLOYEE.key,
                    "companyName" to "",
                    "companyCode" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun savePersonalModeProfile(
        displayName: String,
        keepCompanyName: String,
        keepCompanyCode: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifBlank { "Overtime User" }.take(60)
            val safeCompanyName = keepCompanyName.trim().take(80)
            val normalizedCode = normalizeCompanyCode(keepCompanyCode).takeIf { isValidCompanyCode(it) } ?: ""
            val ref = db.collection("users").document(uid)
            path = ref.path
            upsertUserProfileDoc(
                uid = uid,
                displayName = safeName,
                mode = AppMode.PERSONAL,
                role = UserRole.EMPLOYEE,
                companyName = safeCompanyName,
                companyCode = normalizedCode
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun setupCompanyAsAdmin(
        displayName: String,
        companyName: String,
        rawCompanyCode: String
    ): Result<CompanyDoc> {
        var path = "companies"
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifBlank { "Admin" }.take(60)
            val safeCompanyName = companyName.trim().ifBlank { "Perusahaan Saya" }.take(80)
            val code = normalizeCompanyCode(rawCompanyCode)
            if (!isValidCompanyCode(code)) {
                return Result.failure(IllegalArgumentException("INVALID_CODE"))
            }

            val companyRef = db.collection("companies").document(code)
            path = companyRef.path
            val existingSnap = companyRef.get().await()
            if (existingSnap.exists()) {
                val existingCompany = existingSnap.toObject(CompanyDoc::class.java)
                if (existingCompany != null && existingCompany.adminUid != uid) {
                    return Result.failure(IllegalStateException("CODE_OWNED_BY_OTHER_ADMIN"))
                }
                companyRef.update(
                    mapOf(
                        "companyName" to safeCompanyName,
                        "adminName" to safeName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                companyRef.set(
                    mapOf(
                        "companyCode" to code,
                        "companyName" to safeCompanyName,
                        "adminUid" to uid,
                        "adminName" to safeName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Register/update Admin in members subcollection
            val memberRef = companyRef.collection("members").document(uid)
            path = memberRef.path
            val memberSnap = memberRef.get().await()
            if (memberSnap.exists()) {
                memberRef.update(
                    mapOf(
                        "employeeName" to safeName,
                        "role" to UserRole.ADMIN.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "employeeName" to safeName,
                        "companyCode" to code,
                        "adminUid" to uid,
                        "role" to UserRole.ADMIN.key,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Update user's synced profile
            path = "users/$uid"
            upsertUserProfileDoc(
                uid = uid,
                displayName = safeName,
                mode = AppMode.GROUP,
                role = UserRole.ADMIN,
                companyName = safeCompanyName,
                companyCode = code
            )

            Result.success(
                CompanyDoc(
                    id = code,
                    companyCode = code,
                    companyName = safeCompanyName,
                    adminUid = uid,
                    adminName = safeName
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun joinCompanyAsEmployee(
        displayName: String,
        rawCompanyCode: String
    ): Result<CompanyDoc> {
        var path = "companies"
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifBlank { "Karyawan" }.take(60)
            val code = normalizeCompanyCode(rawCompanyCode)
            if (!isValidCompanyCode(code)) {
                return Result.failure(IllegalArgumentException("INVALID_CODE"))
            }

            val companyRef = db.collection("companies").document(code)
            path = companyRef.path
            val companySnap = companyRef.get().await()
            if (!companySnap.exists()) {
                return Result.failure(IllegalStateException("COMPANY_NOT_FOUND"))
            }
            val company = companySnap.toObject(CompanyDoc::class.java)
                ?: return Result.failure(IllegalStateException("COMPANY_NOT_FOUND"))

            // Register/update Employee in company members subcollection
            val memberRef = companyRef.collection("members").document(uid)
            path = memberRef.path
            val memberSnap = memberRef.get().await()
            if (memberSnap.exists()) {
                memberRef.update(
                    mapOf(
                        "employeeName" to safeName,
                        "role" to UserRole.EMPLOYEE.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "employeeName" to safeName,
                        "companyCode" to code,
                        "adminUid" to company.adminUid,
                        "role" to UserRole.EMPLOYEE.key,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Update user's synced profile
            path = "users/$uid"
            upsertUserProfileDoc(
                uid = uid,
                displayName = safeName,
                mode = AppMode.GROUP,
                role = UserRole.EMPLOYEE,
                companyName = company.companyName,
                companyCode = code
            )

            Result.success(company)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Overtime Mutations ---

    suspend fun addPersonalOvertime(
        employeeName: String,
        dateIso: String,
        startTime: String,
        endTime: String,
        durationMinutes: Int,
        overnight: Boolean,
        notes: String
    ): Result<String> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val safeName = employeeName.trim().ifBlank { "User" }.take(60)
            val safeDuration = durationMinutes.coerceIn(1, 1440).toLong()
            val safeNotes = notes.trim().take(500)

            val docRef = db.collection("users").document(uid)
                .collection("personal_overtime").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "employeeName" to safeName,
                    "dateIso" to dateIso,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "durationMinutes" to safeDuration,
                    "overnight" to overnight,
                    "notes" to safeNotes,
                    "status" to OvertimeStatus.APPROVED.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCompanyOvertime(
        companyCode: String,
        adminUid: String,
        employeeName: String,
        isAdminSelf: Boolean,
        dateIso: String,
        startTime: String,
        endTime: String,
        durationMinutes: Int,
        overnight: Boolean,
        notes: String
    ): Result<String> {
        var path = "companies/$companyCode/overtime_entries"
        return try {
            val uid = requireUserId()
            val code = normalizeCompanyCode(companyCode)
            require(isValidCompanyCode(code) && adminUid.isNotBlank()) { "COMPANY_NOT_CONNECTED" }

            val safeName = employeeName.trim().ifBlank { "Karyawan" }.take(60)
            val safeDuration = durationMinutes.coerceIn(1, 1440).toLong()
            val safeNotes = notes.trim().take(500)
            val initialStatus = if (isAdminSelf && adminUid == uid) {
                OvertimeStatus.APPROVED.key
            } else {
                OvertimeStatus.PENDING.key
            }

            // Ensure membership exists before submitting
            val memberRef = db.collection("companies").document(code).collection("members").document(uid)
            val memberSnap = memberRef.get().await()
            if (!memberSnap.exists()) {
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "employeeName" to safeName,
                        "companyCode" to code,
                        "adminUid" to adminUid,
                        "role" to if (adminUid == uid) UserRole.ADMIN.key else UserRole.EMPLOYEE.key,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val docRef = db.collection("companies").document(code)
                .collection("overtime_entries").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "employeeName" to safeName,
                    "companyCode" to code,
                    "adminUid" to adminUid,
                    "dateIso" to dateIso,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "durationMinutes" to safeDuration,
                    "overnight" to overnight,
                    "notes" to safeNotes,
                    "status" to initialStatus,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewCompanyOvertime(
        companyCode: String,
        entryId: String,
        newStatus: OvertimeStatus
    ): Result<Unit> {
        val code = normalizeCompanyCode(companyCode)
        val path = "companies/$code/overtime_entries/$entryId"
        return try {
            requireUserId()
            require(isValidCompanyCode(code) && entryId.isNotBlank())
            require(newStatus == OvertimeStatus.APPROVED || newStatus == OvertimeStatus.REJECTED)
            db.collection("companies").document(code)
                .collection("overtime_entries").document(entryId)
                .update(
                    mapOf(
                        "status" to newStatus.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteOvertime(record: OvertimeRecord): Result<Unit> {
        var path = "overtime"
        return try {
            val uid = requireUserId()
            require(record.id.isNotBlank())
            val docRef = if (record.personal) {
                db.collection("users").document(uid)
                    .collection("personal_overtime").document(record.id)
            } else {
                db.collection("companies").document(record.companyCode)
                    .collection("overtime_entries").document(record.id)
            }
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun upsertUserProfileDoc(
        uid: String,
        displayName: String,
        mode: AppMode,
        role: UserRole,
        companyName: String,
        companyCode: String
    ) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        if (snap.exists()) {
            ref.update(
                mapOf(
                    "displayName" to displayName,
                    "mode" to mode.key,
                    "role" to role.key,
                    "companyName" to companyName,
                    "companyCode" to companyCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "mode" to mode.key,
                    "role" to role.key,
                    "companyName" to companyName,
                    "companyCode" to companyCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private fun logStreamErrorIfSignedIn(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
