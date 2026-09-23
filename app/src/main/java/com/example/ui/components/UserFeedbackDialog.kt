package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.RedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFeedbackDialog(
    userName: String,
    userUnit: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, type: String, images: List<Uri>, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
    val feedbackTypes = remember {
        listOf(
            "Góp ý chung",
            "Nội dung bài học",
            "Đề thi & Câu hỏi",
            "Báo lỗi kỹ thuật",
            "Ý kiến khác"
        )
    }

    var selectedType by remember { mutableStateOf(feedbackTypes[0]) }
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    val attachedImageUris = remember { mutableStateListOf<Uri>() }
    val maxImages = 5

    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                if (attachedImageUris.size < maxImages && !attachedImageUris.contains(uri)) {
                    attachedImageUris.add(uri)
                }
            }
            statusMessage = null
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RedPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "GÓP Ý & PHẢN HỒI Ý KIẾN",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = RedPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Người gửi
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = NavySecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Người gửi: $userName",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (userUnit.isNotBlank()) {
                                Text(
                                    text = "Đơn vị: $userUnit",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Chọn chủ đề
                Text(
                    text = "Chủ đề góp ý:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(feedbackTypes) { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            label = { Text(type, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RedPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = RedPrimary
                            )
                        )
                    }
                }

                // Tiêu đề góp ý
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { 
                        titleText = it
                        statusMessage = null
                    },
                    label = { Text("Tiêu đề góp ý (Tùy chọn)", fontSize = 13.sp) },
                    placeholder = { Text("Ví dụ: Đề xuất bổ sung thêm tài liệu...", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Nội dung góp ý
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { 
                        contentText = it
                        statusMessage = null
                    },
                    label = { Text("Nội dung chi tiết *", fontSize = 13.sp) },
                    placeholder = { Text("Nhập nội dung phản ánh, góp ý hoặc kiến nghị của bạn gửi tới Web Quản trị...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    enabled = !isSubmitting,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp)
                )

                // PHẦN ĐÍNH KÈM HÌNH ẢNH
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Hình ảnh đính kèm (Tùy chọn):",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (attachedImageUris.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RedPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${attachedImageUris.size}/$maxImages ảnh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Danh sách ảnh đã chọn
                    if (attachedImageUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachedImageUris) { uri ->
                                Box(
                                    modifier = Modifier.size(76.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        shadowElevation = 2.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .align(Alignment.BottomStart)
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Ảnh đính kèm",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Nút xóa ảnh
                                    if (!isSubmitting) {
                                        Surface(
                                            shape = CircleShape,
                                            color = RedPrimary,
                                            shadowElevation = 3.dp,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .clickable { attachedImageUris.remove(uri) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Xóa ảnh",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Nút thêm ảnh nhỏ trong danh sách nếu chưa đạt giới hạn
                            if (attachedImageUris.size < maxImages && !isSubmitting) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clickable {
                                                imagePickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "Thêm ảnh",
                                                tint = RedPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Thêm ảnh",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = RedPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Nút chọn ảnh khi chưa có ảnh nào
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RedPrimary
                            ),
                            border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chọn hình ảnh đính kèm (Tối đa 5 ảnh)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Thông báo trạng thái
                if (statusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isError) MaterialTheme.colorScheme.errorContainer 
                                else Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusMessage!!,
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (contentText.isBlank()) {
                        statusMessage = "Vui lòng nhập nội dung chi tiết trước khi gửi!"
                        isError = true
                        return@Button
                    }
                    isSubmitting = true
                    statusMessage = null
                    onSubmit(
                        titleText.ifBlank { selectedType },
                        contentText,
                        selectedType,
                        attachedImageUris.toList()
                    ) { success, error ->
                        isSubmitting = false
                        if (success) {
                            isError = false
                            statusMessage = "Đã gửi ý kiến và hình ảnh đính kèm thành công về Web Quản trị!"
                            contentText = ""
                            titleText = ""
                            attachedImageUris.clear()
                            onDismiss()
                        } else {
                            isError = true
                            statusMessage = "Lỗi gửi phản hồi: ${error ?: "Lỗi kết nối"}"
                        }
                    }
                },
                enabled = !isSubmitting && contentText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    val countStr = if (attachedImageUris.isNotEmpty()) " (${attachedImageUris.size} ẢNH)" else ""
                    Text("GỬI Ý KIẾN$countStr", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("ĐÓNG")
            }
        }
    )
}
