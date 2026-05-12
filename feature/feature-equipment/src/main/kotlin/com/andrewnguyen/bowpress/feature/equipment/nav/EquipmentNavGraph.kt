package com.andrewnguyen.bowpress.feature.equipment.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andrewnguyen.bowpress.feature.equipment.arrow.AddArrowScreen
import com.andrewnguyen.bowpress.feature.equipment.arrow.ArrowDetailScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.AddBowScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigDetailScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigEditScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.BowDetailScreen
import com.andrewnguyen.bowpress.feature.equipment.home.EquipmentHomeScreen

/**
 * Routes owned by the equipment feature. The top-level `equipment/home` route
 * is the tab target — everything else is a child of the equipment stack.
 *
 *   equipment/home
 *   equipment/bow/add
 *   equipment/bow/{bowId}
 *   equipment/bow/{bowId}/config/{configId}
 *   equipment/bow/{bowId}/config/{configId}/edit
 *   equipment/arrow/add
 *   equipment/arrow/{arrowId}
 *
 * Wire this into the root NavHost from the app module:
 *   `equipmentNavGraph(navController, currentUserId = { … })`
 */
object EquipmentRoutes {
    const val HOME = "equipment/home"
    const val BOW_ADD = "equipment/bow/add"
    const val BOW_DETAIL = "equipment/bow/{${EquipmentArgs.BOW_ID}}"
    const val BOW_CONFIG_DETAIL = "equipment/bow/{${EquipmentArgs.BOW_ID}}/config/{${EquipmentArgs.CONFIG_ID}}"
    const val BOW_CONFIG_EDIT = "equipment/bow/{${EquipmentArgs.BOW_ID}}/config/{${EquipmentArgs.CONFIG_ID}}/edit"
    const val ARROW_ADD = "equipment/arrow/add"
    const val ARROW_DETAIL = "equipment/arrow/{${EquipmentArgs.ARROW_ID}}"

    fun bowDetail(bowId: String) = "equipment/bow/$bowId"
    fun bowConfigDetail(bowId: String, configId: String) = "equipment/bow/$bowId/config/$configId"
    fun bowConfigEdit(bowId: String, configId: String) = "equipment/bow/$bowId/config/$configId/edit"
    fun arrowDetail(arrowId: String) = "equipment/arrow/$arrowId"
}

/**
 * Register the equipment feature's routes on [navController]. Callers provide a
 * [currentUserId] provider so creation flows can stamp a userId on new rows —
 * the equipment feature itself doesn't own the auth session.
 */
fun NavGraphBuilder.equipmentNavGraph(
    navController: NavHostController,
    currentUserId: () -> String,
) {
    composable(EquipmentRoutes.HOME) {
        EquipmentHomeScreen(
            onAddBow = { navController.navigate(EquipmentRoutes.BOW_ADD) },
            onOpenBow = { id -> navController.navigate(EquipmentRoutes.bowDetail(id)) },
            onAddArrow = { navController.navigate(EquipmentRoutes.ARROW_ADD) },
            onOpenArrow = { id -> navController.navigate(EquipmentRoutes.arrowDetail(id)) },
        )
    }

    composable(EquipmentRoutes.BOW_ADD) {
        AddBowScreen(
            userId = currentUserId(),
            onBowCreated = { id ->
                navController.navigate(EquipmentRoutes.bowDetail(id)) {
                    popUpTo(EquipmentRoutes.HOME)
                }
            },
            onCancel = { navController.popBackStack() },
        )
    }

    composable(
        route = EquipmentRoutes.BOW_DETAIL,
        arguments = listOf(navArgument(EquipmentArgs.BOW_ID) { type = NavType.StringType }),
    ) {
        val bowId = it.arguments?.getString(EquipmentArgs.BOW_ID).orEmpty()
        BowDetailScreen(
            onBack = { navController.popBackStack() },
            onOpenConfig = { configId ->
                navController.navigate(EquipmentRoutes.bowConfigDetail(bowId, configId))
            },
        )
    }

    composable(
        route = EquipmentRoutes.BOW_CONFIG_DETAIL,
        arguments = listOf(
            navArgument(EquipmentArgs.BOW_ID) { type = NavType.StringType },
            navArgument(EquipmentArgs.CONFIG_ID) { type = NavType.StringType },
        ),
    ) {
        val bowId = it.arguments?.getString(EquipmentArgs.BOW_ID).orEmpty()
        val configId = it.arguments?.getString(EquipmentArgs.CONFIG_ID).orEmpty()
        BowConfigDetailScreen(
            onBack = { navController.popBackStack() },
            onLogNewTuning = {
                navController.navigate(EquipmentRoutes.bowConfigEdit(bowId, configId))
            },
        )
    }

    composable(
        route = EquipmentRoutes.BOW_CONFIG_EDIT,
        arguments = listOf(
            navArgument(EquipmentArgs.BOW_ID) { type = NavType.StringType },
            navArgument(EquipmentArgs.CONFIG_ID) { type = NavType.StringType },
        ),
    ) {
        BowConfigEditScreen(
            onSaved = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
        )
    }

    composable(EquipmentRoutes.ARROW_ADD) {
        AddArrowScreen(
            userId = currentUserId(),
            onCreated = { id ->
                navController.navigate(EquipmentRoutes.arrowDetail(id)) {
                    popUpTo(EquipmentRoutes.HOME)
                }
            },
            onCancel = { navController.popBackStack() },
        )
    }

    composable(
        route = EquipmentRoutes.ARROW_DETAIL,
        arguments = listOf(navArgument(EquipmentArgs.ARROW_ID) { type = NavType.StringType }),
    ) {
        ArrowDetailScreen(onBack = { navController.popBackStack() })
    }
}
