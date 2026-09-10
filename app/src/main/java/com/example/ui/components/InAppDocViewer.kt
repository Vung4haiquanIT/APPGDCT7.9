package com.example.ui.components

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "InAppDocViewer"

/**
 * Trả về tên file chuẩn hóa và phần mở rộng tương ứng (.docx hoặc .pdf)
 */
fun getStandardFileName(title: String, format: String, urlString: String): String {
    val ext = when {
        format.lowercase().contains("pdf") || urlString.contains(".pdf", ignoreCase = true) -> "pdf"
        format.lowercase().contains("docx") || urlString.contains(".docx", ignoreCase = true) -> "docx"
        format.lowercase().contains("doc") || urlString.contains(".doc", ignoreCase = true) -> "doc"
        else -> if (format.isNotBlank()) format.lowercase() else "docx"
    }
    var cleanTitle = title.ifBlank { "TaiLieu_HocTap" }
        .replace(Regex("[^a-zA-Z0-9_\\-\\s\u00C0-\u024F\u1EA0-\u1EF9]"), "_")
        .trim()
    if (cleanTitle.isEmpty()) cleanTitle = "TaiLieu_HocTap"
    return if (cleanTitle.endsWith(".$ext", ignoreCase = true)) cleanTitle else "$cleanTitle.$ext"
}

/**
 * Xây dựng danh sách các URL ứng viên hợp lệ để tải tệp (khắc phục triệt để lỗi 404 do mã hóa đúp)
 */
fun buildCandidateUrls(rawUrl: String): List<String> {
    val candidateUrls = mutableListOf<String>()
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return candidateUrls

    // 1. Thử chuỗi URL nguyên bản (nếu không chứa khoảng trắng thô)
    if (!trimmed.contains(" ")) {
        candidateUrls.add(trimmed)
    }

    // 2. Chỉ thay thế khoảng trắng đơn thuần bằng %20 (không đụng vào các ký tự % đã mã hóa sẵn)
    val spaceReplaced = trimmed.replace(" ", "%20")
    if (!candidateUrls.contains(spaceReplaced)) {
        candidateUrls.add(spaceReplaced)
    }

    // 3. Giải mã URLDecoder nếu tệp từng bị mã hóa kép, sau đó mã hóa lại an toàn
    try {
        val decoded = java.net.URLDecoder.decode(trimmed, "UTF-8")
        val reEncoded = decoded.replace(" ", "%20")
        if (!candidateUrls.contains(reEncoded)) {
            candidateUrls.add(reEncoded)
        }
    } catch (_: Exception) {}

    // 4. Biến thể Cloudinary fl_attachment nếu có
    val copy = candidateUrls.toList()
    for (url in copy) {
        if (url.contains("cloudinary.com") && url.contains("/raw/upload/") && !url.contains("/fl_attachment/")) {
            val flUrl = url.replace("/raw/upload/", "/raw/upload/fl_attachment/")
            if (!candidateUrls.contains(flUrl)) {
                candidateUrls.add(flUrl)
            }
        }
    }

    return candidateUrls
}

fun sanitizeUrl(rawUrl: String): String {
    return buildCandidateUrls(rawUrl).firstOrNull() ?: rawUrl
}

private const val PREFS_DOWNLOADED_FILES = "vung4_downloaded_files_prefs"
private const val KEY_DOWNLOADED_IDS = "downloaded_file_ids"
private const val KEY_DOWNLOADED_URLS = "downloaded_file_urls"

/**
 * Đánh dấu một tài liệu đã được tải về máy thành công vào SharedPreferences
 */
