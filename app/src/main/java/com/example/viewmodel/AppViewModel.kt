package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "FirebaseDebug"
    }

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e(TAG, "[FIREBASE AUTH ERROR] ${e.localizedMessage}", e)
        null
    }

    private val db: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        Log.e(TAG, "[FIRESTORE ERROR] ${e.localizedMessage}", e)
        null
    }

    private val _firebaseStatus = MutableStateFlow<String>("INITIALIZING")
    val firebaseStatus: StateFlow<String> = _firebaseStatus.asStateFlow()

    private val _firestoreStatus = MutableStateFlow<String>("INITIALIZING")
    val firestoreStatus: StateFlow<String> = _firestoreStatus.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userDoc = MutableStateFlow<UserDoc?>(null)
    val userDoc: StateFlow<UserDoc?> = _userDoc.asStateFlow()
    private val _userDocStatus = MutableStateFlow<String>("NOT AUTHENTICATED")
    val userDocStatus: StateFlow<String> = _userDocStatus.asStateFlow()

    // Collections
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _contents = MutableStateFlow<List<ContentItem>>(emptyList())
    val contents: StateFlow<List<ContentItem>> = _contents.asStateFlow()

    private val _slides = MutableStateFlow<List<SlideItem>>(emptyList())
    val slides: StateFlow<List<SlideItem>> = _slides.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _audios = MutableStateFlow<List<AudioItem>>(emptyList())
    val audios: StateFlow<List<AudioItem>> = _audios.asStateFlow()

    private val _storageFiles = MutableStateFlow<List<StorageFileItem>>(emptyList())
    val storageFiles: StateFlow<List<StorageFileItem>> = _storageFiles.asStateFlow()

    private val _firestoreNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _banners = MutableStateFlow<List<BannerItem>>(BannerItem.getDefaultMilitaryBanners())
    val banners: StateFlow<List<BannerItem>> = _banners.asStateFlow()

    private val _questions = MutableStateFlow<List<QuestionItem>>(QuestionItem.getDefaultQuestionBank())
    val questions: StateFlow<List<QuestionItem>> = _questions.asStateFlow()

    private val _examSessions = MutableStateFlow<List<ExamSessionDoc>>(emptyList())
    val examSessions: StateFlow<List<ExamSessionDoc>> = _examSessions.asStateFlow()

    private val _broadcasts = MutableStateFlow<List<InternalBroadcastItem>>(emptyList())
    val broadcasts: StateFlow<List<InternalBroadcastItem>> = _broadcasts.asStateFlow()

    private val _userExamResults = MutableStateFlow<List<ExamResultDoc>>(emptyList())
    val userExamResults: StateFlow<List<ExamResultDoc>> = _userExamResults.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _progressList = MutableStateFlow<List<ProgressDoc>>(emptyList())
    val progressList: StateFlow<List<ProgressDoc>> = _progressList.asStateFlow()
    private val _progressStatus = MutableStateFlow<String>("NOT AUTHENTICATED")
    val progressStatus: StateFlow<String> = _progressStatus.asStateFlow()

    private val _recentLessonIds = MutableStateFlow<List<String>>(emptyList())
    val recentLessonIds: StateFlow<List<String>> = _recentLessonIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isViewingLesson = MutableStateFlow(false)
    val isViewingLesson: StateFlow<Boolean> = _isViewingLesson.asStateFlow()

    fun setViewingLesson(isViewing: Boolean) {
        _isViewingLesson.value = isViewing
    }

    // Listeners
    private var coursesListener: ListenerRegistration? = null
    private var lessonsListener: ListenerRegistration? = null
    private var contentsListener: ListenerRegistration? = null
    private var slidesListener: ListenerRegistration? = null
    private var videosListener: ListenerRegistration? = null
    private var audiosListener: ListenerRegistration? = null
    private var storageFilesListener: ListenerRegistration? = null
    private var progressListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var bannersListener: ListenerRegistration? = null
    private var postersListener: ListenerRegistration? = null
    private var questionsListener: ListenerRegistration? = null
    private var examSessionsListener: ListenerRegistration? = null
    private var examResultsListener: ListenerRegistration? = null
    private var broadcastsListener: ListenerRegistration? = null
    private var bannersFromBannersColl: List<BannerItem> = emptyList()
    private var bannersFromPostersColl: List<BannerItem> = emptyList()

    // Giám sát trạng thái Khóa tài khoản từ Web Quản trị
    private var userDocListener: ListenerRegistration? = null
    private var accountDocListener: ListenerRegistration? = null
    private var accountSecurityCheckJob: Job? = null

    // Giám sát phiên đăng nhập duy nhất (tránh đăng nhập đồng thời trên nhiều thiết bị)
    private var localSessionId: String = ""

    private val _accountLockedEvent = MutableStateFlow<String?>(null)
    val accountLockedEvent: StateFlow<String?> = _accountLockedEvent.asStateFlow()

    fun clearAccountLockedEvent() {
        _accountLockedEvent.value = null
    }

    private fun updateEffectiveBanners() {
        val combined = if (bannersFromBannersColl.isNotEmpty()) {
            bannersFromBannersColl
        } else if (bannersFromPostersColl.isNotEmpty()) {
            bannersFromPostersColl
        } else {
            BannerItem.getDefaultMilitaryBanners()
        }
        val sortedList = combined.filter { it.active }.sortedBy { it.order }.take(5)
        _banners.value = if (sortedList.isNotEmpty()) sortedList else BannerItem.getDefaultMilitaryBanners()
    }

    init {
        restoreLocalUserSession()
        loadRecentLessons()
        checkConnectionAndStartRealtime()
    }

    private fun loadRecentLessons() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_recent_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("recent_lesson_ids", "") ?: ""
            if (raw.isNotBlank()) {
                _recentLessonIds.value = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[RECENT LESSONS] Error loading recent lessons: ${e.message}")
        }
    }

    fun recordLessonViewed(lessonId: String) {
        if (lessonId.isBlank()) return
        try {
            val current = _recentLessonIds.value.toMutableList()
            current.remove(lessonId)
            current.add(0, lessonId)
            val trimmed = current.take(20)
            _recentLessonIds.value = trimmed

            val prefs = getApplication<Application>().getSharedPreferences("vung4_recent_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("recent_lesson_ids", trimmed.joinToString(",")).apply()
            Log.i(TAG, "[RECENT LESSONS] Recorded viewed lesson $lessonId")
        } catch (e: Exception) {
            Log.e(TAG, "[RECENT LESSONS] Error saving recent lesson: ${e.message}")
        }
    }

    private fun saveUserSession(user: UserDoc) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_id", user.id)
                .putString("current_session_id", localSessionId)
                .putString("user_name", user.name)
                .putString("user_email", user.email)
                .putString("user_role", user.role)
                .putString("user_unit", user.unit)
                .putString("user_rank", user.rank)
                .putString("user_phone", user.phone)
                .putString("user_avatar", user.avatarUrl)
                .putString("user_avatar_${user.id}", user.avatarUrl)
                .apply()
            Log.i(TAG, "[SESSION] Saved login session for ${user.name} (${user.id}), session: $localSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "[SESSION SAVE ERROR] ${e.localizedMessage}", e)
        }
    }

    private fun clearUserSession() {
        try {
            localSessionId = ""
            val prefs = getApplication<Application>().getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.i(TAG, "[SESSION] Cleared saved session")
        } catch (e: Exception) {
            Log.e(TAG, "[SESSION CLEAR ERROR] ${e.localizedMessage}", e)
        }
    }

    private fun restoreLocalUserSession(): UserDoc? {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val id = prefs.getString("user_id", "") ?: ""
            localSessionId = prefs.getString("current_session_id", "") ?: ""
            val rawAvatar = prefs.getString("user_avatar_${id}", "")?.ifEmpty {
                prefs.getString("user_avatar", "")
            } ?: ""
            val localAvatar = resolveAndCacheAvatar(id, rawAvatar)
            if (isLoggedIn && id.isNotBlank()) {
                val restored = UserDoc(
                    id = id,
                    name = prefs.getString("user_name", "") ?: "",
                    email = prefs.getString("user_email", "") ?: "",
                    role = prefs.getString("user_role", "Học viên") ?: "Học viên",
                    unit = prefs.getString("user_unit", "Vùng 4 Hải Quân") ?: "Vùng 4 Hải Quân",
                    rank = prefs.getString("user_rank", "") ?: "",
                    phone = prefs.getString("user_phone", "") ?: "",
                    avatarUrl = localAvatar
                )
                _userDoc.value = restored
                _userDocStatus.value = "CONNECTED (${restored.name})"
                fetchProgress(restored.id)
                fetchExamResults(restored.id)
                syncPendingGuestProgressToFirestore(restored.id)
                startUserAccountRealtimeMonitoring(restored.id, restored.email)
                Log.i(TAG, "[SESSION] Restored login session for ${restored.name}")
                restored
            } else if (localAvatar.isNotEmpty()) {
                // Khôi phục ảnh đại diện cho chế độ khách nếu có
                val guestDoc = UserDoc(name = "Học viên", avatarUrl = localAvatar)
                _userDoc.value = guestDoc
                null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SESSION RESTORE ERROR] ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Kiểm tra trạng thái Khóa của tài khoản người dùng từ dữ liệu Firestore
     */
    fun isUserDocLocked(doc: DocumentSnapshot): Boolean {
        if (!doc.exists()) {
            return false
        }

        // 1. Kiểm tra các cờ boolean rõ ràng (explicit boolean)
        val isLockedExplicit = doc.getBoolean("isLocked")
            ?: doc.getBoolean("locked")
            ?: doc.getBoolean("isBlocked")
            ?: doc.getBoolean("blocked")
            ?: doc.getBoolean("isDisabled")
            ?: doc.getBoolean("disabled")
            ?: doc.getBoolean("khoa")
            ?: doc.getBoolean("biKhoa")
            ?: doc.getBoolean("tamKhoa")
            ?: doc.getBoolean("isBan")
            ?: doc.getBoolean("banned")

        // Nếu Web Admin đã set rõ ràng cờ boolean false -> ĐÃ MỞ KHÓA
        if (isLockedExplicit == false) {
            return false
        }
        // Nếu cờ boolean là true -> BỊ KHÓA
        if (isLockedExplicit == true) {
            return true
        }

        val activeExplicit = doc.getBoolean("active")
            ?: doc.getBoolean("isActive")
            ?: doc.getBoolean("enabled")
            ?: doc.getBoolean("enable")
            ?: doc.getBoolean("hoatDong")

        // Nếu Web Admin đã set active = true -> ĐÃ MỞ KHÓA
        if (activeExplicit == true) {
            return false
        }
        // Nếu active = false -> BỊ KHÓA
        if (activeExplicit == false) {
            return true
        }

        // 2. Kiểm tra chuỗi cờ locked/active dạng String ("false", "true", "0", "1")
        val rawLockedStr = (doc.getString("locked") ?: doc.getString("isLocked") ?: doc.getString("khoa") ?: "").trim().lowercase()
        if (rawLockedStr == "false" || rawLockedStr == "0" || rawLockedStr == "no" || rawLockedStr == "unlocked" || rawLockedStr == "mở khóa" || rawLockedStr == "mo_khoa") {
            return false
        }
        if (rawLockedStr == "true" || rawLockedStr == "1" || rawLockedStr == "yes" || rawLockedStr == "locked" || rawLockedStr == "khoa" || rawLockedStr == "khóa" || rawLockedStr == "khoá") {
            return true
        }

        val rawActiveStr = (doc.getString("active") ?: doc.getString("isActive") ?: doc.getString("enabled") ?: "").trim().lowercase()
        if (rawActiveStr == "true" || rawActiveStr == "1" || rawActiveStr == "yes" || rawActiveStr == "active" || rawActiveStr == "hoat_dong") {
            return false
        }
        if (rawActiveStr == "false" || rawActiveStr == "0" || rawActiveStr == "no" || rawActiveStr == "inactive") {
            return true
        }

        // 3. Kiểm tra chuỗi trạng thái (status, trangThai, state, accountStatus, lockStatus)
        val rawStatus = (
            doc.getString("status")
                ?: doc.getString("trangThai")
                ?: doc.getString("trang_thai")
                ?: doc.getString("state")
                ?: doc.getString("accountStatus")
                ?: doc.getString("lockStatus")
                ?: ""
        ).trim().lowercase()

        if (rawStatus.isNotBlank()) {
            // Danh sách từ khóa MỞ KHÓA / HOẠT ĐỘNG (Ưu tiên kiểm tra trước để tránh nhận diện nhầm!)
            val unlockedKeywords = listOf(
                "unlocked", "unlock", "mo_khoa", "mở khóa", "mở khoá", "mo khoa",
                "huy_khoa", "hủy khóa", "hủy khoá", "active", "hoat_dong", "hoạt động",
                "dang_hoat_dong", "đang hoạt động", "normal", "binh_thuong", "bình thường",
                "enabled", "enable", "open", "ok"
            )
            if (unlockedKeywords.any { rawStatus == it || rawStatus.contains(it) }) {
                return false
            }

            // Danh sách từ khóa KHÓA TÀI KHOẢN chính xác
            val lockedExactMatches = listOf(
                "locked", "lock", "khoa", "khóa", "khoá", "bi_khoa", "bị khóa", "bị khoá",
                "tam_khoa", "tạm khóa", "tạm khoá", "disabled", "blocked", "banned",
                "inactive", "deactivated", "vo_hieu_hoa", "vô hiệu hóa",
                "ngung_hoat_dong", "ngưng hoạt động", "close", "closed", "stop"
            )
            if (lockedExactMatches.any { rawStatus == it }) {
                return true
            }

            // Kiểm tra chứa cụm từ khóa nhưng TUYỆT ĐỐI LOẠI TRỪ các từ phủ định "mở", "hủy", "un"
            val hasLockedSubstr = (rawStatus.contains("khoa") || rawStatus.contains("khóa") || rawStatus.contains("khoá") || rawStatus.contains("locked") || rawStatus.contains("blocked") || rawStatus.contains("disabled"))
            val hasNegation = rawStatus.contains("mở") || rawStatus.contains("mo_") || rawStatus.contains("hủy") || rawStatus.contains("huy_") || rawStatus.contains("un") || rawStatus.contains("không") || rawStatus.contains("khong")
            if (hasLockedSubstr && !hasNegation) {
                return true
            }
        }

        return false
    }

    /**
     * Lắng nghe thời gian thực trạng thái tài khoản từ Web Quản trị.
     * Khi Quản trị viên trên Web bấm "Khóa", ứng dụng sẽ phát hiện ngay tức thì.
     * Đồng thời phát hiện khi tài khoản được đăng nhập trên một thiết bị khác.
     */
    fun startUserAccountRealtimeMonitoring(userId: String, userEmail: String = "") {
        stopUserAccountRealtimeMonitoring()
        if (db == null || userId.isBlank() || userId == "guest") return

        Log.i(TAG, "[SECURITY] Bắt đầu giám sát thời gian thực trạng thái tài khoản: $userId (Session: $localSessionId)")

        // 1. Lắng nghe trực tiếp Firestore collection "users"
        try {
            userDocListener = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[SECURITY ERROR] users listener: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        if (!snapshot.exists()) {
                            // Chỉ kick out nếu thực sự snapshot xác nhận từ server (không phải lỗi tạm thời hay cache)
                            if (!snapshot.metadata.isFromCache) {
                                forceKickOutDueToLock("Tài khoản của đồng chí đã bị xóa khỏi hệ thống bởi Quản trị viên.")
                            }
                        } else if (isUserDocLocked(snapshot)) {
                            forceKickOutDueToLock("Tài khoản của đồng chí đã bị Quản trị viên trên Web khóa.")
                        } else {
                            // Kiểm tra xung đột phiên đăng nhập trên nhiều thiết bị
                            val remoteSession = snapshot.getString("currentSessionId")
                                ?: snapshot.getString("sessionId")
                                ?: snapshot.getString("activeSessionId")
                            if (!remoteSession.isNullOrBlank() && localSessionId.isNotBlank() && remoteSession != localSessionId) {
                                val otherDevice = snapshot.getString("lastDeviceId") ?: "thiết bị khác"
                                forceKickOutDueToOtherDeviceLogin(otherDevice)
                                return@addSnapshotListener
                            }

                            // Tự động đồng bộ ảnh đại diện nếu được thay đổi từ thiết bị khác
                            val remoteAvatar = snapshot.getString("avatarUrl")
                                ?: snapshot.getString("avatar")
                                ?: snapshot.getString("avatarBase64")
                                ?: snapshot.getString("photoUrl")
                                ?: ""
                            if (remoteAvatar.isNotBlank()) {
                                val current = _userDoc.value
                                val resolved = resolveAndCacheAvatar(userId, remoteAvatar)
                                if (current != null && resolved.isNotBlank() && current.avatarUrl != resolved) {
                                    _userDoc.value = current.copy(avatarUrl = resolved)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[SECURITY] Failed to attach users listener: ${e.message}")
        }

        // 2. Lắng nghe thêm collection "accounts" nếu tài khoản được quản lý ở đó
        try {
            accountDocListener = db.collection("accounts").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        if (isUserDocLocked(snapshot)) {
                            forceKickOutDueToLock("Tài khoản của đồng chí đã bị Quản trị viên trên Web khóa.")
                        } else {
                            val remoteSession = snapshot.getString("currentSessionId")
                                ?: snapshot.getString("sessionId")
                                ?: snapshot.getString("activeSessionId")
                            if (!remoteSession.isNullOrBlank() && localSessionId.isNotBlank() && remoteSession != localSessionId) {
                                val otherDevice = snapshot.getString("lastDeviceId") ?: "thiết bị khác"
                                forceKickOutDueToOtherDeviceLogin(otherDevice)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[SECURITY] Failed to attach accounts listener: ${e.message}")
        }

        // 3. Fallback định kỳ (mỗi 4 giây) đối chiếu trực tiếp document trên Server
        accountSecurityCheckJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(4000)
                try {
                    // Kiểm tra Firebase Auth nếu có
                    if (isFirebaseApiKeyValid() && auth != null && auth.currentUser != null) {
                        try {
                            auth.currentUser?.reload()?.await()
                        } catch (_: Exception) {}
                    }

                    // Đối chiếu lại trực tiếp với Firestore (ưu tiên SERVER để tránh cache)
                    if (db != null) {
                        try {
                            val userDoc = try {
                                db.collection("users").document(userId).get(com.google.firebase.firestore.Source.SERVER).await()
                            } catch (_: Exception) {
                                db.collection("users").document(userId).get().await()
                            }
                            if (userDoc.exists()) {
                                if (isUserDocLocked(userDoc)) {
                                    withContext(Dispatchers.Main) {
                                        forceKickOutDueToLock("Tài khoản của đồng chí đã bị Quản trị viên trên Web khóa.")
                                    }
                                    break
                                }
                                val remoteSession = userDoc.getString("currentSessionId")
                                    ?: userDoc.getString("sessionId")
                                    ?: userDoc.getString("activeSessionId")
                                if (!remoteSession.isNullOrBlank() && localSessionId.isNotBlank() && remoteSession != localSessionId) {
                                    val otherDevice = userDoc.getString("lastDeviceId") ?: "thiết bị khác"
                                    withContext(Dispatchers.Main) {
                                        forceKickOutDueToOtherDeviceLogin(otherDevice)
                                    }
                                    break
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun stopUserAccountRealtimeMonitoring() {
        try {
            userDocListener?.remove()
            userDocListener = null
            accountDocListener?.remove()
            accountDocListener = null
            accountSecurityCheckJob?.cancel()
            accountSecurityCheckJob = null
        } catch (e: Exception) {
            Log.w(TAG, "[SECURITY] Error stopping monitoring: ${e.message}")
        }
    }

    /**
     * Xử lý và lưu bộ nhớ đệm ảnh đại diện cục bộ từ dữ liệu Firestore (hỗ trợ Base64, URL Web, file nội bộ)
     * Đảm bảo khi đăng nhập trên máy khác, ảnh đại diện lập tức hiển thị đồng bộ và sắc nét.
     */
    fun resolveAndCacheAvatar(userId: String, rawAvatar: String): String {
        val context = getApplication<Application>()
        val avatarDir = java.io.File(context.filesDir, "avatars")
        if (!avatarDir.exists()) avatarDir.mkdirs()
        val targetFile = java.io.File(avatarDir, "avatar_${userId}.jpg")

        if (rawAvatar.isBlank()) {
            return if (targetFile.exists()) targetFile.absolutePath else ""
        }

        // Trường hợp 1: Dữ liệu ảnh là chuỗi Base64
        if (rawAvatar.startsWith("data:image/") || (rawAvatar.length > 150 && !rawAvatar.startsWith("http") && !rawAvatar.startsWith("/"))) {
            try {
                val base64Content = if (rawAvatar.contains(",")) {
                    rawAvatar.substringAfter(",")
                } else {
                    rawAvatar
                }
                val imageBytes = Base64.decode(base64Content.trim(), Base64.DEFAULT)
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    targetFile.writeBytes(imageBytes)
                    val prefs = context.getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("user_avatar_${userId}", targetFile.absolutePath)
                        .putString("user_avatar", targetFile.absolutePath)
                        .apply()
                    return targetFile.absolutePath
                }
            } catch (e: Exception) {
                Log.w(TAG, "[AVATAR] Lỗi giải mã Base64 avatar: ${e.message}")
            }
        }

        // Trường hợp 2: URL Internet / Web Admin (http:// hoặc https://)
        if (rawAvatar.startsWith("http://") || rawAvatar.startsWith("https://")) {
            return rawAvatar
        }

        // Trường hợp 3: Đường dẫn file cục bộ (có thể từ thiết bị này hoặc thiết bị khác)
        if (rawAvatar.startsWith("/")) {
            val file = java.io.File(rawAvatar)
            if (file.exists()) {
                return rawAvatar
            } else if (targetFile.exists()) {
                // File từ máy khác không có, nhưng máy này đã có cache file chuẩn
                return targetFile.absolutePath
            } else {
                return ""
            }
        }

        return rawAvatar
    }

    /**
     * Buộc đăng xuất ngay lập tức khi phát hiện tài khoản đã đăng nhập trên thiết bị khác
     */
    fun forceKickOutDueToOtherDeviceLogin(deviceName: String = "thiết bị khác") {
        val hasUser = _userDoc.value != null && !_userDoc.value!!.id.isNullOrBlank()
        val hasFbUser = _currentUser.value != null
        if (!hasUser && !hasFbUser) return

        val reason = "Tài khoản của đồng chí vừa được đăng nhập trên một thiết bị khác ($deviceName). Để bảo mật thông tin, phiên làm việc trên thiết bị này đã tự động kết thúc."
        Log.w(TAG, "[SECURITY] KÍCH HOẠT ĐÁ VĂNG KHỎI APP DO ĐĂNG NHẬP THIẾT BỊ KHÁC: $reason")

        // 1. Tắt màn hình xem bài học ngay lập tức
        _isViewingLesson.value = false

        // 2. Dừng giám sát và xóa phiên cục bộ
        stopUserAccountRealtimeMonitoring()
        localSessionId = ""
        clearUserSession()
        _currentUser.value = null
        _userDoc.value = null
        _userDocStatus.value = "DISCONNECTED (Đã đăng nhập trên $deviceName)"
        _progressList.value = emptyList()
        _progressStatus.value = "DISCONNECTED"
        _userExamResults.value = emptyList()
        try {
            auth?.signOut()
        } catch (_: Exception) {}

        // 3. Kích hoạt sự kiện để MainScreen hiện Dialog cảnh báo và chuyển về màn Đăng nhập
        _accountLockedEvent.value = reason
    }

    /**
     * Đá văng người dùng đang mở App về màn hình Đăng nhập ngay lập tức khi bị khóa
     */
    fun forceKickOutDueToLock(reason: String = "Tài khoản của đồng chí đã bị Quản trị viên trên Web khóa.") {
        val hasUser = _userDoc.value != null && !_userDoc.value!!.id.isNullOrBlank()
        val hasFbUser = _currentUser.value != null
        if (!hasUser && !hasFbUser) return

        Log.w(TAG, "[SECURITY] KÍCH HOẠT ĐÁ VĂNG KHỎI APP DO TÀI KHOẢN BỊ KHÓA: $reason")

        // 1. Tắt màn hình xem bài học ngay lập tức
        _isViewingLesson.value = false

        // 2. Dừng giám sát và đăng xuất
        stopUserAccountRealtimeMonitoring()
        localSessionId = ""
        clearUserSession()
        _currentUser.value = null
        _userDoc.value = null
        _userDocStatus.value = "LOCKED"
        _progressList.value = emptyList()
        _progressStatus.value = "LOCKED"
        _userExamResults.value = emptyList()
        try {
            auth?.signOut()
        } catch (_: Exception) {}

        // 3. Kích hoạt sự kiện khóa tài khoản để giao diện đưa về màn hình Đăng nhập ngay lập tức
        _accountLockedEvent.value = reason
    }

    fun updateUserAvatar(uri: android.net.Uri, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val avatarDir = java.io.File(context.filesDir, "avatars")
                if (!avatarDir.exists()) avatarDir.mkdirs()

                val userId = _userDoc.value?.id.takeIf { !it.isNullOrBlank() } ?: "local_user"
                val targetFile = java.io.File(avatarDir, "avatar_${userId}.jpg")

                // 1. Đọc và nén ảnh
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    throw Exception("Không thể đọc định dạng hình ảnh đã chọn")
                }

                // Căn giữa hình vuông và nén kích thước tối đa 256x256 px
                val maxDim = 256
                val width = originalBitmap.width
                val height = originalBitmap.height
                val cropSize = width.coerceAtMost(height)
                val cropX = (width - cropSize) / 2
                val cropY = (height - cropSize) / 2
                val croppedBitmap = Bitmap.createBitmap(originalBitmap, cropX, cropY, cropSize, cropSize)

                val scaledBitmap = if (cropSize > maxDim) {
                    Bitmap.createScaledBitmap(croppedBitmap, maxDim, maxDim, true)
                } else {
                    croppedBitmap
                }

                val baos = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val imageBytes = baos.toByteArray()

                // Lưu file cục bộ vào bộ nhớ trong máy
                targetFile.writeBytes(imageBytes)
                val avatarPath = targetFile.absolutePath

                // 2. Tạo chuỗi Base64 Data URI đồng bộ đa nền tảng
                val base64Data = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // Lưu vào SharedPreferences của máy hiện tại
                val prefs = context.getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("user_avatar_${userId}", avatarPath)
                    .putString("user_avatar", avatarPath)
                    .putString("user_avatar_base64_${userId}", base64Data)
                    .apply()

                // Cập nhật State tức thì trên giao diện
                val current = _userDoc.value
                val updated = if (current != null) {
                    current.copy(avatarUrl = avatarPath)
                } else {
                    UserDoc(name = "Học viên", avatarUrl = avatarPath)
                }
                _userDoc.value = updated

                // 3. Đồng bộ chuỗi Base64 lên Firestore để TẤT CẢ các thiết bị khác khi đăng nhập đều tải được ảnh
                if (db != null && current != null && current.id.isNotBlank() && current.id != "guest") {
                    try {
                        val updates = hashMapOf<String, Any>(
                            "avatarUrl" to base64Data,
                            "avatar" to base64Data,
                            "avatarBase64" to base64Data,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        db.collection("users").document(current.id)
                            .set(updates, SetOptions.merge()).await()
                        Log.i(TAG, "[AVATAR] Đã đồng bộ ảnh đại diện Base64 lên Firestore cho tài khoản ${current.id}")
                    } catch (dbEx: Exception) {
                        Log.w(TAG, "[AVATAR FIRESTORE SYNC] Lỗi đồng bộ: ${dbEx.localizedMessage}")
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[AVATAR ERROR] Lỗi cập nhật ảnh đại diện: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e.localizedMessage)
                }
            }
        }
    }

    fun changeUserPassword(oldPass: String, newPass: String, onComplete: (Boolean, String?) -> Unit) {
        val current = _userDoc.value
        if (current == null || current.id.isBlank()) {
            onComplete(false, "Vui lòng đăng nhập để đổi mật khẩu")
            return
        }
        val trimmedOld = oldPass.trim()
        val trimmedNew = newPass.trim()
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty()) {
            onComplete(false, "Mật khẩu không được để trống")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (db == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Lỗi kết nối cơ sở dữ liệu")
                    }
                    return@launch
                }

                // 1. Kiểm tra mật khẩu cũ trên Firestore
                val userRef = db.collection("users").document(current.id)
                val snapshot = userRef.get().await()
                if (!snapshot.exists()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Không tìm thấy thông tin tài khoản")
                    }
                    return@launch
                }

                val savedPassword = snapshot.getString("password") 
                    ?: snapshot.getString("matKhau") 
                    ?: snapshot.getString("pass")
                    ?: snapshot.getString("mat_khau")

                if (!savedPassword.isNullOrBlank() && savedPassword != trimmedOld) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Mật khẩu cũ không chính xác")
                    }
                    return@launch
                }

                // 2. Cập nhật trên Firebase Auth nếu có người dùng đăng nhập qua SDK
                if (isFirebaseApiKeyValid() && auth != null && auth.currentUser != null) {
                    try {
                        auth.currentUser?.updatePassword(trimmedNew)?.await()
                        Log.i(TAG, "[PASSWORD] Firebase Auth password updated successfully")
                    } catch (authEx: Exception) {
                        Log.w(TAG, "[PASSWORD AUTH SDK] Update failed: ${authEx.localizedMessage}")
                    }
                }

                // 3. Cập nhật trên Firestore
                val updates = mapOf(
                    "password" to trimmedNew,
                    "matKhau" to trimmedNew,
                    "pass" to trimmedNew,
                    "mat_khau" to trimmedNew
                )
                userRef.update(updates).await()
                Log.i(TAG, "[PASSWORD] Firestore document password updated successfully for ${current.id}")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PASSWORD ERROR] Failed to change password: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Lỗi hệ thống khi đổi mật khẩu")
                }
            }
        }
    }

    fun resetUserAvatar(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val userId = _userDoc.value?.id.takeIf { !it.isNullOrBlank() } ?: "local_user"
                val prefs = context.getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .remove("user_avatar_${userId}")
                    .remove("user_avatar")
                    .remove("user_avatar_base64_${userId}")
                    .apply()

                try {
                    val localFile = java.io.File(context.filesDir, "avatars/avatar_${userId}.jpg")
                    if (localFile.exists()) localFile.delete()
                } catch (_: Exception) {}

                val current = _userDoc.value
                if (current != null) {
                    _userDoc.value = current.copy(avatarUrl = "")
                    if (db != null && current.id.isNotBlank() && current.id != "guest") {
                        try {
                            val updates = hashMapOf<String, Any>(
                                "avatarUrl" to "",
                                "avatar" to "",
                                "avatarBase64" to "",
                                "updatedAt" to System.currentTimeMillis()
                            )
                            db.collection("users").document(current.id)
                                .set(updates, SetOptions.merge()).await()
                        } catch (e: Exception) {
                            Log.w(TAG, "[AVATAR RESET FIRESTORE] Notice: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[AVATAR RESET ERROR] ${e.message}")
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun checkConnectionAndStartRealtime() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val app = FirebaseApp.getInstance()
                Log.i(TAG, "[FIREBASE] Project: ${app.options.projectId}, AppName: ${app.name}")
                _firebaseStatus.value = "CONNECTED (${app.options.projectId})"
            } catch (e: Exception) {
                Log.e(TAG, "[FIREBASE ERROR] ${e.localizedMessage}", e)
                _firebaseStatus.value = "ERROR: ${e.localizedMessage}"
            }

            if (db != null) {
                _firestoreStatus.value = "CONNECTED"
                startRealtimeListeners()
            } else {
                _firestoreStatus.value = "ERROR: Firestore instance is null"
            }

            if (auth != null) {
                _currentUser.value = auth.currentUser
                auth.addAuthStateListener { firebaseAuth ->
                    val user = firebaseAuth.currentUser
                    _currentUser.value = user
                    if (user != null) {
                        fetchUserDoc(user.uid)
                        fetchProgress(user.uid)
                        fetchExamResults(user.uid)
                    } else {
                        val session = restoreLocalUserSession()
                        if (session == null) {
                            _userDoc.value = null
                            _userDocStatus.value = "NOT AUTHENTICATED"
                            _progressList.value = emptyList()
                            _progressStatus.value = "NOT AUTHENTICATED"
                            _userExamResults.value = emptyList()
                        }
                    }
                }
                auth.currentUser?.let { user ->
                    fetchUserDoc(user.uid)
                    fetchProgress(user.uid)
                    fetchExamResults(user.uid)
                }
            } else {
                val session = restoreLocalUserSession()
                if (session == null) {
                    _userDocStatus.value = "NOT AUTHENTICATED"
                }
            }
            _isLoading.value = false
        }
    }

    private fun startRealtimeListeners() {
        if (db == null) return

        // 1. courses
        try {
            coursesListener?.remove()
            coursesListener = db.collection("courses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[COURSES ERROR] ${error.code}: ${error.message}", error)
                        _errorMessage.value = "Courses Error: ${error.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { Course.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _courses.value = list
                        Log.i(TAG, "[COURSES] Documents: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[COURSES EXCEPTION] ${e.localizedMessage}", e)
        }

        // 2. lessons
        try {
            lessonsListener?.remove()
            lessonsListener = db.collection("lessons")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[LESSONS ERROR] ${error.code}: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { Lesson.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _lessons.value = list
                        Log.i(TAG, "[LESSONS] Documents: ${list.size}")
                        updateCombinedNotifications()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[LESSONS EXCEPTION] ${e.localizedMessage}", e)
        }

        // 3. contents
        try {
            contentsListener?.remove()
            contentsListener = db.collection("contents")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[CONTENTS ERROR] ${error.code}: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { ContentItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _contents.value = list
                        Log.i(TAG, "[CONTENTS] Documents: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[CONTENTS EXCEPTION] ${e.localizedMessage}", e)
        }

        // 4. slides
        try {
            slidesListener?.remove()
            slidesListener = db.collection("slides")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[SLIDES ERROR] ${error.code}: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { SlideItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _slides.value = list
                        Log.i(TAG, "[SLIDES] Documents: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[SLIDES EXCEPTION] ${e.localizedMessage}", e)
        }

        // 5. videos
        try {
            videosListener?.remove()
            videosListener = db.collection("videos")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[VIDEOS ERROR] ${error.code}: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { VideoItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _videos.value = list
                        Log.i(TAG, "[VIDEOS] Documents: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[VIDEOS EXCEPTION] ${e.localizedMessage}", e)
        }

        // 6. audios
        try {
            audiosListener?.remove()
            audiosListener = db.collection("audios")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[AUDIOS ERROR] ${error.code}: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { AudioItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _audios.value = list
                        Log.i(TAG, "[AUDIOS] Documents: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[AUDIOS EXCEPTION] ${e.localizedMessage}", e)
        }

        // 7. documents (storage files)
        try {
            storageFilesListener?.remove()
            storageFilesListener = db.collection("documents")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[DOCUMENTS ERROR] ${error.code}: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { StorageFileItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _storageFiles.value = list
                        Log.i(TAG, "[DOCUMENTS] Files count: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[DOCUMENTS EXCEPTION] ${e.localizedMessage}", e)
        }

        // 8. notifications từ Web Quản Trị
        try {
            notificationsListener?.remove()
            notificationsListener = db.collection("notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[NOTIFICATIONS ERROR] ${error.code}: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { NotificationItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _firestoreNotifications.value = list
                        updateCombinedNotifications()
                        Log.i(TAG, "[NOTIFICATIONS] Loaded ${list.size} from Firestore")
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[NOTIFICATIONS EXCEPTION] ${e.localizedMessage}")
        }

        // 9. banners & posters từ Web Quản Trị (Tự động cập nhật tức thời theo thời gian thực)
        try {
            bannersListener?.remove()
            bannersListener = db.collection("banners")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[BANNERS ERROR] ${error.code}: ${error.message}")
                        return@addSnapshotListener
                    }
                    bannersFromBannersColl = snapshot?.documents?.mapNotNull { 
                        try { BannerItem.fromDoc(it) } catch (e: Exception) { null }
                    } ?: emptyList()
                    updateEffectiveBanners()
                    Log.i(TAG, "[BANNERS] Realtime sync: ${bannersFromBannersColl.size} banners from Web Quản trị")
                }
        } catch (e: Exception) {
            Log.w(TAG, "[BANNERS EXCEPTION] ${e.localizedMessage}")
        }

        try {
            postersListener?.remove()
            postersListener = db.collection("posters")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[POSTERS ERROR] ${error.code}: ${error.message}")
                        return@addSnapshotListener
                    }
                    bannersFromPostersColl = snapshot?.documents?.mapNotNull { 
                        try { BannerItem.fromDoc(it) } catch (e: Exception) { null }
                    } ?: emptyList()
                    updateEffectiveBanners()
                    Log.i(TAG, "[POSTERS] Realtime sync: ${bannersFromPostersColl.size} posters from Web Quản trị")
                }
        } catch (e: Exception) {
            Log.w(TAG, "[POSTERS EXCEPTION] ${e.localizedMessage}")
        }

        // 8. questions / cauHoi / exam_questions / cauHoiKiemTra từ Web Quản trị
        try {
            questionsListener?.remove()
            questionsListener = db.collection("questions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[QUESTIONS ERROR] ${error.code}: ${error.message}")
                        return@addSnapshotListener
                    }
                    val defaultBank = QuestionItem.getDefaultQuestionBank()
                    if (snapshot != null && !snapshot.isEmpty) {
                        val remoteList = snapshot.documents.mapNotNull { 
                            try { QuestionItem.fromDoc(it) } catch (e: Exception) { null }
                        }
                        if (remoteList.isNotEmpty()) {
                            // Dùng 100% câu hỏi từ Web Quản trị
                            _questions.value = remoteList
                            Log.i(TAG, "[QUESTIONS] Synced EXCLUSIVELY from Web Quản trị: ${remoteList.size} questions")
                        } else {
                            _questions.value = defaultBank
                        }
                    } else {
                        // Kiểm tra bộ sưu tập dự phòng: "cauHoi", "exam_questions", "cauHoiKiemTra"
                        db.collection("cauHoi").get().addOnSuccessListener { cauHoiSnap ->
                            if (cauHoiSnap != null && !cauHoiSnap.isEmpty) {
                                val cauHoiList = cauHoiSnap.documents.mapNotNull { 
                                    try { QuestionItem.fromDoc(it) } catch (e: Exception) { null }
                                }
                                if (cauHoiList.isNotEmpty()) {
                                    _questions.value = cauHoiList
                                    Log.i(TAG, "[QUESTIONS] Synced from cauHoi collection: ${cauHoiList.size} questions")
                                    return@addOnSuccessListener
                                }
                            }
                            db.collection("exam_questions").get().addOnSuccessListener { eqSnap ->
                                if (eqSnap != null && !eqSnap.isEmpty) {
                                    val eqList = eqSnap.documents.mapNotNull {
                                        try { QuestionItem.fromDoc(it) } catch (e: Exception) { null }
                                    }
                                    if (eqList.isNotEmpty()) {
                                        _questions.value = eqList
                                        Log.i(TAG, "[QUESTIONS] Synced from exam_questions collection: ${eqList.size} questions")
                                        return@addOnSuccessListener
                                    }
                                }
                                db.collection("cauHoiKiemTra").get().addOnSuccessListener { chktSnap ->
                                    if (chktSnap != null && !chktSnap.isEmpty) {
                                        val chktList = chktSnap.documents.mapNotNull {
                                            try { QuestionItem.fromDoc(it) } catch (e: Exception) { null }
                                        }
                                        if (chktList.isNotEmpty()) {
                                            _questions.value = chktList
                                            Log.i(TAG, "[QUESTIONS] Synced from cauHoiKiemTra collection: ${chktList.size} questions")
                                            return@addOnSuccessListener
                                        }
                                    }
                                    _questions.value = defaultBank
                                }
                            }
                        }.addOnFailureListener {
                            _questions.value = defaultBank
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[QUESTIONS EXCEPTION] ${e.localizedMessage}")
        }

        // 9. exam_sessions / dot_thi từ Web Quản Trị
        try {
            examSessionsListener?.remove()
            examSessionsListener = db.collection("exam_sessions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[EXAM_SESSIONS ERROR] ${error.code}: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { ExamSessionDoc.fromDoc(it) } catch (e: Exception) { null }
                        }
                        if (list.isNotEmpty()) {
                            _examSessions.value = list
                            Log.i(TAG, "[EXAM_SESSIONS] Realtime sync: ${list.size} exam sessions from exam_sessions")
                        } else {
                            // Secondary fallback check for dot_thi / dotThi collections if Web Admin uses Vietnamese collection names
                            db.collection("dot_thi").get().addOnSuccessListener { dotThiSnap ->
                                if (dotThiSnap != null && !dotThiSnap.isEmpty) {
                                    val dtList = dotThiSnap.documents.mapNotNull {
                                        try { ExamSessionDoc.fromDoc(it) } catch (e: Exception) { null }
                                    }
                                    _examSessions.value = dtList
                                    Log.i(TAG, "[EXAM_SESSIONS] Sync: ${dtList.size} exam sessions from dot_thi")
                                } else {
                                    db.collection("dotThi").get().addOnSuccessListener { dt2Snap ->
                                        if (dt2Snap != null && !dt2Snap.isEmpty) {
                                            val dt2List = dt2Snap.documents.mapNotNull {
                                                try { ExamSessionDoc.fromDoc(it) } catch (e: Exception) { null }
                                            }
                                            _examSessions.value = dt2List
                                            Log.i(TAG, "[EXAM_SESSIONS] Sync: ${dt2List.size} exam sessions from dotThi")
                                        } else {
                                            _examSessions.value = emptyList()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[EXAM_SESSIONS EXCEPTION] ${e.localizedMessage}")
        }

        // 10. radio_broadcasts / broadcasts / truyen_thanh / ban_tin từ Web Quản Trị
        try {
            broadcastsListener?.remove()
            broadcastsListener = db.collection("radio_broadcasts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[RADIO_BROADCASTS ERROR] ${error.code}: ${error.message}")
                        // Thử fallback sang broadcasts nếu radio_broadcasts có lỗi quyền
                        db.collection("broadcasts").get().addOnSuccessListener { bSnap ->
                            if (bSnap != null && !bSnap.isEmpty) {
                                val bList = bSnap.documents.mapNotNull {
                                    try { InternalBroadcastItem.fromDoc(it) } catch (e: Exception) { null }
                                }.sortedByDescending { it.createdAt }
                                if (bList.isNotEmpty()) {
                                    _broadcasts.value = bList
                                }
                            }
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { 
                            try { InternalBroadcastItem.fromDoc(it) } catch (e: Exception) { null }
                        }.sortedByDescending { it.createdAt }
                        if (list.isNotEmpty()) {
                            _broadcasts.value = list
                            Log.i(TAG, "[RADIO_BROADCASTS] Realtime sync: ${list.size} broadcasts from Web Quản trị (radio_broadcasts)")
                            return@addSnapshotListener
                        }
                    }
                    // Thử các collection dự phòng nếu radio_broadcasts trống: broadcasts, truyen_thanh, ban_tin
                    db.collection("broadcasts").get().addOnSuccessListener { snapBc ->
                        if (snapBc != null && !snapBc.isEmpty) {
                            val bcList = snapBc.documents.mapNotNull { 
                                try { InternalBroadcastItem.fromDoc(it) } catch (e: Exception) { null }
                            }.sortedByDescending { it.createdAt }
                            if (bcList.isNotEmpty()) {
                                _broadcasts.value = bcList
                                Log.i(TAG, "[BROADCASTS] Realtime sync: ${bcList.size} broadcasts from broadcasts")
                                return@addOnSuccessListener
                            }
                        }
                        db.collection("truyen_thanh").get().addOnSuccessListener { snapTt ->
                            if (snapTt != null && !snapTt.isEmpty) {
                                val ttList = snapTt.documents.mapNotNull { 
                                    try { InternalBroadcastItem.fromDoc(it) } catch (e: Exception) { null }
                                }.sortedByDescending { it.createdAt }
                                if (ttList.isNotEmpty()) {
                                    _broadcasts.value = ttList
                                    Log.i(TAG, "[BROADCASTS] Realtime sync: ${ttList.size} broadcasts from truyen_thanh")
                                    return@addOnSuccessListener
                                }
                            }
                            db.collection("ban_tin").get().addOnSuccessListener { snapBt ->
                                if (snapBt != null && !snapBt.isEmpty) {
                                    val btList = snapBt.documents.mapNotNull { 
                                        try { InternalBroadcastItem.fromDoc(it) } catch (e: Exception) { null }
                                    }.sortedByDescending { it.createdAt }
                                    if (btList.isNotEmpty()) {
                                        _broadcasts.value = btList
                                        Log.i(TAG, "[BROADCASTS] Realtime sync: ${btList.size} broadcasts from ban_tin")
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[RADIO_BROADCASTS EXCEPTION] ${e.localizedMessage}")
        }
    }

    fun fetchExamResults(uid: String) {
        if (db == null) return
        try {
            examResultsListener?.remove()
            examResultsListener = db.collection("exam_results")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[EXAM_RESULTS ERROR] ${error.code}: ${error.message}")
                        return@addSnapshotListener
                    }
                    val userEmail = _userDoc.value?.email ?: _currentUser.value?.email ?: ""
                    val userName = _userDoc.value?.name ?: _currentUser.value?.displayName ?: ""

                    val list1 = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val item = ExamResultDoc.fromDoc(doc)
                            val docUserId = doc.getString("userId") ?: doc.getString("user_id") ?: doc.getString("nguoiDungId") ?: ""
                            val docEmail = doc.getString("userEmail") ?: doc.getString("email") ?: ""
                            val docName = doc.getString("userName") ?: doc.getString("hoTen") ?: ""

                            val isMatch = docUserId == uid ||
                                    (userEmail.isNotBlank() && docEmail.equals(userEmail, ignoreCase = true)) ||
                                    (userName.isNotBlank() && docName.equals(userName, ignoreCase = true)) ||
                                    (docUserId.isNotBlank() && uid.contains(docUserId))

                            if (isMatch) item else null
                        } catch (e: Exception) { null }
                    } ?: emptyList()

                    // Fallback to fetch additional results from ket_qua_thi collection on Firestore
                    db.collection("ket_qua_thi").get().addOnSuccessListener { ketQuaSnap ->
                        val list2 = ketQuaSnap?.documents?.mapNotNull { doc ->
                            try {
                                val item = ExamResultDoc.fromDoc(doc)
                                val docUserId = doc.getString("userId") ?: doc.getString("user_id") ?: doc.getString("nguoiDungId") ?: ""
                                val docEmail = doc.getString("userEmail") ?: doc.getString("email") ?: ""
                                val docName = doc.getString("userName") ?: doc.getString("hoTen") ?: ""

                                val isMatch = docUserId == uid ||
                                        (userEmail.isNotBlank() && docEmail.equals(userEmail, ignoreCase = true)) ||
                                        (userName.isNotBlank() && docName.equals(userName, ignoreCase = true)) ||
                                        (docUserId.isNotBlank() && uid.contains(docUserId))

                                if (isMatch) item else null
                            } catch (e: Exception) { null }
                        } ?: emptyList()

                        val combined = (list1 + list2).distinctBy { "${it.examId}_${it.timestamp}" }.sortedByDescending { it.timestamp }
                        _userExamResults.value = combined
                        Log.i(TAG, "[EXAM_RESULTS] Realtime sync: ${combined.size} results for user $uid")
                    }.addOnFailureListener {
                        _userExamResults.value = list1.sortedByDescending { it.timestamp }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "[EXAM_RESULTS EXCEPTION] ${e.localizedMessage}")
        }
    }

    private fun fetchUserDoc(uid: String) {
        if (db == null) return
        viewModelScope.launch {
            try {
                val doc = try {
                    db.collection("users").document(uid).get(com.google.firebase.firestore.Source.SERVER).await()
                } catch (_: Exception) {
                    db.collection("users").document(uid).get().await()
                }
                if (doc.exists()) {
                    if (isUserDocLocked(doc)) {
                        forceKickOutDueToLock("Tài khoản của đồng chí đã bị Quản trị viên trên Web khóa.")
                        return@launch
                    }

                    // Kiểm tra xem tài khoản có bị đăng nhập trên thiết bị khác không
                    val remoteSession = doc.getString("currentSessionId")
                        ?: doc.getString("sessionId")
                        ?: doc.getString("activeSessionId")
                    if (!remoteSession.isNullOrBlank() && localSessionId.isNotBlank() && remoteSession != localSessionId) {
                        val otherDevice = doc.getString("lastDeviceId") ?: "thiết bị khác"
                        forceKickOutDueToOtherDeviceLogin(otherDevice)
                        return@launch
                    }

                    // Tự động khởi tạo session token nếu tài khoản chưa từng được gán
                    if (localSessionId.isBlank()) {
                        if (!remoteSession.isNullOrBlank()) {
                            localSessionId = remoteSession
                        } else {
                            localSessionId = UUID.randomUUID().toString()
                            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
                            val sessionData = hashMapOf<String, Any>(
                                "currentSessionId" to localSessionId,
                                "lastLoginAt" to System.currentTimeMillis(),
                                "lastDeviceId" to deviceName
                            )
                            try {
                                db.collection("users").document(uid).set(sessionData, SetOptions.merge())
                            } catch (_: Exception) {}
                        }
                        val prefs = getApplication<Application>().getSharedPreferences("vung4_auth_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("current_session_id", localSessionId).apply()
                    }

                    val rawAvatar = doc.getString("avatarUrl")
                        ?: doc.getString("avatar")
                        ?: doc.getString("avatarBase64")
                        ?: doc.getString("photoUrl")
                        ?: ""
                    val resolvedAvatar = resolveAndCacheAvatar(uid, rawAvatar)
                    val userDocObj = UserDoc.fromDoc(doc)
                    val finalUserDoc = if (resolvedAvatar.isNotBlank()) {
                        userDocObj.copy(avatarUrl = resolvedAvatar)
                    } else {
                        userDocObj
                    }
                    _userDoc.value = finalUserDoc
                    _userDocStatus.value = "CONNECTED (${finalUserDoc.name})"
                    saveUserSession(finalUserDoc)
                    fetchProgress(finalUserDoc.id)
                    fetchExamResults(finalUserDoc.id)
                    syncPendingGuestProgressToFirestore(finalUserDoc.id)
                    startUserAccountRealtimeMonitoring(finalUserDoc.id, finalUserDoc.email)
                    Log.i(TAG, "[USERS] User doc found for UID: $uid")
                } else {
                    _userDocStatus.value = "NOT FOUND (Document does not exist)"
                    Log.w(TAG, "[USERS] Document not found for UID: $uid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[USERS ERROR] ${e.localizedMessage}", e)
                _userDocStatus.value = "ERROR: ${e.localizedMessage}"
            }
        }
    }

    fun getGuestProgressList(): List<ProgressDoc> {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_guest_progress", Context.MODE_PRIVATE)
            val set = prefs.getStringSet("guest_items", emptySet()) ?: emptySet()
            set.mapNotNull { entry ->
                val parts = entry.split("###")
                if (parts.size >= 2) {
                    ProgressDoc(
                        id = "guest_${parts[0]}",
                        userId = "guest",
                        lessonId = parts[0],
                        completed = parts[1].toBoolean(),
                        updatedAt = if (parts.size >= 3) parts[2].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGuestProgressItem(
        lessonId: String,
        completed: Boolean,
        score: Int? = null,
        totalQuestions: Int? = null,
        viewedSlides: Boolean = false,
        readContent: Boolean = false,
        passedQuiz: Boolean = false
    ) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_guest_progress", Context.MODE_PRIVATE)
            val current = prefs.getStringSet("guest_items", emptySet())?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$lessonId###") }
            current.add("$lessonId###$completed###${System.currentTimeMillis()}###${score ?: -1}###${totalQuestions ?: -1}###$viewedSlides###$readContent###$passedQuiz")
            prefs.edit().putStringSet("guest_items", current).apply()

            if (_currentUser.value == null && _userDoc.value == null) {
                val updated = _progressList.value.filter { it.lessonId != lessonId }.toMutableList()
                val percent = if (score != null && totalQuestions != null && totalQuestions > 0) (score * 100 / totalQuestions) else null
                updated.add(
                    ProgressDoc(
                        id = "guest_$lessonId",
                        userId = "guest",
                        lessonId = lessonId,
                        completed = completed,
                        score = score,
                        totalQuestions = totalQuestions,
                        scorePercentage = percent,
                        viewedSlides = viewedSlides,
                        readContent = readContent,
                        passedQuiz = passedQuiz,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _progressList.value = updated
                updateCombinedNotifications()
            }
            Log.i(TAG, "[GUEST PROGRESS] Saved offline progress for lesson $lessonId, completed=$completed, score=$score")
        } catch (e: Exception) {
            Log.e(TAG, "[GUEST PROGRESS SAVE ERROR] ${e.localizedMessage}")
        }
    }

    fun syncPendingGuestProgressToFirestore(uid: String) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("vung4_guest_progress", Context.MODE_PRIVATE)
                val set = prefs.getStringSet("guest_items", emptySet()) ?: emptySet()
                if (set.isNotEmpty()) {
                    for (entry in set) {
                        val parts = entry.split("###")
                        if (parts.size >= 2) {
                            val lessonId = parts[0]
                            val completed = parts[1].toBoolean()
                            val score = if (parts.size >= 4) parts[3].toIntOrNull()?.takeIf { it >= 0 } else null
                            val totalQuestions = if (parts.size >= 5) parts[4].toIntOrNull()?.takeIf { it >= 0 } else null
                            val viewedSlides = if (parts.size >= 6) parts[5].toBoolean() else false
                            val readContent = if (parts.size >= 7) parts[6].toBoolean() else false
                            val passedQuiz = if (parts.size >= 8) parts[7].toBoolean() else false
                            updateLessonProgress(
                                lessonId = lessonId,
                                completed = completed,
                                score = score,
                                totalQuestions = totalQuestions,
                                viewedSlides = viewedSlides,
                                readContent = readContent,
                                passedQuiz = passedQuiz
                            )
                        }
                    }
                    prefs.edit().clear().apply()
                    Log.i(TAG, "[SYNC] Synced pending guest progress (${set.size} items) to user $uid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[SYNC GUEST ERROR] ${e.localizedMessage}")
            }
        }
    }

    fun fetchProgress(uid: String) {
        if (db == null) {
            _progressList.value = getGuestProgressList()
            return
        }
        try {
            progressListener?.remove()
            progressListener = db.collection("progress")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[PROGRESS ERROR] ${error.code}: ${error.message}", error)
                        _progressStatus.value = "ERROR: ${error.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val userEmail = _userDoc.value?.email ?: _currentUser.value?.email ?: ""
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val item = ProgressDoc.fromDoc(doc)
                                val docUserId = doc.getString("userId") ?: doc.getString("user_id") ?: doc.getString("nguoiDungId") ?: ""
                                val docEmail = doc.getString("userEmail") ?: ""
                                if (docUserId == uid || (userEmail.isNotBlank() && docEmail.equals(userEmail, ignoreCase = true)) || doc.id.startsWith("${uid}_")) {
                                    item
                                } else null
                            } catch (e: Exception) { null }
                        }
                        // Gộp thêm tiến độ tạm thời của khách nếu chưa đồng bộ
                        val guestList = getGuestProgressList()
                        val combined = list.toMutableList()
                        for (g in guestList) {
                            if (combined.none { it.lessonId == g.lessonId }) {
                                combined.add(g)
                            }
                        }
                        _progressList.value = combined
                        _progressStatus.value = "CONNECTED (${combined.size} docs)"
                        Log.i(TAG, "[PROGRESS] Documents for user $uid: ${combined.size}")
                        updateCombinedNotifications()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[PROGRESS EXCEPTION] ${e.localizedMessage}", e)
            _progressStatus.value = "ERROR: ${e.localizedMessage}"
        }
    }

    fun updateLessonProgress(
        lessonId: String,
        completed: Boolean,
        score: Int? = null,
        totalQuestions: Int? = null,
        viewedSlides: Boolean = true,
        readContent: Boolean = true,
        passedQuiz: Boolean = true,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val user = _userDoc.value
        val currentFbUser = _currentUser.value
        val uid = user?.id ?: currentFbUser?.uid ?: user?.email

        if (uid.isNullOrBlank()) {
            saveGuestProgressItem(
                lessonId = lessonId,
                completed = completed,
                score = score,
                totalQuestions = totalQuestions,
                viewedSlides = viewedSlides,
                readContent = readContent,
                passedQuiz = passedQuiz
            )
            onError?.invoke("Chưa đăng nhập. Vui lòng đăng nhập tài khoản để tính điểm và lưu tiến độ vào hồ sơ Web Quản trị!")
            return
        }

        viewModelScope.launch {
            try {
                val lessonObj = _lessons.value.find { it.id == lessonId }
                val lessonTitle = lessonObj?.title ?: "Bài học chính trị"
                val userName = user?.name?.ifEmpty { currentFbUser?.displayName ?: currentFbUser?.email ?: "Học viên Vùng 4" } ?: "Học viên Vùng 4"
                val userEmail = user?.email?.ifEmpty { currentFbUser?.email ?: "" } ?: ""
                val userUnit = user?.unit?.ifEmpty { "Vùng 4 Hải Quân" } ?: "Vùng 4 Hải Quân"
                val userRank = user?.rank ?: ""
                val userRole = user?.role ?: "Học viên"

                val currentTime = System.currentTimeMillis()
                val scorePercent = if (score != null && totalQuestions != null && totalQuestions > 0) {
                    (score * 100 / totalQuestions)
                } else null

                val data = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "user_id" to uid,
                    "nguoiDungId" to uid,
                    "userName" to userName,
                    "userEmail" to userEmail,
                    "unit" to userUnit,
                    "donVi" to userUnit,
                    "rank" to userRank,
                    "role" to userRole,
                    "lessonId" to lessonId,
                    "lesson_id" to lessonId,
                    "baiHocId" to lessonId,
                    "lessonTitle" to lessonTitle,
                    "tenBaiHoc" to lessonTitle,
                    "completed" to completed,
                    "hoanThanh" to completed,
                    "isCompleted" to completed,
                    "viewedSlides" to viewedSlides,
                    "daXemSlide" to viewedSlides,
                    "readContent" to readContent,
                    "daDocNoiDung" to readContent,
                    "passedQuiz" to passedQuiz,
                    "daDat" to passedQuiz,
                    "status" to if (completed) "completed" else "in_progress",
                    "trangThai" to if (completed) "Đã hoàn thành" else "Đang học",
                    "updatedAt" to currentTime,
                    "device" to "Android App Vùng 4",
                    "source" to "mobile_app"
                )

                if (completed) {
                    data["completedAt"] = currentTime
                    data["thoiGianHoanThanh"] = currentTime
                }

                if (score != null) {
                    data["score"] = score
                    data["diem"] = score
                    data["correctAnswers"] = score
                    data["totalQuestions"] = totalQuestions ?: 0
                    data["tongSoCau"] = totalQuestions ?: 0
                    data["scorePercentage"] = scorePercent ?: 100
                    data["phanTramDiem"] = scorePercent ?: 100
                    data["passed"] = (score.toFloat() / (totalQuestions ?: 1).coerceAtLeast(1)) >= 0.5f
                }

                if (db != null) {
                    // 1. Lưu vào collection 'progress' với document ID định danh {userId}_{lessonId}
                    val docKey = "${uid}_${lessonId}"
                    db.collection("progress").document(docKey).set(data, SetOptions.merge()).await()

                    // Kiểm tra và cập nhật thêm nếu trước đó tài liệu có document ID khác
                    try {
                        val query = db.collection("progress")
                            .whereEqualTo("userId", uid)
                            .whereEqualTo("lessonId", lessonId)
                            .get()
                            .await()
                        for (doc in query.documents) {
                            if (doc.id != docKey) {
                                db.collection("progress").document(doc.id).set(data, SetOptions.merge()).await()
                            }
                        }
                    } catch (ignored: Exception) {}

                    // 2. Đồng bộ sang collection 'tienDoHocTap' để tương thích với Web Quản Trị
                    try {
                        db.collection("tienDoHocTap").document(docKey).set(data, SetOptions.merge()).await()
                    } catch (ignored: Exception) {}

                    // 3. Cập nhật bài học gần nhất vào tài liệu người dùng (collection 'users')
                    try {
                        val userUpdate = mutableMapOf<String, Any>(
                            "lastLessonId" to lessonId,
                            "lastLessonTitle" to lessonTitle,
                            "lastStudiedAt" to currentTime,
                            "updatedAt" to currentTime
                        )
                        if (score != null) {
                            userUpdate["lastScore"] = score
                            userUpdate["lastScorePercentage"] = scorePercent ?: 100
                        }
                        db.collection("users").document(uid).set(userUpdate, SetOptions.merge()).await()
                    } catch (ignored: Exception) {}

                    // 4. Ghi nhật ký học tập (study_logs) cho bảng điều khiển Web Admin
                    try {
                        val logEntry = mapOf(
                            "userId" to uid,
                            "userName" to userName,
                            "userEmail" to userEmail,
                            "unit" to userUnit,
                            "lessonId" to lessonId,
                            "lessonTitle" to lessonTitle,
                            "action" to if (completed) "COMPLETED_LESSON" else "STUDYING",
                            "score" to (score ?: 0),
                            "totalQuestions" to (totalQuestions ?: 0),
                            "scorePercentage" to (scorePercent ?: 0),
                            "viewedSlides" to viewedSlides,
                            "readContent" to readContent,
                            "passedQuiz" to passedQuiz,
                            "timestamp" to currentTime,
                            "createdAt" to currentTime
                        )
                        db.collection("study_logs").add(logEntry).await()
                    } catch (ignored: Exception) {}
                }

                // Cập nhật StateFlow nội bộ ngay lập tức
                val existing = _progressList.value.filter { it.lessonId != lessonId }.toMutableList()
                existing.add(
                    ProgressDoc(
                        id = "${uid}_${lessonId}",
                        userId = uid,
                        lessonId = lessonId,
                        completed = completed,
                        score = score,
                        totalQuestions = totalQuestions,
                        scorePercentage = scorePercent,
                        viewedSlides = viewedSlides,
                        readContent = readContent,
                        passedQuiz = passedQuiz,
                        updatedAt = currentTime
                    )
                )
                _progressList.value = existing
                updateCombinedNotifications()

                Log.i(TAG, "[PROGRESS] Successfully pushed progress to Web Admin for lesson $lessonId, completed=$completed, score=$score, percent=$scorePercent%")
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "[PROGRESS UPDATE ERROR] ${e.localizedMessage}", e)
                onError?.invoke("Lỗi kết nối máy chủ: ${e.localizedMessage}")
            }
        }
    }

    // Authentication State & Actions
    private val _authActionLoading = MutableStateFlow(false)
    val authActionLoading: StateFlow<Boolean> = _authActionLoading.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    private fun isFirebaseApiKeyValid(): Boolean {
        return try {
            val key = FirebaseApp.getInstance().options.apiKey
            key.isNotBlank() &&
                    !key.contains("dummy", ignoreCase = true) &&
                    !key.contains("example", ignoreCase = true) &&
                    key.startsWith("AIzaSy") &&
                    key.length >= 35
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Đăng nhập bằng tài khoản được cấp từ trang Quản Trị Web
     * Liên kết trực tiếp với dữ liệu Firestore Web Quản Trị (gdctv4-4e1f3)
     */
    fun loginWithAdminAccount(
        emailOrUsername: String,
        pass: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedInput = emailOrUsername.trim()
        val password = pass.trim()
        if (trimmedInput.isEmpty() || password.isEmpty()) {
            onError("Vui lòng nhập đầy đủ tài khoản và mật khẩu!")
            return
        }

        _authActionLoading.value = true
        _authMessage.value = null

        viewModelScope.launch {
            try {
                // 1. Tra cứu trực tiếp trong dữ liệu Quản Trị Web (Firestore collection 'users' / 'accounts')
                if (db != null) {
                    try {
                        var matchedDoc: com.google.firebase.firestore.DocumentSnapshot? = null

                        try {
                            // Luôn ưu tiên lấy từ SERVER để cập nhật ngay trạng thái mở khóa từ Web
                            val usersSnapshot = try {
                                db.collection("users").get(com.google.firebase.firestore.Source.SERVER).await()
                            } catch (_: Exception) {
                                db.collection("users").get().await()
                            }
                            for (doc in usersSnapshot.documents) {
                                val email = doc.getString("email") ?: ""
                                val username = doc.getString("username") ?: doc.getString("account") ?: doc.getString("taiKhoan") ?: ""
                                val phone = doc.getString("phone") ?: doc.getString("soDienThoai") ?: ""
                                val code = doc.getString("code") ?: doc.getString("maHocVien") ?: ""
                                val docId = doc.id

                                val matchesInput = email.equals(trimmedInput, ignoreCase = true) ||
                                        username.equals(trimmedInput, ignoreCase = true) ||
                                        phone == trimmedInput ||
                                        code.equals(trimmedInput, ignoreCase = true) ||
                                        docId.equals(trimmedInput, ignoreCase = true) ||
                                        (trimmedInput.contains("@") && email.equals(trimmedInput, ignoreCase = true)) ||
                                        (!trimmedInput.contains("@") && email.startsWith("$trimmedInput@", ignoreCase = true))

                                if (matchesInput) {
                                    matchedDoc = doc
                                    break
                                }
                            }
                        } catch (usersEx: Exception) {
                            Log.w(TAG, "[LOOKUP USERS] ${usersEx.localizedMessage}")
                        }

                        // Nếu chưa thấy trong 'users', thử tìm trong 'accounts'
                        if (matchedDoc == null) {
                            try {
                                val accSnapshot = try {
                                    db.collection("accounts").get(com.google.firebase.firestore.Source.SERVER).await()
                                } catch (_: Exception) {
                                    db.collection("accounts").get().await()
                                }
                                for (doc in accSnapshot.documents) {
                                    val email = doc.getString("email") ?: ""
                                    val username = doc.getString("username") ?: doc.getString("account") ?: ""
                                    if (email.equals(trimmedInput, ignoreCase = true) || 
                                        username.equals(trimmedInput, ignoreCase = true) ||
                                        doc.id.equals(trimmedInput, ignoreCase = true)) {
                                        matchedDoc = doc
                                        break
                                    }
                                }
                            } catch (ignored: Exception) {}
                        }

                        if (matchedDoc != null) {
                            // Lấy snapshot tươi mới nhất từ SERVER đối với document này để tránh cache
                            val freshDoc = try {
                                matchedDoc.reference.get(com.google.firebase.firestore.Source.SERVER).await()
                            } catch (_: Exception) {
                                matchedDoc
                            }
                            val docToCheck = if (freshDoc != null && freshDoc.exists()) freshDoc else matchedDoc

                            if (isUserDocLocked(docToCheck)) {
                                _authActionLoading.value = false
                                onError("Tài khoản của đồng chí đang bị khóa trên hệ thống Web. Vui lòng liên hệ cán bộ quản trị để được hỗ trợ!")
                                return@launch
                            }

                            val savedPassword = docToCheck.getString("password") 
                                ?: docToCheck.getString("matKhau") 
                                ?: docToCheck.getString("pass")
                                ?: docToCheck.getString("mat_khau")

                            // Nếu web admin có lưu mật khẩu thì kiểm tra khớp
                            if (!savedPassword.isNullOrBlank() && savedPassword != password) {
                                _authActionLoading.value = false
                                onError("Mật khẩu không chính xác. Vui lòng kiểm tra lại!")
                                return@launch
                            }

                            // Gọi FirebaseAuth đăng nhập bổ trợ để đồng bộ phiên nếu API key hợp lệ
                            if (isFirebaseApiKeyValid() && auth != null) {
                                val emailToTry = docToCheck.getString("email")?.ifEmpty { null }
                                    ?: if (!trimmedInput.contains("@")) "${trimmedInput.lowercase()}@gdctvung4.vn" else trimmedInput
                                try {
                                    val authResult = auth.signInWithEmailAndPassword(emailToTry, password).await()
                                    _currentUser.value = authResult.user
                                } catch (authEx: Exception) {
                                    Log.w(TAG, "[AUTH SDK] Background sign in non-critical: ${authEx.localizedMessage}")
                                    // Chú ý: Vì tài khoản trên Firestore của Web Quản Trị đã được xác nhận MỞ KHÓA,
                                    // không chặn người dùng nếu Firebase Auth SDK có độ trễ đồng bộ.
                                }
                            }

                            // Xác thực thành công tài khoản từ Web Quản Trị!
                            clearAccountLockedEvent()

                            // Tạo session token mới duy nhất cho thiết bị này để tránh đăng nhập nhiều thiết bị cùng lúc
                            val newSessionId = UUID.randomUUID().toString()
                            localSessionId = newSessionId
                            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

                            // Cập nhật session mới lên Firestore
                            if (db != null) {
                                try {
                                    val sessionData = hashMapOf<String, Any>(
                                        "currentSessionId" to newSessionId,
                                        "lastLoginAt" to System.currentTimeMillis(),
                                        "lastDeviceId" to deviceName
                                    )
                                    db.collection("users").document(matchedDoc.id).set(sessionData, SetOptions.merge()).await()
                                    try {
                                        db.collection("accounts").document(matchedDoc.id).set(sessionData, SetOptions.merge())
                                    } catch (_: Exception) {}
                                    Log.i(TAG, "[SESSION] Cập nhật session mới thành công trên server: $newSessionId")
                                } catch (sessEx: Exception) {
                                    Log.w(TAG, "[SESSION SYNC ERROR] ${sessEx.localizedMessage}")
                                }
                            }

                            // Giải mã và đồng bộ ảnh đại diện (nếu tài khoản đã đổi ảnh trên máy khác)
                            val rawAvatar = docToCheck.getString("avatarUrl")
                                ?: docToCheck.getString("avatar")
                                ?: docToCheck.getString("avatarBase64")
                                ?: docToCheck.getString("photoUrl")
                                ?: ""
                            val resolvedAvatar = resolveAndCacheAvatar(matchedDoc.id, rawAvatar)

                            val userDocObj = UserDoc.fromDoc(docToCheck)
                            val finalUserDoc = if (resolvedAvatar.isNotBlank()) {
                                userDocObj.copy(avatarUrl = resolvedAvatar)
                            } else {
                                userDocObj
                            }
                            _userDoc.value = finalUserDoc
                            _userDocStatus.value = "CONNECTED (${finalUserDoc.name})"
                            saveUserSession(finalUserDoc)
                            fetchProgress(matchedDoc.id)
                            fetchExamResults(matchedDoc.id)
                            syncPendingGuestProgressToFirestore(matchedDoc.id)
                            startUserAccountRealtimeMonitoring(finalUserDoc.id, finalUserDoc.email)
                            _authActionLoading.value = false
                            onSuccess()
                            return@launch
                        } else {
                            // Tài khoản KHÔNG ĐƯỢC ĐĂNG KÝ trên hệ thống quản trị -> KHÔNG ĐĂNG NHẬP ĐƯỢC
                            _authActionLoading.value = false
                            onError("Tài khoản chưa được đăng ký bởi hệ thống Web Quản trị. Vui lòng liên hệ cán bộ quản trị để được cấp tài khoản!")
                            return@launch
                        }
                    } catch (dbEx: Exception) {
                        Log.e(TAG, "[FIRESTORE LOGIN ERROR] ${dbEx.localizedMessage}", dbEx)
                        _authActionLoading.value = false
                        onError("Không thể kết nối máy chủ quản trị. Vui lòng thử lại!")
                        return@launch
                    }
                } else {
                    _authActionLoading.value = false
                    onError("Dữ liệu máy chủ chưa sẵn sàng, vui lòng thử lại!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[LOGIN ERROR] ${e.localizedMessage}", e)
                _authActionLoading.value = false
                onError("Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản và mật khẩu!")
            }
        }
    }

    /**
     * Đăng xuất và trở về chế độ Khách (Guest Mode)
     */
    fun logout() {
        stopUserAccountRealtimeMonitoring()
        val uid = _userDoc.value?.id
        if (db != null && !uid.isNullOrBlank() && uid != "guest") {
            try {
                // Xóa session trên Firestore nếu máy này chủ động đăng xuất
                db.collection("users").document(uid).update("currentSessionId", "")
            } catch (_: Exception) {}
        }
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "[LOGOUT ERROR] ${e.localizedMessage}", e)
        }
        clearUserSession()
        localSessionId = ""
        _currentUser.value = null
        _userDoc.value = null
        _userDocStatus.value = "NOT AUTHENTICATED"
        _progressList.value = emptyList()
        _progressStatus.value = "NOT AUTHENTICATED"
        _userExamResults.value = emptyList()
        _authMessage.value = "Đã chuyển về chế độ Khách"
        updateCombinedNotifications()
    }

    private fun getReadNotificationIds(): Set<String> {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_notif_prefs", Context.MODE_PRIVATE)
            prefs.getStringSet("read_ids", emptySet()) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun markNotificationAsRead(id: String) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_notif_prefs", Context.MODE_PRIVATE)
            val current = prefs.getStringSet("read_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(id)
            prefs.edit().putStringSet("read_ids", current).apply()
            updateCombinedNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "[NOTIF READ ERROR] ${e.localizedMessage}")
        }
    }

    fun markAllNotificationsAsRead() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("vung4_notif_prefs", Context.MODE_PRIVATE)
            val allIds = _notifications.value.map { it.id }.toSet()
            prefs.edit().putStringSet("read_ids", allIds).apply()
            updateCombinedNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "[NOTIF READ ALL ERROR] ${e.localizedMessage}")
        }
    }

    fun pushReminderToDevice(title: String? = null, message: String? = null) {
        val app = getApplication<Application>()
        if (!title.isNullOrBlank() && !message.isNullOrBlank()) {
            NotificationHelper.sendNotification(app, title, message)
            return
        }

        val completedLessonIds = _progressList.value.filter { it.completed }.map { it.lessonId }.toSet()
        val incomplete = _lessons.value.filter { !completedLessonIds.contains(it.id) }

        if (incomplete.isNotEmpty()) {
            val lesson = incomplete.first()
            NotificationHelper.sendNotification(
                app,
                "Nhắc nhở học tập: ${lesson.title}",
                "Đồng chí đang còn ${incomplete.size} bài học chính trị chưa hoàn thành. Hãy vào học để cập nhật tiến độ huấn luyện!"
            )
        } else {
            NotificationHelper.sendNotification(
                app,
                "Vùng 4 Hải Quân - Thông báo",
                "Đồng chí đã hoàn thành đầy đủ các bài học chính trị được phân công. Chúc mừng đồng chí!"
            )
        }
    }

    fun updateCombinedNotifications() {
        val adminList = _firestoreNotifications.value.ifEmpty {
            listOf(
                NotificationItem(
                    id = "admin_notif_1",
                    title = "Kế hoạch giáo dục chính trị Vùng 4 Hải Quân năm 2026",
                    message = "Yêu cầu 100% cán bộ, chiến sĩ và học viên hoàn thành các chuyên đề học tập chính trị trước đợt kiểm tra đánh giá định kỳ.",
                    type = "admin",
                    priority = "urgent",
                    timestamp = System.currentTimeMillis() - 3600000L * 2
                ),
                NotificationItem(
                    id = "admin_notif_2",
                    title = "Cập nhật bài giảng đa phương tiện & chuyên đề số",
                    message = "Ban Tuyên huấn Vùng 4 đã phát hành thêm các video tư liệu và slide bài giảng chuyên đề trên hệ thống quản trị trực tuyến.",
                    type = "admin",
                    priority = "normal",
                    timestamp = System.currentTimeMillis() - 3600000L * 24
                )
            )
        }

        val allLessons = _lessons.value
        val completedLessonIds = _progressList.value.filter { it.completed }.map { it.lessonId }.toSet()
        val incompleteLessons = allLessons.filter { !completedLessonIds.contains(it.id) }

        val reminderList = mutableListOf<NotificationItem>()
        if (incompleteLessons.isNotEmpty()) {
            incompleteLessons.forEach { lesson ->
                reminderList.add(
                    NotificationItem(
                        id = "reminder_${lesson.id}",
                        title = "Thiếu tiến độ: ${lesson.title}",
                        message = "Đồng chí chưa hoàn thành bài học này. Nhấn vào đây để tiếp tục học và ghi nhận kết quả.",
                        type = "reminder",
                        targetLessonId = lesson.id,
                        targetCourseId = lesson.courseId,
                        priority = "high",
                        timestamp = System.currentTimeMillis() - 3600000L * 4
                    )
                )
            }
        } else if (allLessons.isNotEmpty()) {
            reminderList.add(
                NotificationItem(
                    id = "reminder_all_done",
                    title = "Xuất sắc: Đã hoàn thành tất cả chuyên đề",
                    message = "Đồng chí đã hoàn thành 100% nội dung học tập chính trị. Hãy duy trì tinh thần tự học!",
                    type = "reminder",
                    priority = "normal",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        val readIds = getReadNotificationIds()
        val combined = (reminderList + adminList).map { notif ->
            if (readIds.contains(notif.id)) notif.copy(isRead = true) else notif
        }.sortedWith(compareByDescending<NotificationItem> { it.priority == "urgent" }
            .thenByDescending { !it.isRead }
            .thenByDescending { it.timestamp })

        _notifications.value = combined
        _unreadCount.value = combined.count { !it.isRead }
    }

    fun addOrUpdateBanner(banner: BannerItem, onResult: (Boolean, String) -> Unit) {
        if (db == null) {
            onResult(false, "Chưa kết nối cơ sở dữ liệu")
            return
        }
        val currentCount = _banners.value.filter { it.id != banner.id }.size
        if (currentCount >= 5) {
            onResult(false, "Đã đạt số lượng tối đa 5 poster/banner. Vui lòng xóa bớt trước khi thêm mới!")
            return
        }
        viewModelScope.launch {
            try {
                val bannerId = banner.id.ifBlank { "banner_${System.currentTimeMillis()}" }
                val data = hashMapOf(
                    "title" to banner.title,
                    "subtitle" to banner.subtitle,
                    "imageUrl" to banner.imageUrl,
                    "linkUrl" to banner.linkUrl,
                    "targetLessonId" to (banner.targetLessonId ?: ""),
                    "order" to banner.order,
                    "active" to banner.active,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("banners").document(bannerId).set(data).await()
                onResult(true, "Đã lưu banner lên hệ thống quản trị thành công!")
            } catch (e: Exception) {
                Log.e(TAG, "[BANNER SAVE ERROR] ${e.localizedMessage}", e)
                onResult(false, "Lỗi khi lưu banner: ${e.localizedMessage}")
            }
        }
    }

    fun deleteBanner(bannerId: String, onResult: (Boolean, String) -> Unit) {
        if (db == null) {
            onResult(false, "Chưa kết nối cơ sở dữ liệu")
            return
        }
        viewModelScope.launch {
            try {
                db.collection("banners").document(bannerId).delete().await()
                onResult(true, "Đã xóa banner thành công!")
            } catch (e: Exception) {
                Log.e(TAG, "[BANNER DELETE ERROR] ${e.localizedMessage}", e)
                onResult(false, "Lỗi khi xóa banner: ${e.localizedMessage}")
            }
        }
    }

    fun seedDefaultBanners(onResult: (Boolean, String) -> Unit) {
        if (db == null) {
            onResult(false, "Chưa kết nối cơ sở dữ liệu")
            return
        }
        viewModelScope.launch {
            try {
                val defaults = BannerItem.getDefaultMilitaryBanners()
                defaults.forEach { item ->
                    val data = hashMapOf(
                        "title" to item.title,
                        "subtitle" to item.subtitle,
                        "imageUrl" to item.imageUrl,
                        "order" to item.order,
                        "active" to true,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("banners").document(item.id).set(data).await()
                }
                onResult(true, "Đã khởi tạo 5 banner mẫu Vùng 4 lên cơ sở dữ liệu quản trị!")
            } catch (e: Exception) {
                Log.e(TAG, "[BANNER SEED ERROR] ${e.localizedMessage}", e)
                onResult(false, "Lỗi khi khởi tạo banner: ${e.localizedMessage}")
            }
        }
    }

    fun submitExamResult(
        score: Int,
        totalQuestions: Int,
        timeSpentSeconds: Int,
        examId: String = "",
        examName: String = "Đề thi kiểm tra 20 câu ngẫu nhiên"
    ) {
        viewModelScope.launch {
            val percent = if (totalQuestions > 0) (score * 100 / totalQuestions) else 0
            val passed = percent >= 50
            val user = _userDoc.value
            val currentFbUser = _currentUser.value
            val uid = user?.id ?: currentFbUser?.uid ?: "guest"
            val userName = user?.name?.ifEmpty { currentFbUser?.displayName ?: currentFbUser?.email ?: "Chiến sĩ Vùng 4" } ?: "Chiến sĩ Vùng 4"
            val userEmail = user?.email?.ifEmpty { currentFbUser?.email ?: "" } ?: ""
            val userUnit = user?.unit?.ifEmpty { "Vùng 4 Hải Quân" } ?: "Vùng 4 Hải Quân"
            val userRank = user?.rank ?: ""
            val timestamp = System.currentTimeMillis()

            val newResultDoc = ExamResultDoc(
                id = "res_${timestamp}",
                userId = uid,
                userName = userName,
                userEmail = userEmail,
                userUnit = userUnit,
                userRank = userRank,
                examId = examId,
                examName = examName,
                score = score,
                totalQuestions = totalQuestions,
                scorePercentage = percent,
                passed = passed,
                timeSpentSeconds = timeSpentSeconds,
                timestamp = timestamp
            )

            // Update local state immediately for instant feedback
            val currentList = _userExamResults.value.toMutableList()
            currentList.add(0, newResultDoc)
            _userExamResults.value = currentList

            val logData = hashMapOf<String, Any>(
                "userId" to uid,
                "user_id" to uid,
                "nguoiDungId" to uid,
                "userName" to userName,
                "hoTen" to userName,
                "userEmail" to userEmail,
                "email" to userEmail,
                "userUnit" to userUnit,
                "unit" to userUnit,
                "donVi" to userUnit,
                "userRank" to userRank,
                "rank" to userRank,
                "capBac" to userRank,
                "examId" to examId,
                "dotThiId" to examId,
                "examSessionId" to examId,
                "examName" to examName,
                "tenDotThi" to examName,
                "tenBaiThi" to examName,
                "score" to score,
                "diem" to score,
                "soCauDung" to score,
                "totalQuestions" to totalQuestions,
                "tongSoCau" to totalQuestions,
                "soCauHoi" to totalQuestions,
                "scorePercentage" to percent,
                "phanTramDiem" to percent,
                "passed" to passed,
                "dat" to passed,
                "timeSpentSeconds" to timeSpentSeconds,
                "thoiGianLamBai" to timeSpentSeconds,
                "timestamp" to timestamp,
                "createdAt" to timestamp,
                "thoiGianNop" to timestamp,
                "type" to "exam_quiz",
                "source" to "mobile_app",
                "device" to "Android App Vùng 4"
            )
            
            if (db != null) {
                try {
                    db.collection("exam_results").add(logData).await()
                    db.collection("ket_qua_thi").add(logData).await()
                    db.collection("study_logs").add(logData).await()
                    
                    if (uid.isNotBlank() && uid != "guest") {
                        // 1. Gửi kết quả về dữ liệu riêng trực tiếp của tài khoản ngay lập tức
                        val docId = "res_${timestamp}"
                        db.collection("users").document(uid).collection("exam_results").document(docId).set(logData).await()
                        db.collection("users").document(uid).collection("ket_qua_thi").document(docId).set(logData).await()
                        
                        // Cũng thử lưu vào accounts nếu tài khoản nằm ở collection accounts
                        try {
                            db.collection("accounts").document(uid).collection("exam_results").document(docId).set(logData).await()
                            db.collection("accounts").document(uid).collection("ket_qua_thi").document(docId).set(logData).await()
                        } catch (ignored: Exception) {}

                        // 2. Cập nhật các thông số tổng hợp kiểm tra trực tiếp vào dữ liệu tài khoản
                        try {
                            val userRef = db.collection("users").document(uid)
                            db.runTransaction { transaction ->
                                val snapshot = transaction.get(userRef)
                                if (snapshot.exists()) {
                                    val currentTotal = snapshot.getLong("totalExamsCount") ?: 0L
                                    val currentPassed = snapshot.getLong("passedExamsCount") ?: 0L
                                    
                                    val updates = hashMapOf<String, Any>(
                                        "lastExamScore" to score,
                                        "lastExamTotal" to totalQuestions,
                                        "lastExamPercent" to percent,
                                        "lastExamPassed" to passed,
                                        "lastExamTime" to timestamp,
                                        "totalExamsCount" to (currentTotal + 1),
                                        "passedExamsCount" to if (passed) (currentPassed + 1) else currentPassed
                                    )
                                    transaction.update(userRef, updates)
                                }
                            }.await()
                        } catch (userDocEx: Exception) {
                            Log.w(TAG, "[EXAM USER DOC UPDATE ERROR] ${userDocEx.localizedMessage}")
                        }

                        try {
                            val accRef = db.collection("accounts").document(uid)
                            db.runTransaction { transaction ->
                                val snapshot = transaction.get(accRef)
                                if (snapshot.exists()) {
                                    val currentTotal = snapshot.getLong("totalExamsCount") ?: 0L
                                    val currentPassed = snapshot.getLong("passedExamsCount") ?: 0L
                                    
                                    val updates = hashMapOf<String, Any>(
                                        "lastExamScore" to score,
                                        "lastExamTotal" to totalQuestions,
                                        "lastExamPercent" to percent,
                                        "lastExamPassed" to passed,
                                        "lastExamTime" to timestamp,
                                        "totalExamsCount" to (currentTotal + 1),
                                        "passedExamsCount" to if (passed) (currentPassed + 1) else currentPassed
                                    )
                                    transaction.update(accRef, updates)
                                }
                            }.await()
                        } catch (ignored: Exception) {}
                    }
                    
                    Log.i(TAG, "[EXAM] Successfully synced exam result to Web Admin for $userName: $score/$totalQuestions ($percent%)")
                } catch (e: Exception) {
                    Log.e(TAG, "[EXAM SAVE ERROR] ${e.localizedMessage}", e)
                }
            }
        }
    }

    fun sendExamFeedback(
        examId: String,
        examName: String,
        feedbackContent: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = _userDoc.value
                val currentFbUser = _currentUser.value
                val isAuth = currentFbUser != null || (user != null && !user.id.isNullOrBlank())
                if (!isAuth) {
                    onError("Bạn cần đăng nhập để gửi phản ánh.")
                    return@launch
                }

                val uid = user?.id ?: currentFbUser?.uid ?: "guest"
                val userName = user?.name?.ifEmpty { currentFbUser?.displayName ?: "Học viên Vùng 4" } ?: "Học viên Vùng 4"
                val userEmail = user?.email?.ifEmpty { currentFbUser?.email ?: "" } ?: ""
                val timestamp = System.currentTimeMillis()

                val data = hashMapOf<String, Any>(
                    "userId" to uid,
                    "userName" to userName,
                    "userEmail" to userEmail,
                    "examId" to examId,
                    "examName" to examName,
                    "feedback" to feedbackContent,
                    "content" to feedbackContent,
                    "timestamp" to timestamp,
                    "createdAt" to timestamp,
                    "status" to "pending",
                    "type" to "exam_feedback"
                )

                if (db != null) {
                    db.collection("exam_feedbacks").add(data).await()
                    db.collection("feedbacks").add(data).await()
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Lỗi kết nối máy chủ")
            }
        }
    }

    fun sendUserFeedback(
        title: String,
        feedbackContent: String,
        feedbackType: String = "Góp ý chung",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = _userDoc.value
                val currentFbUser = _currentUser.value
                val isAuth = currentFbUser != null || (user != null && !user.id.isNullOrBlank())
                if (!isAuth) {
                    onError("Bạn cần đăng nhập để thực hiện gửi phản hồi và góp ý.")
                    return@launch
                }

                val uid = user?.id ?: currentFbUser?.uid ?: "guest"
                val userName = user?.name?.ifEmpty { currentFbUser?.displayName ?: "Học viên Vùng 4" } ?: "Học viên Vùng 4"
                val userEmail = user?.email?.ifEmpty { currentFbUser?.email ?: "" } ?: ""
                val userUnit = user?.unit?.ifEmpty { "Vùng 4 Hải Quân" } ?: "Vùng 4 Hải Quân"
                val userRank = user?.rank ?: ""
                val timestamp = System.currentTimeMillis()

                val data = hashMapOf<String, Any>(
                    "userId" to uid,
                    "userName" to userName,
                    "userEmail" to userEmail,
                    "unit" to userUnit,
                    "rank" to userRank,
                    "title" to title.ifBlank { "Ý kiến đóng góp từ học viên" },
                    "feedback" to feedbackContent,
                    "content" to feedbackContent,
                    "type" to feedbackType,
                    "timestamp" to timestamp,
                    "createdAt" to timestamp,
                    "status" to "pending"
                )

                if (db != null) {
                    db.collection("feedbacks").add(data).await()
                    db.collection("user_feedbacks").add(data).await()
                    db.collection("gop_y").add(data).await()
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Lỗi kết nối máy chủ")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopUserAccountRealtimeMonitoring()
        coursesListener?.remove()
        lessonsListener?.remove()
        contentsListener?.remove()
        slidesListener?.remove()
        videosListener?.remove()
        audiosListener?.remove()
        storageFilesListener?.remove()
        progressListener?.remove()
        notificationsListener?.remove()
        bannersListener?.remove()
        postersListener?.remove()
        questionsListener?.remove()
        examSessionsListener?.remove()
        examResultsListener?.remove()
        broadcastsListener?.remove()
    }
}
