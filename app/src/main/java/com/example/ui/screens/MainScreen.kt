package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.RedPrimary
import com.example.viewmodel.AppViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object TrangChu : Screen("trang_chu", "Trang chủ", Icons.Default.Home)
    object HocTap : Screen("hoc_tap", "Học tập", Icons.Default.MenuBook)
    object CaNhan : Screen("ca_nhan", "Cá nhân", Icons.Default.Person)
    object ThongBao : Screen("thong_bao", "Thông báo", Icons.Default.Notifications)
    object Debug : Screen("debug", "Kiểm tra kết nối", Icons.Default.BugReport)
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    var currentRoute by remember { mutableStateOf<String>(Screen.TrangChu.route) }

    val items = listOf(
        Screen.TrangChu,
        Screen.HocTap,
        Screen.CaNhan
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Debug.route && currentRoute != Screen.ThongBao.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RedPrimary
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = { currentRoute = screen.route },
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
                    onNavigateToHocTap = { currentRoute = Screen.HocTap.route },
                    onNavigateToThongBao = { currentRoute = Screen.ThongBao.route },
                    onNavigateToDebug = { currentRoute = Screen.Debug.route },
                    onCategoryClick = { category ->
                        currentRoute = Screen.HocTap.route
                    }
                )
                Screen.HocTap.route -> HocTapScreen(viewModel = viewModel)
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