fun markFileAsDownloaded(context: Context, fileId: String = "", fileUrl: String = "", fileName: String = "") {
    try {
        val prefs = context.getSharedPreferences(PREFS_DOWNLOADED_FILES, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_DOWNLOADED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val urls = prefs.getStringSet(KEY_DOWNLOADED_URLS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (fileId.isNotBlank()) ids.add(fileId)
        if (fileUrl.isNotBlank()) urls.add(fileUrl)
        if (fileName.isNotBlank()) urls.add(fileName)
        prefs.edit()
            .putStringSet(KEY_DOWNLOADED_IDS, ids)
            .putStringSet(KEY_DOWNLOADED_URLS, urls)
            .apply()
    } catch (e: Exception) {
        Log.e(TAG, "Error saving download state: ${e.message}")
    }
}

/**
 * Lấy tập hợp tất cả các fileId, fileUrl và tên tệp đã được tải về máy
 */
fun getDownloadedFileKeys(context: Context): Set<String> {
    return try {
        val prefs = context.getSharedPreferences(PREFS_DOWNLOADED_FILES, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_DOWNLOADED_IDS, emptySet()) ?: emptySet()
        val urls = prefs.getStringSet(KEY_DOWNLOADED_URLS, emptySet()) ?: emptySet()
        ids + urls
    } catch (_: Exception) {
        emptySet()
    }
}

/**
 * Kiểm tra xem tệp tài liệu đã được lưu trong bộ nhớ đệm (Internal Storage) của ứng dụng chưa
 */
fun isDocumentCachedInApp(context: Context, urlString: String, fileName: String, fileFormat: String = ""): Boolean {
    return try {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) return false
        val urlHash = Math.abs(urlString.hashCode()).toString()
        val files = dir.listFiles()
        if (files != null && files.any { it.name.contains(urlHash) && it.length() > 100 }) {
            return true
        }
        val standardFileName = getStandardFileName(fileName, fileFormat, urlString)
        val ext = if (standardFileName.contains(".")) standardFileName.substringAfterLast(".") else "bin"
        val baseName = standardFileName.substringBeforeLast(".")
        val cacheFileName = "${baseName}_$urlHash.$ext"
        val file = File(dir, cacheFileName)
        file.exists() && file.length() > 100
    } catch (_: Exception) {
        false
    }
}

/**
 * Kiểm tra xem tài liệu đã tải về máy (Downloads) hoặc đã lưu trữ ngoại tuyến thành công chưa
 */
fun isDocumentSavedToDevice(
    context: Context,
    fileId: String = "",
    fileUrl: String = "",
    fileName: String = "",
    fileTitle: String = ""
): Boolean {
    // 1. Kiểm tra SharedPreferences đã lưu tệp này trước đó
    val downloadedKeys = getDownloadedFileKeys(context)
    if (fileId.isNotBlank() && downloadedKeys.contains(fileId)) return true
    if (fileUrl.isNotBlank() && downloadedKeys.contains(fileUrl)) return true
    if (fileName.isNotBlank() && downloadedKeys.contains(fileName)) return true
    if (fileTitle.isNotBlank() && downloadedKeys.contains(fileTitle)) return true

    // 2. Kiểm tra bộ nhớ đệm offline nội bộ của ứng dụng
    if (fileUrl.isNotBlank() && isDocumentCachedInApp(context, fileUrl, fileName)) {
        return true
    }

    // 3. Kiểm tra tệp trong thư mục Downloads của thiết bị
    try {
        val stdName = getStandardFileName(fileTitle.ifBlank { fileName }, "", fileUrl)
        val cleanName = getStandardFileName(fileName, "", fileUrl)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            if (File(downloadsDir, stdName).exists() && File(downloadsDir, stdName).length() > 100) return true
            if (File(downloadsDir, cleanName).exists() && File(downloadsDir, cleanName).length() > 100) return true
            if (fileName.isNotBlank() && File(downloadsDir, fileName).exists() && File(downloadsDir, fileName).length() > 100) return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} = ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(stdName, cleanName, fileName)
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                    if (size > 100) return true
                }
            }
        }
    } catch (_: Exception) {}

    return false
}

/**
 * Tải tệp tài liệu và lưu an toàn vào bộ nhớ nội bộ của ứng dụng (Internal Storage)
 */
suspend fun downloadFileToAppStorage(
    context: Context,
    urlString: String,
    fileName: String
): Pair<File?, String?> {
    return withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "documents")
            if (!dir.exists()) dir.mkdirs()

            // Dùng hash của URL để cache chính xác từng file
            val urlHash = Math.abs(urlString.hashCode()).toString()
            val ext = if (fileName.contains(".")) fileName.substringAfterLast(".") else "bin"
            val baseName = fileName.substringBeforeLast(".")
            val cacheFileName = "${baseName}_$urlHash.$ext"
            val file = File(dir, cacheFileName)

            if (file.exists() && file.length() > 100) {
                // Kiểm tra sơ bộ tính toàn vẹn (tránh cache file rỗng)
                return@withContext Pair(file, null)
            }

            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val candidateUrls = buildCandidateUrls(urlString)

            var lastErrorMsg: String? = null
            for (testUrl in candidateUrls) {
                try {
                    val request = Request.Builder()
                        .url(testUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .addHeader("Accept", "*/*")
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            FileOutputStream(file).use { fos ->
                                body.byteStream().use { input ->
                                    input.copyTo(fos)
                                }
                            }
                            if (file.exists() && file.length() > 50) {
                                markFileAsDownloaded(context, "", urlString, fileName)
                                return@withContext Pair(file, null)
                            }
                        }
                    } else {
                        val code = response.code
                        if (code == 401 || code == 403) {
                            lastErrorMsg = "Máy chủ lưu trữ yêu cầu phân quyền công khai (Mã lỗi $code: Access Denied). Bạn có thể xem tài liệu qua chế độ 'Trình xem Web' bên dưới."
                        } else if (code == 404) {
                            lastErrorMsg = "Không tìm thấy tệp tài liệu trên máy chủ (Mã lỗi 404: Not Found)."
                        } else {
                            lastErrorMsg = "Lỗi kết nối máy chủ (Mã HTTP: $code)."
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMsg = "Lỗi tải tệp: ${e.localizedMessage}"
                }
            }

            if (file.exists()) file.delete()
            Pair(null, lastErrorMsg ?: "Không thể kết nối đến máy chủ tải tệp.")
        } catch (e: Exception) {
            Log.e(TAG, "Download critical error: ${e.message}", e)
            Pair(null, "Lỗi: ${e.localizedMessage}")
        }
    }
}

/**
 * Lưu tệp vào thư mục Tải về (Downloads) của thiết bị để học viên xem lại bên ngoài
 */
fun saveToDeviceDownloads(context: Context, sourceFile: File, fileName: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val mime = when {
                fileName.endsWith(".pdf", true) -> "application/pdf"
                fileName.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "application/msword"
            }
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                markFileAsDownloaded(context, fileName = fileName)
                true
            } else false
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            markFileAsDownloaded(context, fileName = fileName)
            true
        }
    } catch (e: Exception) {
        Log.e(TAG, "Save downloads error: ${e.message}", e)
        false
    }
}

