package com.andrewnguyen.bowpress.feature.equipment.nav

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andrewnguyen.bowpress.feature.equipment.arrow.AddArrowScreen
import com.andrewnguyen.bowpress.feature.equipment.arrow.AddArrowViewModel
import com.andrewnguyen.bowpress.feature.equipment.arrow.ArrowDetailScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.AddBowScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.AddBowViewModel
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigDetailScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.BowConfigEditScreen
import com.andrewnguyen.bowpress.feature.equipment.bow.BowDetailScreen
import com.andrewnguyen.bowpress.feature.equipment.home.EquipmentHomeScreen
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Routes owned by the equipment feature. The top-level `equipment/home` route
 * is the tab target — everything else is a child of the equipment stack.
 *
 *   equipment/home
 *   equipment/bow/{bowId}
 *   equipment/bow/{bowId}/config/{configId}
 *   equipment/bow/{bowId}/config/{configId}/edit
 *   equipment/arrow/{arrowId}
 *
 * Add Bow and Add Arrow are presented as ModalBottomSheets from `home`
 * (no dedicated route) so the spatial relationship to the Equipment list
 * is preserved, matching iOS .sheet() presentation.
 *
 * Wire this into the root NavHost from the app module:
 *   `equipmentNavGraph(navController, currentUserId = { … })`
 */
object EquipmentRoutes {
    const val HOME = "equipment/home"
    const val BOW_DETAIL = "equipment/bow/{${EquipmentArgs.BOW_ID}}"
    const val BOW_CONFIG_DETAIL = "equipment/bow/{${EquipmentArgs.BOW_ID}}/config/{${EquipmentArgs.CONFIG_ID}}"
    const val BOW_CONFIG_EDIT = "equipment/bow/{${EquipmentArgs.BOW_ID}}/config/{${EquipmentArgs.CONFIG_ID}}/edit"
    const val ARROW_DETAIL = "equipment/arrow/{${EquipmentArgs.ARROW_ID}}"
    const val SIGHT_MARKS = "equipment/bow/{${EquipmentArgs.BOW_ID}}/sight-marks"

    fun bowDetail(bowId: String) = "equipment/bow/$bowId"
    fun bowConfigDetail(bowId: String, configId: String) = "equipment/bow/$bowId/config/$configId"
    fun bowConfigEdit(bowId: String, configId: String) = "equipment/bow/$bowId/config/$configId/edit"
    fun arrowDetail(arrowId: String) = "equipment/arrow/$arrowId"
    fun sightMarks(bowId: String) = "equipment/bow/$bowId/sight-marks"
}

/**
 * Register the equipment feature's routes on [navController]. Callers provide a
 * [currentUserId] provider so creation flows can stamp a userId on new rows —
 * the equipment feature itself doesn't own the auth session.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.equipmentNavGraph(
    navController: NavHostController,
    currentUserId: () -> String,
) {
    composable(EquipmentRoutes.HOME) {
        var addBowSheetOpen by remember { mutableStateOf(false) }
        var addArrowSheetOpen by remember { mutableStateOf(false) }
        val addBowSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val addArrowSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        EquipmentHomeScreen(
            onAddBow = { addBowSheetOpen = true },
            onOpenBow = { id -> navController.navigate(EquipmentRoutes.bowDetail(id)) },
            onAddArrow = { addArrowSheetOpen = true },
            onOpenArrow = { id -> navController.navigate(EquipmentRoutes.arrowDetail(id)) },
        )

        if (addBowSheetOpen) {
            // Per-open ViewModel slot. Without a unique key, hiltViewModel()
            // returns the same AddBowViewModel across sheet open/close cycles
            // (it's scoped to this NavBackStackEntry), and any `saved…` signal
            // left set from the previous save immediately re-fires the dismiss
            // effect. The key is forgotten when this if-block exits, so the
            // next open allocates a fresh slot + fresh VM.
            val sheetKey = remember { UUID.randomUUID().toString() }
            ModalBottomSheet(
                onDismissRequest = { addBowSheetOpen = false },
                sheetState = addBowSheetState,
            ) {
                AddBowScreen(
                    userId = currentUserId(),
                    viewModel = hiltViewModel<AddBowViewModel>(key = sheetKey),
                    onBowCreated = { id ->
                        scope.launch { addBowSheetState.hide() }
                            .invokeOnCompletion {
                                addBowSheetOpen = false
                                navController.navigate(EquipmentRoutes.bowDetail(id))
                            }
                    },
                    onCancel = {
                        scope.launch { addBowSheetState.hide() }
                            .invokeOnCompletion { addBowSheetOpen = false }
                    },
                )
            }
        }

        if (addArrowSheetOpen) {
            val sheetKey = remember { UUID.randomUUID().toString() }
            ModalBottomSheet(
                onDismissRequest = { addArrowSheetOpen = false },
                sheetState = addArrowSheetState,
            ) {
                AddArrowScreen(
                    userId = currentUserId(),
                    viewModel = hiltViewModel<AddArrowViewModel>(key = sheetKey),
                    onCreated = { _ ->
                        // iOS AddArrowView.save() just dismisses — no nav to detail
                        // (unlike AddBow which DOES nav). Mirror iOS so the user
                        // lands back on the Equipment list to confirm the new row.
                        scope.launch { addArrowSheetState.hide() }
                            .invokeOnCompletion { addArrowSheetOpen = false }
                    },
                    onCancel = {
                        scope.launch { addArrowSheetState.hide() }
                            .invokeOnCompletion { addArrowSheetOpen = false }
                    },
                )
            }
        }
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
            onOpenSightMarks = { navController.navigate(EquipmentRoutes.sightMarks(bowId)) },
        )
    }

    composable(
        route = EquipmentRoutes.SIGHT_MARKS,
        arguments = listOf(navArgument(EquipmentArgs.BOW_ID) { type = NavType.StringType }),
    ) {
        com.andrewnguyen.bowpress.feature.equipment.sightmarks.SightMarksListScreen(
            onBack = { navController.popBackStack() },
            userId = currentUserId(),
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

    composable(
        route = EquipmentRoutes.ARROW_DETAIL,
        arguments = listOf(navArgument(EquipmentArgs.ARROW_ID) { type = NavType.StringType }),
    ) {
        ArrowDetailScreen(onBack = { navController.popBackStack() })
    }
}
