package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.TrongDongBackground
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object TrangChu : Screen("trang_chu", "Trang chủ", Icons.Default.Home)
    object HocTap : Screen("hoc_tap", "Học tập", Icons.Default.MenuBook)
    object KiemTra : Screen("kiem_tra", "Kiểm tra", Icons.Default.Quiz)
    object CaNhan : Screen("ca_nhan", "Cá nhân", Icons.Default.Person)
    object ThongBao : Screen("thong_bao", "Thông báo", Icons.Default.Notifications)
    object Debug : Screen("debug", "Kiểm tra kết nối", Icons.Default.BugReport)
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val backStack = remember { mutableStateListOf(Screen.TrangChu.route) }
    val currentRoute = backStack.lastOrNull() ?: Screen.TrangChu.route
    var selectedCategoryForHocTap by remember { mutableStateOf<String?>(null) }
    var isExamTaking by remember { mutableStateOf(false) }
    val isViewingLesson by viewModel.isViewingLesson.collectAsState()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    fun navigateTo(route: String) {
        if (route == Screen.TrangChu.route) {
            backStack.clear()
            backStack.add(Screen.TrangChu.route)
        } else {
            if (backStack.lastOrNull() != route) {
                backStack.remove(route)
                backStack.add(route)
            }
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    // Xử lý nút Back của hệ thống trên thanh điều hướng điện thoại
    BackHandler(enabled = !isExamTaking && !isViewingLesson) {
        if (backStack.size > 1) {
            navigateBack()
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Nhấn trở lại lần nữa để thoát", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val bottomNavItems = listOf(
        Screen.TrangChu,
        Screen.HocTap,
        Screen.KiemTra,
        Screen.CaNhan
    )

    TrongDongBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (currentRoute != Screen.Debug.route && currentRoute != Screen.ThongBao.route && !isExamTaking && !isViewingLesson) {
                    NavigationBar(
                        containerColor = Color.White.copy(alpha = 0.98f),
                        contentColor = RedPrimary,
                        tonalElevation = 3.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (screen == Screen.HocTap && currentRoute != Screen.HocTap.route) {
                                        selectedCategoryForHocTap = null // reset filter when clicked from nav
                                    }
                                    navigateTo(screen.route)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RedPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTextColor = RedPrimary
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentRoute) {
                    Screen.TrangChu.route -> TrangChuScreen(
                        viewModel = viewModel,
                        onNavigateToHocTap = {
                            selectedCategoryForHocTap = null
                            navigateTo(Screen.HocTap.route)
                        },
                        onNavigateToThongBao = { navigateTo(Screen.ThongBao.route) },
                        onNavigateToDebug = { navigateTo(Screen.Debug.route) },
                        onCategoryClick = { category ->
                            if (category.equals("kiem_tra", ignoreCase = true) || category.equals("kiemtra", ignoreCase = true)) {
                                navigateTo(Screen.KiemTra.route)
                            } else {
                                selectedCategoryForHocTap = category
                                navigateTo(Screen.HocTap.route)
                            }
                        }
                    )
                    Screen.HocTap.route -> HocTapScreen(
                        viewModel = viewModel,
                        initialCategory = selectedCategoryForHocTap,
                        onBack = { navigateBack() }
                    )
                    Screen.KiemTra.route -> KiemTraScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onExamTakingStateChange = { isTaking ->
                            isExamTaking = isTaking
                        }
                    )
                    Screen.CaNhan.route -> CaNhanScreen(
                        viewModel = viewModel,
                        onNavigateToThongBao = { navigateTo(Screen.ThongBao.route) },
                        onNavigateToDebug = { navigateTo(Screen.Debug.route) }
                    )
                    Screen.ThongBao.route -> ThongBaoScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToHocTap = { navigateTo(Screen.HocTap.route) }
                    )
                    Screen.Debug.route -> FirebaseDebugScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() }
                    )
                }
            }
        }
    }
}
