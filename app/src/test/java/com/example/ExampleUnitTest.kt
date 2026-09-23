package com.example

import com.example.model.NotificationItem
import com.example.util.TimeUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun notificationItem_defaultValues_areCorrect() {
    val item = NotificationItem(
      id = "test_1",
      title = "Thông báo kiểm tra",
      message = "Nội dung bài học",
      type = "reminder",
      targetLessonId = "lesson_123"
    )
    assertEquals("test_1", item.id)
    assertEquals("reminder", item.type)
    assertEquals("lesson_123", item.targetLessonId)
    assertFalse(item.isRead)
  }

  @Test
  fun timeUtils_vietnamTimeZone_isCorrect() {
    // 1. Chuỗi ISO UTC từ Web Quản trị (08:36 UTC tương ứng 15:36 Việt Nam)
    val parsedUtc1 = TimeUtils.parseTime("2026-09-19T08:36:00.000Z")
    val formatted1 = TimeUtils.formatDateTime(parsedUtc1)
    assertEquals("19/09/2026 15:36", formatted1)

    // 2. Chuỗi ISO UTC 08:33 tương ứng 15:33 Việt Nam
    val parsedUtc2 = TimeUtils.parseTime("2026-09-19T08:33:00Z")
    val formatted2 = TimeUtils.formatDateTime(parsedUtc2)
    assertEquals("19/09/2026 15:33", formatted2)

    // 3. Chuỗi thời gian định dạng Việt Nam từ Web Quản trị
    val parsedVn = TimeUtils.parseTime("15:35:20 19/9/2026")
    val formattedVn = TimeUtils.formatDateTime(parsedVn)
    assertEquals("19/09/2026 15:35", formattedVn)
  }
}


