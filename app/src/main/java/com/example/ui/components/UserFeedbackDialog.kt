package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavySecondary
import com.example.ui.theme.RedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFeedbackDialog(
    userName: String,
    userUnit: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, type: String, onComplete: (Boolean, String?) -> Unit) -> Unit
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
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

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
                        .height(130.dp),
                    enabled = !isSubmitting,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp)
                )

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
                        selectedType
                    ) { success, error ->
                        isSubmitting = false
                        if (success) {
                            isError = false
                            statusMessage = "Đã gửi ý kiến thành công về Web Quản trị!"
                            contentText = ""
                            titleText = ""
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
                    Text("GỬI Ý KIẾN", fontWeight = FontWeight.Bold)
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