/**
 * Tải tệp trực tiếp từ Web Quản trị về máy bằng Android System DownloadManager.
 * Đảm bảo tải tệp gốc qua mạng đúng định dạng MIME và hiển thị thanh tiến trình trên hệ thống.
 */
fun downloadFileViaSystemManager(
    context: Context,
    fileUrl: String,
    fileTitle: String,
    standardFileName: String
): Boolean {
    return try {
        val candidateUrls = buildCandidateUrls(fileUrl)
        val validUrl = candidateUrls.firstOrNull() ?: fileUrl
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return false

        val ext = if (standardFileName.contains(".")) standardFileName.substringAfterLast(".").lowercase() else ""
        val mime = when {
            ext == "pdf" -> "application/pdf"
            ext == "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ext == "doc" -> "application/msword"
            ext == "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ext == "xls" -> "application/vnd.ms-excel"
            ext == "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ext == "ppt" -> "application/vnd.ms-powerpoint"
            else -> "application/octet-stream"
        }

        val request = DownloadManager.Request(Uri.parse(validUrl)).apply {
            setTitle(fileTitle.ifBlank { standardFileName })
            setDescription("Tải tài liệu học tập từ Web Quản trị")
            setMimeType(mime)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, standardFileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        downloadManager.enqueue(request)
        markFileAsDownloaded(context, fileId = fileTitle, fileUrl = validUrl, fileName = standardFileName)
        Toast.makeText(
            context,
            "Đang tải tệp '$standardFileName' từ máy chủ về thư mục Downloads...",
            Toast.LENGTH_LONG
        ).show()
        true
    } catch (e: Exception) {
        Log.e(TAG, "System DownloadManager error: ${e.message}", e)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Mở thư mục Tải về (Downloads) hoặc Trình quản lý tệp trên thiết bị
 */
fun openDownloadsFolder(context: Context, fileName: String? = null) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", downloadsDir)
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val msg = if (!fileName.isNullOrBlank()) {
                "Tệp '$fileName' đã được lưu tại thư mục Tải về (Downloads) trên máy của bạn."
            } else {
                "Tài liệu đã được lưu tại thư mục Tải về (Downloads) trên máy."
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Bộ chuyển đổi chuyên dụng DOCX -> HTML hiển thị đầy đủ định dạng (Đậm, Nghiêng, Gạch chân, Màu sắc, Bảng biểu, Tiêu đề, Hình ảnh)
 * LOẠI BỎ TRIỆT ĐỂ MỌI KÝ TỰ CODE XML, FIELD CODES (HYPERLINK, PAGE, MERGEFORMAT...)
 */
object DocxToHtmlConverter {

    fun convert(file: File): String {
        return try {
            val mediaMap = mutableMapOf<String, String>()
            val relsMap = mutableMapOf<String, String>()
            var documentXmlBytes: ByteArray? = null

            // 1. Quét tệp zip để lấy document.xml, quan hệ rels và hình ảnh media
            ZipInputStream(file.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "word/document.xml") {
                        documentXmlBytes = zis.readBytes()
                    } else if (name == "word/_rels/document.xml.rels") {
                        val relsXml = String(zis.readBytes(), Charsets.UTF_8)
                        parseRels(relsXml, relsMap)
                    } else if (name.startsWith("word/media/")) {
                        val imgBytes = zis.readBytes()
                        val mime = when {
                            name.endsWith(".png", true) -> "image/png"
                            name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
                            name.endsWith(".gif", true) -> "image/gif"
                            else -> "image/png"
                        }
                        val base64 = Base64.encodeToString(imgBytes, Base64.NO_WRAP)
                        val relativeName = name.removePrefix("word/")
                        mediaMap[relativeName] = "data:$mime;base64,$base64"
                        mediaMap[name] = "data:$mime;base64,$base64"
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (documentXmlBytes == null) {
                // Không phải docx chuẩn, thử đọc text dự phòng
                return fallbackBinaryDocToHtml(file)
            }

            // 2. Phân tích cú pháp cây DOM
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(documentXmlBytes))

            val htmlBody = StringBuilder()
            val bodyList = doc.getElementsByTagName("w:body")
            val bodyNode = if (bodyList.length > 0) bodyList.item(0) else doc.documentElement

            val children = bodyNode.childNodes
            for (i in 0 until children.length) {
                val child = children.item(i)
                when (child.nodeName) {
                    "w:p" -> parseParagraph(child, htmlBody, relsMap, mediaMap)
                    "w:tbl" -> parseTable(child, htmlBody, relsMap, mediaMap)
                }
            }

            wrapHtmlDocument(htmlBody.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Docx parsing error: ${e.message}", e)
            fallbackBinaryDocToHtml(file)
        }
    }

    private fun parseRels(xml: String, relsMap: MutableMap<String, String>) {
        val relRegex = "<Relationship[^>]*Id=\"([^\"]+)\"[^>]*Target=\"([^\"]+)\"".toRegex()
        relRegex.findAll(xml).forEach { match ->
            val id = match.groupValues[1]
            val target = match.groupValues[2]
            relsMap[id] = target
        }
    }

    private fun parseParagraph(
        pNode: Node,
        out: StringBuilder,
        relsMap: Map<String, String>,
        mediaMap: Map<String, String>
    ) {
        var headingLevel = 0
        var isTitle = false
        var textAlign = ""
        var isListItem = false

        // Kiểm tra thuộc tính đoạn văn <w:pPr>
        val children = pNode.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeName == "w:pPr") {
                val prChildren = child.childNodes
                for (j in 0 until prChildren.length) {
                    val prChild = prChildren.item(j)
                    when (prChild.nodeName) {
                        "w:pStyle" -> {
                            val styleVal = (prChild as? Element)?.getAttribute("w:val") ?: ""
                            if (styleVal.contains("Heading1", ignoreCase = true) || styleVal == "1") headingLevel = 1
                            else if (styleVal.contains("Heading2", ignoreCase = true) || styleVal == "2") headingLevel = 2
                            else if (styleVal.contains("Heading3", ignoreCase = true) || styleVal == "3") headingLevel = 3
                            else if (styleVal.contains("Title", ignoreCase = true)) isTitle = true
                        }
                        "w:jc" -> {
                            val jcVal = (prChild as? Element)?.getAttribute("w:val") ?: ""
                            textAlign = when (jcVal.lowercase()) {
                                "center" -> "center"
                                "right" -> "right"
                                "both" -> "justify"
                                else -> ""
                            }
                        }
                        "w:numPr" -> {
                            isListItem = true
                        }
                    }
                }
            }
        }

        val tag = when {
            isTitle -> "h1 class='doc-title'"
            headingLevel == 1 -> "h1"
            headingLevel == 2 -> "h2"
            headingLevel == 3 -> "h3"
            isListItem -> "li"
            else -> "p"
        }

        val styleAttr = if (textAlign.isNotEmpty()) " style='text-align: $textAlign;'" else ""
        val pContent = StringBuilder()

        // Phân tích các Run <w:r> hoặc liên kết <w:hyperlink>
        for (i in 0 until children.length) {
            val child = children.item(i)
            when (child.nodeName) {
                "w:r" -> parseRun(child, pContent, relsMap, mediaMap)
                "w:hyperlink" -> {
                    val linkContent = StringBuilder()
                    val rChildren = child.childNodes
                    for (k in 0 until rChildren.length) {
                        if (rChildren.item(k).nodeName == "w:r") {
                            parseRun(rChildren.item(k), linkContent, relsMap, mediaMap)
                        }
                    }
                    if (linkContent.isNotEmpty()) {
                        pContent.append("<span style='color: #2563eb; text-decoration: underline;'>")
                            .append(linkContent)
                            .append("</span>")
                    }
                }
            }
        }

        val contentStr = pContent.toString().trim()
        if (contentStr.isNotEmpty() || tag.startsWith("h")) {
            out.append("<$tag$styleAttr>").append(contentStr.ifEmpty { "&nbsp;" }).append("</${tag.substringBefore(" ")}>\n")
        }
    }

    private fun parseRun(
        rNode: Node,
        out: StringBuilder,
        relsMap: Map<String, String>,
        mediaMap: Map<String, String>
    ) {
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isStrike = false
        var textColor: String? = null
        var bgColor: String? = null
        var fontSizePt: Int? = null

        val children = rNode.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeName == "w:rPr") {
                val prChildren = child.childNodes
                for (j in 0 until prChildren.length) {
                    val prChild = prChildren.item(j)
                    val elem = prChild as? Element
                    when (prChild.nodeName) {
                        "w:b" -> {
                            val v = elem?.getAttribute("w:val")
                            isBold = v == null || v == "1" || v.equals("true", true)
                        }
                        "w:i" -> {
                            val v = elem?.getAttribute("w:val")
                            isItalic = v == null || v == "1" || v.equals("true", true)
                        }
                        "w:u" -> isUnderline = true
                        "w:strike" -> isStrike = true
                        "w:color" -> {
                            val c = elem?.getAttribute("w:val")
                            if (!c.isNullOrBlank() && c != "auto") textColor = "#$c"
                        }
                        "w:highlight" -> {
                            val hl = elem?.getAttribute("w:val")
                            if (!hl.isNullOrBlank() && hl != "none") {
                                bgColor = when (hl.lowercase()) {
                                    "yellow" -> "#fef08a"
                                    "green" -> "#bbf7d0"
                                    "cyan" -> "#bae6fd"
                                    "magenta" -> "#fbcfe8"
                                    else -> "#fef08a"
                                }
                            }
                        }
                        "w:sz" -> {
                            val sz = elem?.getAttribute("w:val")?.toIntOrNull()
                            if (sz != null) fontSizePt = sz / 2
                        }
                    }
                }
            }
        }

        // Lấy nội dung chữ và xử lý hình ảnh
        val runText = StringBuilder()
        for (i in 0 until children.length) {
            val child = children.item(i)
            when (child.nodeName) {
                "w:t" -> {
                    // Escape HTML characters để chữ hiển thị chính xác
                    val rawText = child.textContent ?: ""
                    runText.append(escapeHtml(rawText))
                }
                "w:br" -> runText.append("<br/>")
                "w:tab" -> runText.append("&emsp;&emsp;")
                "w:drawing" -> {
                    // Tìm kiếm hình ảnh
                    parseDrawing(child, runText, relsMap, mediaMap)
                }
                // CHẶN TRIỆT ĐỂ: Không đọc w:instrText (chứa mã code Word: HYPERLINK, PAGE, TOC, MERGEFORMAT...)
                "w:instrText", "w:fldSimple", "w:fldChar", "w:proofErr" -> {
                    // Bỏ qua hoàn toàn, không hiển thị mã code
                }
            }
        }

        if (runText.isEmpty()) return

        var styled = runText.toString()
        if (isBold) styled = "<strong>$styled</strong>"
        if (isItalic) styled = "<em>$styled</em>"
        if (isUnderline) styled = "<u>$styled</u>"
        if (isStrike) styled = "<del>$styled</del>"

        val styles = mutableListOf<String>()
        if (textColor != null) styles.add("color: $textColor")
        if (bgColor != null) styles.add("background-color: $bgColor")
        if (fontSizePt != null && fontSizePt in 9..36) styles.add("font-size: ${fontSizePt}pt")

        if (styles.isNotEmpty()) {
            styled = "<span style='${styles.joinToString("; ")}'>$styled</span>"
        }

        out.append(styled)
    }

    private fun parseDrawing(
        drawingNode: Node,
        out: StringBuilder,
        relsMap: Map<String, String>,
        mediaMap: Map<String, String>
    ) {
        val xml = nodeToString(drawingNode)
        val blipRegex = "<a:blip[^>]*r:embed=\"([^\"]+)\"".toRegex()
        val match = blipRegex.find(xml)
        if (match != null) {
            val rId = match.groupValues[1]
            val target = relsMap[rId]
            if (target != null) {
                val dataUri = mediaMap[target] ?: mediaMap["media/${target.substringAfterLast("/")}"]
                if (dataUri != null) {
                    out.append("<img src='$dataUri' alt='Hình ảnh tài liệu' class='doc-img' />")
                }
            }
        }
    }

    private fun parseTable(
        tblNode: Node,
        out: StringBuilder,
        relsMap: Map<String, String>,
        mediaMap: Map<String, String>
    ) {
        out.append("<div class='table-wrapper'><table class='doc-table'>\n")
        val trList = tblNode.childNodes
        for (i in 0 until trList.length) {
            val tr = trList.item(i)
            if (tr.nodeName == "w:tr") {
                out.append("<tr>\n")
                val tcList = tr.childNodes
                for (j in 0 until tcList.length) {
                    val tc = tcList.item(j)
                    if (tc.nodeName == "w:tc") {
                        var colSpan = 1
                        var bgColor: String? = null

                        // Check tcPr
                        val tcChildren = tc.childNodes
                        for (k in 0 until tcChildren.length) {
                            val tcChild = tcChildren.item(k)
                            if (tcChild.nodeName == "w:tcPr") {
                                val prNodes = tcChild.childNodes
                                for (m in 0 until prNodes.length) {
                                    val prNode = prNodes.item(m)
                                    if (prNode.nodeName == "w:gridSpan") {
                                        colSpan = (prNode as? Element)?.getAttribute("w:val")?.toIntOrNull() ?: 1
                                    } else if (prNode.nodeName == "w:shd") {
                                        val fill = (prNode as? Element)?.getAttribute("w:fill")
                                        if (!fill.isNullOrBlank() && fill != "auto") bgColor = "#$fill"
                                    }
                                }
                            }
                        }

                        val tdAttrs = mutableListOf<String>()
                        if (colSpan > 1) tdAttrs.add("colspan='$colSpan'")
                        if (bgColor != null) tdAttrs.add("style='background-color: $bgColor;'")

                        out.append("<td ${tdAttrs.joinToString(" ")}>\n")
                        for (k in 0 until tcChildren.length) {
                            val tcChild = tcChildren.item(k)
                            if (tcChild.nodeName == "w:p") {
                                parseParagraph(tcChild, out, relsMap, mediaMap)
                            }
                        }
                        out.append("</td>\n")
                    }
                }
                out.append("</tr>\n")
            }
        }
        out.append("</table></div>\n")
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun nodeToString(node: Node): String {
        val sw = java.io.StringWriter()
        try {
            val t = javax.xml.transform.TransformerFactory.newInstance().newTransformer()
            t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes")
            t.transform(javax.xml.transform.dom.DOMSource(node), javax.xml.transform.stream.StreamResult(sw))
        } catch (_: Exception) {}
        return sw.toString()
    }

    private fun fallbackBinaryDocToHtml(file: File): String {
        return try {
            val bytes = file.readBytes()
            val sb = StringBuilder()
            val text = String(bytes, Charsets.UTF_8)
            // Tìm các đoạn văn bản có ý nghĩa (bỏ qua byte nhị phân)
            val lines = text.split("\r\n", "\n", "\r")
                .map { it.replace(Regex("[^a-zA-Z0-9_\\-\\s\u00C0-\u024F\u1EA0-\u1EF9.,:;!?'\"()/%]"), " ").trim() }
                .filter { it.length > 5 }

            if (lines.isNotEmpty()) {
                lines.forEach { line ->
                    sb.append("<p>").append(escapeHtml(line)).append("</p>\n")
                }
            } else {
                sb.append("<p>Tài liệu đã được tải về máy thành công. Định dạng tệp Word đặc biệt, vui lòng nhấn 'Tải về máy' để xem chi tiết đầy đủ.</p>")
            }
            wrapHtmlDocument(sb.toString())
        } catch (e: Exception) {
            wrapHtmlDocument("<p>Đã lưu tài liệu vào bộ nhớ ứng dụng.</p>")
        }
    }

    private fun wrapHtmlDocument(bodyHtml: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
                <style>
                    * { box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        background-color: #f8fafc;
                        color: #1e293b;
                        margin: 0;
                        padding: 12px;
                        font-size: 15px;
                        line-height: 1.65;
                        word-break: break-word;
                    }
                    .doc-sheet {
                        background: #ffffff;
                        max-width: 820px;
                        margin: 0 auto;
                        padding: 22px 18px;
                        border-radius: 12px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
                        border: 1px solid #e2e8f0;
                    }
                    h1, .doc-title {
                        color: #b91c1c;
                        font-size: 20px;
                        font-weight: 700;
                        margin-top: 18px;
                        margin-bottom: 12px;
                        line-height: 1.35;
                        border-bottom: 2px solid #fee2e2;
                        padding-bottom: 6px;
                    }
                    h2 {
                        color: #1e3a8a;
                        font-size: 17px;
                        font-weight: 600;
                        margin-top: 16px;
                        margin-bottom: 8px;
                    }
                    h3 {
                        color: #334155;
                        font-size: 15px;
                        font-weight: 600;
                        margin-top: 12px;
                        margin-bottom: 6px;
                    }
                    p {
                        margin: 0 0 10px 0;
                        text-align: justify;
                    }
                    .table-wrapper {
                        width: 100%;
                        overflow-x: auto;
                        margin: 14px 0;
                        -webkit-overflow-scrolling: touch;
                    }
                    table.doc-table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 13.5px;
                        background: #ffffff;
                    }
                    table.doc-table th, table.doc-table td {
                        border: 1px solid #cbd5e1;
                        padding: 8px 10px;
                        text-align: left;
                        vertical-align: top;
                    }
                    table.doc-table tr:nth-child(even) td {
                        background-color: #f8fafc;
                    }
                    ul, ol {
                        margin: 6px 0 12px 20px;
                        padding: 0;
                    }
                    li {
                        margin-bottom: 6px;
                    }
                    .doc-img {
                        max-width: 100%;
                        height: auto;
                        border-radius: 8px;
                        margin: 12px auto;
                        display: block;
                        box-shadow: 0 2px 6px rgba(0,0,0,0.08);
                    }
                    strong { font-weight: 700; }
                    em { font-style: italic; }
                    u { text-decoration: underline; }
                </style>
            </head>
            <body>
                <div class="doc-sheet">
                    $bodyHtml
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}

/**
 * Render trang PDF sang Bitmap trên luồng chạy nền với nền trắng tuyệt đối
 */
suspend fun renderPdfPageBitmap(
    renderer: PdfRenderer,
    pageIndex: Int,
    scale: Float = 2.0f
): Bitmap? = withContext(Dispatchers.Default) {
    var page: PdfRenderer.Page? = null
    try {
        page = renderer.openPage(pageIndex)
        val width = (page.width * scale).toInt().coerceIn(400, 2600)
        val height = (page.height * scale).toInt().coerceIn(400, 3800)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Quan trọng: Vẽ nền trắng trước khi render để chữ không bị trong suốt / đen
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    } catch (e: Exception) {
        Log.e(TAG, "Error rendering PDF page $pageIndex: ${e.message}", e)
        null
    } finally {
        try { page?.close() } catch (_: Exception) {}
    }
}

/**
 * Dialog xem tài liệu In-App hoàn chỉnh:
 * - Tự động tải và lưu vào bộ nhớ app
 * - Đọc Word (.docx/.doc) đầy đủ định dạng qua bộ hiển thị chuẩn mực (Không dính ký tự code)
 * - Đọc PDF trực quan với lật trang, phóng to / thu nhỏ, điều hướng mượt mà
 * - Hỗ trợ chế độ Web Viewer dự phòng nếu máy chủ Cloudinary hạn chế quyền ACL
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppDocumentViewerDialog(
    fileTitle: String,
    fileUrl: String,
    fileFormat: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var localFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var wordHtmlContent by remember { mutableStateOf("") }
    var isWebViewMode by remember { mutableStateOf(false) } // Mặc định dùng chế độ đọc bộ nhớ đệm nội bộ (hoạt động khi Ngoại tuyến / Offline)

    var isWebLoading by remember { mutableStateOf(true) }
    var webProgress by remember { mutableStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val standardFileName = remember(fileTitle, fileFormat, fileUrl) {
        getStandardFileName(fileTitle, fileFormat, fileUrl)
    }

    var isSavedToDeviceInDialog by remember(fileUrl, standardFileName) {
        mutableStateOf(isDocumentSavedToDevice(context, fileTitle, fileUrl, standardFileName, fileTitle))
    }

    val isPdf = standardFileName.endsWith(".pdf", ignoreCase = true)
    val isWord = standardFileName.endsWith(".docx", ignoreCase = true) || standardFileName.endsWith(".doc", ignoreCase = true)

    // PDF state
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var parcelFileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var currentPageIndex by remember { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var currentPdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPage by remember { mutableStateOf(false) }

    // PDF Zoom & Pan state
    var pdfZoomScale by remember { mutableStateOf(1f) }
    var pdfOffsetX by remember { mutableStateOf(0f) }
    var pdfOffsetY by remember { mutableStateOf(0f) }

    fun loadPage(index: Int) {
        val renderer = pdfRenderer ?: return
        if (index !in 0 until pageCount) return
        coroutineScope.launch {
            isRenderingPage = true
            val bmp = renderPdfPageBitmap(renderer, index, 2.0f)
            if (bmp != null) {
                currentPdfBitmap = bmp
                currentPageIndex = index
                pdfZoomScale = 1f
                pdfOffsetX = 0f
                pdfOffsetY = 0f
            }
            isRenderingPage = false
        }
    }

    LaunchedEffect(fileUrl) {
        isLoading = true
        downloadError = null
        if (!isSavedToDeviceInDialog) {
            isSavedToDeviceInDialog = isDocumentSavedToDevice(context, fileTitle, fileUrl, standardFileName, fileTitle)
        }
        val (downloaded, err) = downloadFileToAppStorage(context, fileUrl, standardFileName)
        if (downloaded != null && downloaded.exists()) {
            localFile = downloaded
            isWebViewMode = false
            if (isWord) {
                withContext(Dispatchers.Default) {
                    wordHtmlContent = DocxToHtmlConverter.convert(downloaded)
                }
            } else if (isPdf) {
                try {
                    val pfd = ParcelFileDescriptor.open(downloaded, ParcelFileDescriptor.MODE_READ_ONLY)
                    parcelFileDescriptor = pfd
                    val renderer = PdfRenderer(pfd)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    if (pageCount > 0) {
                        loadPage(0)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot init PdfRenderer: ${e.message}", e)
                    downloadError = "Không thể phân giải cấu trúc PDF nội bộ. Bạn có thể bấm 'Mở chế độ Web' để xem trực tuyến."
                }
            }
        } else {
            downloadError = err ?: "Không thể kết nối tải tệp."
            // Nếu download trực tiếp gặp mã 401 hoặc lỗi, tự động chuyển sang chế độ Web Viewer dự phòng
            if (err?.contains("401") == true || err?.contains("Access Denied") == true) {
                isWebViewMode = true
            }
        }
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. THANH TIÊU ĐỀ
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Article,
                                contentDescription = null,
                                tint = if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = standardFileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = if (isWebViewMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else if (isPdf) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        contentColor = if (isWebViewMode) MaterialTheme.colorScheme.primary else if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = if (isWebViewMode) "XEM TRỰC TUYẾN" else if (isPdf) "PDF CHUẨN" else "WORD ĐỊNH DẠNG",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isWebViewMode) "Trình xem Web (Mặc định)" else "Đọc trực tiếp trong App",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    }
                }

                // 2. THANH CÔNG CỤ THAO TÁC (Tải về máy / Đổi chế độ xem / Tải lại / Zoom)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Nút Tải về máy / Đã tải (Dạng thư mục mở thư mục chứa)
                        if (isSavedToDeviceInDialog) {
                            OutlinedButton(
                                onClick = {
                                    openDownloadsFolder(context, standardFileName)
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Đã tải - Mở thư mục", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        } else if (localFile != null && localFile!!.exists()) {
                            OutlinedButton(
                                onClick = {
                                    val okSystem = downloadFileViaSystemManager(context, fileUrl, fileTitle, standardFileName)
                                    localFile?.let { file ->
                                        saveToDeviceDownloads(context, file, standardFileName)
                                    }
                                    markFileAsDownloaded(context, fileTitle, fileUrl, standardFileName)
                                    isSavedToDeviceInDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tải về máy", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    downloadFileViaSystemManager(context, fileUrl, fileTitle, standardFileName)
                                    markFileAsDownloaded(context, fileTitle, fileUrl, standardFileName)
                                    isSavedToDeviceInDialog = true
                                    coroutineScope.launch {
                                        val (downloaded, _) = downloadFileToAppStorage(context, fileUrl, standardFileName)
                                        if (downloaded != null && downloaded.exists()) {
                                            localFile = downloaded
                                            isWebViewMode = false
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tải tệp về máy", fontSize = 12.sp)
                            }
                        }

                        // Điều khiển Chế độ xem & Phóng to thu nhỏ
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isWebViewMode) {
                                IconButton(
                                    onClick = { webViewInstance?.reload() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Tải lại", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Không thể mở trình duyệt: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Mở bằng trình duyệt", modifier = Modifier.size(18.dp))
                                }
                            }

                            // Chuyển đổi giữa Chế độ đọc App và Trình xem Web
                            TextButton(
                                onClick = { isWebViewMode = !isWebViewMode },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWebViewMode) Icons.Default.MenuBook else Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isWebViewMode) "Xem trong App" else "Trình xem Web",
                                    fontSize = 11.sp
                                )
                            }

                            // Zoom controls cho PDF khi ở chế độ xem trong App
                            if (isPdf && !isWebViewMode && pageCount > 0) {
                                IconButton(
                                    onClick = {
                                        pdfZoomScale = (pdfZoomScale / 1.25f).coerceAtLeast(1f)
                                        if (pdfZoomScale == 1f) { pdfOffsetX = 0f; pdfOffsetY = 0f }
                                    },
                                    modifier = Modifier.size(32.dp),
                                    enabled = pdfZoomScale > 1f
                                ) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Thu nhỏ", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        pdfZoomScale = (pdfZoomScale * 1.25f).coerceAtMost(3.5f)
                                    },
                                    modifier = Modifier.size(32.dp),
                                    enabled = pdfZoomScale < 3.5f
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Phóng to", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // 3. KHU VỰC HIỂN THỊ NỘI DUNG TÀI LIỆU
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (isWebViewMode) {
                        // CHẾ ĐỘ MẶC ĐỊNH: Trình xem Web (Google Docs Viewer) mở ngay lập tức
                        val encodedDocUrl = remember(fileUrl) {
                            try {
                                java.net.URLEncoder.encode(fileUrl, "UTF-8")
                            } catch (_: Exception) {
                                fileUrl
                            }
                        }
                        val webViewerUrl = remember(encodedDocUrl) {
                            "https://docs.google.com/viewer?url=$encodedDocUrl&embedded=true"
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            setSupportZoom(true)
                                            cacheMode = WebSettings.LOAD_DEFAULT
                                        }
                                        webChromeClient = object : android.webkit.WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                webProgress = newProgress
                                                if (newProgress >= 90) {
                                                    isWebLoading = false
                                                }
                                            }
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                isWebLoading = true
                                            }
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                isWebLoading = false
                                            }
                                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                                isWebLoading = false
                                            }
                                        }
                                        webViewInstance = this
                                        loadUrl(webViewerUrl)
                                    }
                                },
                                update = { webView ->
                                    webViewInstance = webView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isWebLoading) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 24.dp, vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        progress = { (webProgress.coerceAtLeast(10) / 100f) },
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Đang tải tài liệu...", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (webProgress > 0) "$webProgress%" else "Đang kết nối...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (isLoading) {
                        // Khi người dùng chuyển sang chế độ Xem trong App nhưng tệp đang được tải ngầm
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Đang tải dữ liệu tệp về máy...", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Vui lòng đợi trong giây lát", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (downloadError != null && localFile == null) {
                        // HIỂN THỊ LỖI KÈM NÚT CHUYỂN WEB VIEWER
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Thông báo tải tệp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = downloadError ?: "Không thể kết nối đến tệp",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { isWebViewMode = true },
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Xem qua Trình xem Web")
                            }
                        }
                    } else if (isWord) {
                        // CHẾ ĐỘ ĐỌC WORD ĐẦY ĐỦ ĐỊNH DẠNG (HTML RENDERER IN-APP)
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        setSupportZoom(true)
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            return true // Chặn không cho nhảy ra ngoài trình duyệt ngoài
                                        }
                                    }
                                    loadDataWithBaseURL(null, wordHtmlContent, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, wordHtmlContent, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (isPdf) {
                        // CHẾ ĐỘ ĐỌC PDF TRỰC TIẾP TRONG APP (PDF RENDERER NATIVE)
                        if (pdfRenderer != null && pageCount > 0) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Khung xem trang PDF với cử chỉ phóng to & kéo
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(android.graphics.Color.DKGRAY.let { androidx.compose.ui.graphics.Color(0xFF2B2D30) })
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                pdfZoomScale = (pdfZoomScale * zoom).coerceIn(1f, 4f)
                                                if (pdfZoomScale > 1f) {
                                                    pdfOffsetX += pan.x
                                                    pdfOffsetY += pan.y
                                                } else {
                                                    pdfOffsetX = 0f
                                                    pdfOffsetY = 0f
                                                }
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    if (pdfZoomScale > 1f) {
                                                        pdfZoomScale = 1f
                                                        pdfOffsetX = 0f
                                                        pdfOffsetY = 0f
                                                    } else {
                                                        pdfZoomScale = 2.2f
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRenderingPage) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        currentPdfBitmap?.let { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Trang PDF",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer(
                                                        scaleX = pdfZoomScale,
                                                        scaleY = pdfZoomScale,
                                                        translationX = pdfOffsetX,
                                                        translationY = pdfOffsetY
                                                    )
                                                    .padding(4.dp)
                                            )
                                        } ?: CircularProgressIndicator()
                                    }
                                }

                                // Thanh điều hướng trang PDF
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { loadPage(currentPageIndex - 1) },
                                            enabled = currentPageIndex > 0,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronLeft, contentDescription = "Trang trước")
                                        }

                                        Text(
                                            text = "Trang ${currentPageIndex + 1} / $pageCount",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        FilledTonalIconButton(
                                            onClick = { loadPage(currentPageIndex + 1) },
                                            enabled = currentPageIndex < pageCount - 1,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Trang sau")
                                        }
                                    }
                                }
                            }
                        } else {
                            // Không thể mở với PdfRenderer -> Gợi ý mở Web Viewer
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Không thể kết xuất trang PDF nội bộ.", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { isWebViewMode = true }) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Xem trực tuyến qua Web")
                                }
                            }
                        }
                    } else {
                        // Tệp thông thường
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Tài liệu $standardFileName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Đã được tải về và lưu an toàn trong bộ nhớ ứng dụng.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
