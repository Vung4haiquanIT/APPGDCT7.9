package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import java.util.Locale
import java.util.TimeZone

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Thiết lập múi giờ Việt Nam (GMT+7) và ngôn ngữ chuẩn toàn ứng dụng
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
            Locale.setDefault(Locale.forLanguageTag("vi-VN"))
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Initialization exception fallback
        }
    }
}
