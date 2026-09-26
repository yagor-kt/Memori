package com.example.memori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import com.example.memori.ui.login.LoginScreen
import com.example.memori.ui.login.LoginViewModel
import com.example.memori.ui.main.MainScreen
import com.example.memori.ui.main.MainViewModel
import com.example.memori.ui.theme.MemoriTheme

@Serializable
data object LoginRoute

@Serializable
data object MainRoute

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MemoriTheme {
                MemoriNavHost(application = application as App)
            }
        }
    }
}

@Composable
private fun MemoriNavHost(
    application: App,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = LoginRoute
    ) {
        composable<LoginRoute> {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(application)
            )

            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(MainRoute) {
                        popUpTo<LoginRoute> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<MainRoute> {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(application)
            )

            MainScreen(
                viewModel = mainViewModel,
                onChangePlayer = {
                    application.preferences.edit()
                        .remove(App.CURRENT_PLAYER_ID_KEY)
                        .apply()

                    navController.navigate(LoginRoute) {
                        popUpTo<MainRoute> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}