package com.example

import com.example.model.NotificationItem
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
}

