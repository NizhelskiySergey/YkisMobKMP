package com.ykis.ykismobkmp.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ykis.mob.ui.navigation.SignInScreen
import com.ykis.mob.ui.navigation.SignUpScreen
import com.ykis.mob.ui.navigation.VerifyEmailScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreen
import com.ykis.ykismobkmp.ui.screens.auth.SignUpScreenModel
import org.koin.compose.viewmodel.koinViewModel

fun NavGraphBuilder.authNavGraph(
  navController: NavHostController,signUpViewModel: SignUpScreenModel
) {
  navigation(
    route = Graph.AUTHENTICATION,
    startDestination = SignInScreen.route
  ) {
    composable(SignInScreen.route) {
      SignInScreen(
        openScreen = { route -> navController.navigate(route) },
        navController = navController
      )
    }

    composable(SignUpScreen.route) {
      // ViewModel создается только для этого экрана
      val viewModel: SignUpScreenModel = koinViewModel()
      SignUpScreen(
        viewModel = viewModel,
        navController = navController
      )
    }

    composable(VerifyEmailScreen.route) {
      val viewModel: SignUpScreenModel = koinViewModel()
      VerifyEmailScreen(
        restartApp = { route ->
          navController.navigate(route) {
            // Очищаем ВООБЩЕ всё под ноль
            popUpTo(0) { inclusive = true }
          }
        },
        viewModel = viewModel,
        navController = navController
      )
    }

  }
}
