package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var currentRoute by remember { mutableStateOf<String>(Screen.TrangChu.route) }
    var selectedCategoryForHocTap by remember { mutableStateOf<String?>(null) }
    var isExamTaking by remember { mutableStateOf(false) }

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
                if (currentRoute != Screen.Debug.route && currentRoute != Screen.ThongBao.route && !isExamTaking) {
                    NavigationBar(
                        containerColor = Color.White.copy(alpha = 0.96f),
                        contentColor = RedPrimary
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
                                    currentRoute = screen.route
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
        Box(modifier = Modifier.padding(padding)) {
            when (currentRoute) {
                Screen.TrangChu.route -> TrangChuScreen(
                    viewModel = viewModel,
                    onNavigateToHocTap = {
                        selectedCategoryForHocTap = null
                        currentRoute = Screen.HocTap.route
                    },
                    onNavigateToThongBao = { currentRoute = Screen.ThongBao.route },
                    onNavigateToDebug = { currentRoute = Screen.Debug.route },
                    onCategoryClick = { category ->
                        if (category.equals("kiem_tra", ignoreCase = true) || category.equals("kiemtra", ignoreCase = true)) {
                            currentRoute = Screen.KiemTra.route
                        } else {
                            selectedCategoryForHocTap = category
                            currentRoute = Screen.HocTap.route
                        }
                    }
                )
                Screen.HocTap.route -> HocTapScreen(
                    viewModel = viewModel,
                    initialCategory = selectedCategoryForHocTap,
                    onBack = { currentRoute = Screen.TrangChu.route }
                )
                Screen.KiemTra.route -> KiemTraScreen(
                    viewModel = viewModel,
                    onBack = { currentRoute = Screen.TrangChu.route },
                    onExamTakingStateChange = { isTaking ->
                        isExamTaking = isTaking
                    }
                )
                Screen.CaNhan.route -> CaNhanScreen(
                    viewModel = viewModel,
                    onNavigateToThongBao = { currentRoute = Screen.ThongBao.route },
                    onNavigateToDebug = { currentRoute = Screen.Debug.route }
                )
                Screen.ThongBao.route -> ThongBaoScreen(
                    viewModel = viewModel,
                    onBack = { currentRoute = Screen.TrangChu.route },
                    onNavigateToHocTap = { currentRoute = Screen.HocTap.route }
                )
                Screen.Debug.route -> FirebaseDebugScreen(
                    viewModel = viewModel,
                    onBack = { currentRoute = Screen.TrangChu.route }
                )
            }
        }
    }
}
}
