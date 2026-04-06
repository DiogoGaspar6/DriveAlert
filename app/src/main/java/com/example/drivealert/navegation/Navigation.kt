package com.example.drivealert.navegation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.drivealert.ui.screens.CameraScreen
import com.example.drivealert.ui.screens.HomeScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(navController)
        }

        composable("camera") {
            CameraScreen(navController)
        }
    }
}