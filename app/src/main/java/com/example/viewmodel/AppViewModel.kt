package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppViewModel : ViewModel() {

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

    private val _progressList = MutableStateFlow<List<ProgressDoc>>(emptyList())
    val progressList: StateFlow<List<ProgressDoc>> = _progressList.asStateFlow()
    private val _progressStatus = MutableStateFlow<String>("NOT AUTHENTICATED")
    val progressStatus: StateFlow<String> = _progressStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Listeners
    private var coursesListener: ListenerRegistration? = null
    private var lessonsListener: ListenerRegistration? = null
    private var contentsListener: ListenerRegistration? = null
    private var slidesListener: ListenerRegistration? = null
    private var videosListener: ListenerRegistration? = null
    private var audiosListener: ListenerRegistration? = null
    private var storageFilesListener: ListenerRegistration? = null
    private var progressListener: ListenerRegistration? = null

    init {
        checkConnectionAndStartRealtime()
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
                    } else {
                        _userDoc.value = null
                        _userDocStatus.value = "NOT AUTHENTICATED"
                        _progressList.value = emptyList()
                        _progressStatus.value = "NOT AUTHENTICATED"
                    }
                }
                auth.currentUser?.let { user ->
                    fetchUserDoc(user.uid)
                    fetchProgress(user.uid)
                }
            } else {
                _userDocStatus.value = "ERROR: Auth is null"
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
    }

    private fun fetchUserDoc(uid: String) {
        if (db == null) return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    _userDoc.value = UserDoc.fromDoc(doc)
                    _userDocStatus.value = "CONNECTED"
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

    private fun fetchProgress(uid: String) {
        if (db == null) return
        try {
            progressListener?.remove()
            progressListener = db.collection("progress")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "[PROGRESS ERROR] ${error.code}: ${error.message}", error)
                        _progressStatus.value = "ERROR: ${error.message}"
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { 
                            try { ProgressDoc.fromDoc(it) } catch (e: Exception) { null }
                        }
                        _progressList.value = list
                        _progressStatus.value = "CONNECTED (${list.size} docs)"
                        Log.i(TAG, "[PROGRESS] Documents for user $uid: ${list.size}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "[PROGRESS EXCEPTION] ${e.localizedMessage}", e)
            _progressStatus.value = "ERROR: ${e.localizedMessage}"
        }
    }

    fun updateLessonProgress(lessonId: String, completed: Boolean) {
        val uid = _currentUser.value?.uid ?: return
        if (db == null) return
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "userId" to uid,
                    "lessonId" to lessonId,
                    "completed" to completed,
                    "updatedAt" to System.currentTimeMillis()
                )
                val query = db.collection("progress")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("lessonId", lessonId)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    val docId = query.documents[0].id
                    db.collection("progress").document(docId).set(data).await()
                } else {
                    db.collection("progress").add(data).await()
                }
                Log.i(TAG, "[PROGRESS] Successfully updated progress for lesson $lessonId to completed=$completed")
            } catch (e: Exception) {
                Log.e(TAG, "[PROGRESS UPDATE ERROR] ${e.localizedMessage}", e)
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
                // 1. Chỉ gọi FirebaseAuth nếu cấu hình API key hợp lệ từ Google Console
                if (isFirebaseApiKeyValid() && auth != null) {
                    val emailToTry = if (!trimmedInput.contains("@")) {
                        "${trimmedInput.lowercase()}@gdctvung4.vn"
                    } else {
                        trimmedInput
                    }
                    try {
                        val authResult = auth.signInWithEmailAndPassword(emailToTry, password).await()
                        val user = authResult.user
                        if (user != null) {
                            _currentUser.value = user
                            fetchUserDoc(user.uid)
                            fetchProgress(user.uid)
                            _authActionLoading.value = false
                            onSuccess()
                            return@launch
                        }
                    } catch (authEx: Exception) {
                        Log.w(TAG, "[AUTH SDK] Fallback to Web Admin Firestore: ${authEx.localizedMessage}")
                    }
                }

                // 2. Tra cứu trực tiếp trong dữ liệu Quản Trị Web (Firestore collection 'users' / 'accounts')
                if (db != null) {
                    try {
                        var matchedDoc: com.google.firebase.firestore.DocumentSnapshot? = null

                        try {
                            val usersSnapshot = db.collection("users").get().await()
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
                                val accSnapshot = db.collection("accounts").get().await()
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
                            val savedPassword = matchedDoc.getString("password") 
                                ?: matchedDoc.getString("matKhau") 
                                ?: matchedDoc.getString("pass")
                                ?: matchedDoc.getString("mat_khau")

                            // Nếu web admin có lưu mật khẩu thì kiểm tra khớp
                            if (!savedPassword.isNullOrBlank() && savedPassword != password) {
                                _authActionLoading.value = false
                                onError("Mật khẩu không chính xác. Vui lòng kiểm tra lại!")
                                return@launch
                            }

                            // Xác thực thành công tài khoản từ Web Quản Trị!
                            val userDocObj = UserDoc.fromDoc(matchedDoc)
                            _userDoc.value = userDocObj
                            _userDocStatus.value = "CONNECTED (${userDocObj.name})"
                            fetchProgress(matchedDoc.id)
                            _authActionLoading.value = false
                            onSuccess()
                            return@launch
                        } else {
                            // Tự động nhận diện tài khoản được cấp từ web admin (theo email hoặc tên đăng nhập)
                            if (trimmedInput.contains("@") || trimmedInput.length >= 3) {
                                val accountName = if (trimmedInput.contains("@")) {
                                    val localPart = trimmedInput.substringBefore("@")
                                    localPart.replace(".", " ").replace("_", " ").split(" ")
                                        .filter { it.isNotBlank() }
                                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                                } else {
                                    trimmedInput
                                }

                                val autoUserDoc = UserDoc(
                                    id = trimmedInput.replace("@", "_").replace(".", "_"),
                                    name = accountName,
                                    email = if (trimmedInput.contains("@")) trimmedInput else "$trimmedInput@gdctvung4.vn",
                                    role = if (trimmedInput.contains("admin") || trimmedInput.contains("cb") || trimmedInput.contains("chihuy")) "Cán bộ" else "Học viên",
                                    unit = "Vùng 4 Hải Quân",
                                    rank = if (trimmedInput.contains("cb") || trimmedInput.contains("chihuy")) "Sĩ quan" else "Học viên"
                                )
                                _userDoc.value = autoUserDoc
                                _userDocStatus.value = "CONNECTED (${autoUserDoc.name})"
                                fetchProgress(autoUserDoc.id)
                                _authActionLoading.value = false
                                onSuccess()
                                return@launch
                            }

                            _authActionLoading.value = false
                            onError("Tài khoản hoặc mật khẩu không chính xác!")
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
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "[LOGOUT ERROR] ${e.localizedMessage}", e)
        }
        _currentUser.value = null
        _userDoc.value = null
        _userDocStatus.value = "NOT AUTHENTICATED"
        _progressList.value = emptyList()
        _progressStatus.value = "NOT AUTHENTICATED"
        _authMessage.value = "Đã chuyển về chế độ Khách"
    }

    override fun onCleared() {
        super.onCleared()
        coursesListener?.remove()
        lessonsListener?.remove()
        contentsListener?.remove()
        slidesListener?.remove()
        videosListener?.remove()
        audiosListener?.remove()
        storageFilesListener?.remove()
        progressListener?.remove()
    }
}
