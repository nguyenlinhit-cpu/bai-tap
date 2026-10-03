package com.baiviet.game.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.baiviet.game.bacay.ui.BaCayHouseRulesScreen
import com.baiviet.game.bacay.ui.BaCayRulesScreen
import com.baiviet.game.bacay.ui.BaCayTableScreen
import com.baiviet.game.bacay.ui.BaCayTutorialScreen
import com.baiviet.game.lieng.ui.LiengHouseRulesScreen
import com.baiviet.game.lieng.ui.LiengRulesScreen
import com.baiviet.game.lieng.ui.LiengTableScreen
import com.baiviet.game.lieng.ui.LiengTutorialScreen
import com.baiviet.game.poker.ui.PokerHouseRulesScreen
import com.baiviet.game.poker.ui.PokerRulesScreen
import com.baiviet.game.poker.ui.PokerTableScreen
import com.baiviet.game.poker.ui.PokerTutorialScreen
import com.baiviet.game.maubinh.ui.MauBinhHouseRulesScreen
import com.baiviet.game.maubinh.ui.MauBinhRulesScreen
import com.baiviet.game.maubinh.ui.MauBinhTableScreen
import com.baiviet.game.maubinh.ui.MauBinhTutorialScreen
import com.baiviet.game.phom.ui.PhomHouseRulesScreen
import com.baiviet.game.phom.ui.PhomRulesScreen
import com.baiviet.game.phom.ui.PhomTableScreen
import com.baiviet.game.phom.ui.PhomTutorialScreen
import com.baiviet.game.samloc.ui.SamLocHouseRulesScreen
import com.baiviet.game.samloc.ui.SamLocRulesScreen
import com.baiviet.game.samloc.ui.SamLocTableScreen
import com.baiviet.game.samloc.ui.SamLocTutorialScreen
import com.baiviet.game.tienlen.ui.TienLenHouseRulesScreen
import com.baiviet.game.tienlen.ui.TienLenRulesScreen
import com.baiviet.game.tienlen.ui.TienLenTableScreen
import com.baiviet.game.tienlen.ui.TienLenTutorialScreen
import com.baiviet.game.xidach.ui.XiDachHouseRulesScreen
import com.baiviet.game.xidach.ui.XiDachRulesScreen
import com.baiviet.game.xidach.ui.XiDachTableScreen
import com.baiviet.game.xidach.ui.XiDachTutorialScreen
import com.baiviet.game.ui.lobby.LobbyScreen
import com.baiviet.game.ui.profile.ProfileScreen
import com.baiviet.game.ui.settings.SettingsScreen
import com.baiviet.game.ui.setup.TableSetupScreen
import com.baiviet.game.ui.splash.SplashScreen

@Composable
fun BaiVietNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Route.Splash,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)) },
    ) {
        composable<Route.Splash> {
            SplashScreen(
                onFinished = {
                    navController.navigate(Route.Lobby) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.Lobby> {
            LobbyScreen(
                onGameSelected = { gameId -> navController.navigate(Route.TableSetup(gameId)) },
                onSettingsClick = { navController.navigate(Route.Settings) },
                onProfileClick = { navController.navigate(Route.Profile) },
            )
        }

        composable<Route.TableSetup> { entry ->
            val route = entry.toRoute<Route.TableSetup>()
            TableSetupScreen(
                gameId = route.gameId,
                onBack = back,
                onEnter = { players, difficulty, bet, option ->
                    navController.navigate(Route.GamePlay(route.gameId, players, difficulty, bet, option))
                },
                onRules = { navController.navigate(Route.Rules(route.gameId)) },
                onHouseRules = { navController.navigate(Route.HouseRules(route.gameId)) },
                onTutorial = { navController.navigate(Route.Tutorial(route.gameId)) },
            )
        }

        composable<Route.GamePlay> { entry ->
            val route = entry.toRoute<Route.GamePlay>()
            when (route.gameId) {
                "maubinh" -> MauBinhTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    onExit = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                "phom" -> PhomTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    onExit = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                "samloc" -> SamLocTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    onExit = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                "xidach" -> XiDachTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    dealerOption = route.option,
                    onNavigateBack = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                "bacay" -> BaCayTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    dealerOption = route.option,
                    onNavigateBack = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                "lieng" -> LiengTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    onNavigateBack = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                "poker" -> PokerTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    buyInBB = route.option,
                    onNavigateBack = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
                else -> TienLenTableScreen(
                    players = route.players,
                    difficulty = route.difficulty,
                    betUnit = route.betLevel,
                    onExit = back,
                    onOpenRules = { navController.navigate(Route.Rules(route.gameId)) },
                )
            }
        }

        composable<Route.Rules> { entry ->
            val route = entry.toRoute<Route.Rules>()
            when (route.gameId) {
                "maubinh" -> MauBinhRulesScreen(
                    onBack = back,
                    onOpenHouseRules = { navController.navigate(Route.HouseRules("maubinh")) },
                )
                "phom" -> PhomRulesScreen(onBack = back)
                "samloc" -> SamLocRulesScreen(onBack = back)
                "xidach" -> XiDachRulesScreen(onNavigateBack = back)
                "bacay" -> BaCayRulesScreen(onNavigateBack = back)
                "lieng" -> LiengRulesScreen(onNavigateBack = back)
                "poker" -> PokerRulesScreen(onNavigateBack = back)
                else -> TienLenRulesScreen(onBack = back)
            }
        }

        composable<Route.HouseRules> { entry ->
            val route = entry.toRoute<Route.HouseRules>()
            when (route.gameId) {
                "maubinh" -> MauBinhHouseRulesScreen(onBack = back)
                "phom" -> PhomHouseRulesScreen(onBack = back)
                "samloc" -> SamLocHouseRulesScreen(onBack = back)
                "xidach" -> XiDachHouseRulesScreen(onNavigateBack = back)
                "bacay" -> BaCayHouseRulesScreen(onNavigateBack = back)
                "lieng" -> LiengHouseRulesScreen(onNavigateBack = back)
                "poker" -> PokerHouseRulesScreen(onNavigateBack = back)
                else -> TienLenHouseRulesScreen(onBack = back)
            }
        }

        composable<Route.Tutorial> { entry ->
            val route = entry.toRoute<Route.Tutorial>()
            when (route.gameId) {
                "maubinh" -> MauBinhTutorialScreen(onBack = back)
                "phom" -> PhomTutorialScreen(onBack = back)
                "samloc" -> SamLocTutorialScreen(onBack = back)
                "xidach" -> XiDachTutorialScreen(onNavigateBack = back)
                "bacay" -> BaCayTutorialScreen(onNavigateBack = back)
                "lieng" -> LiengTutorialScreen(onNavigateBack = back)
                "poker" -> PokerTutorialScreen(onNavigateBack = back)
                else -> TienLenTutorialScreen(onBack = back)
            }
        }

        composable<Route.Settings> { SettingsScreen(onBack = back) }
        composable<Route.Profile> { ProfileScreen(onBack = back) }
    }
}
