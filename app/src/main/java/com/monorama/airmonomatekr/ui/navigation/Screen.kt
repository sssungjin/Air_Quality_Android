package com.monorama.airmonomatekr.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Logs : Screen("logs")
    object Settings : Screen("settings")
    object Login : Screen("login")
    object Register : Screen("register")
} 