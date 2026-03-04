package chang.sllj.homeassetkeeper.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import chang.sllj.homeassetkeeper.ui.camera.CameraScreen
import chang.sllj.homeassetkeeper.ui.detail.ItemDetailScreen
import chang.sllj.homeassetkeeper.ui.detail.NAV_ARG_ITEM_ID
import chang.sllj.homeassetkeeper.ui.form.FormScreen
import chang.sllj.homeassetkeeper.ui.form.FormViewModel
import chang.sllj.homeassetkeeper.ui.form.NAV_ARG_EDIT_ITEM_ID
import chang.sllj.homeassetkeeper.ui.home.HomeScreen
import chang.sllj.homeassetkeeper.ui.items.ItemListScreen
import chang.sllj.homeassetkeeper.ui.settings.SettingsScreen

// ── Route constants ───────────────────────────────────────────────────────────

object Screen {
    const val HOME     = "home"
    const val ITEMS    = "items"
    const val DETAIL   = "items/{$NAV_ARG_ITEM_ID}"
    const val FORM     = "form?$NAV_ARG_EDIT_ITEM_ID={$NAV_ARG_EDIT_ITEM_ID}"
    const val CAMERA   = "camera"
    const val SETTINGS = "settings"

    fun detail(itemId: String) = "items/$itemId"
    fun formEdit(editItemId: String) = "form?$NAV_ARG_EDIT_ITEM_ID=$editItemId"
    fun formNew() = "form"
}

// ── Bottom navigation ─────────────────────────────────────────────────────────

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.HOME,     "Home",     Icons.Filled.Home),
    BottomNavItem(Screen.ITEMS,    "Assets",   Icons.AutoMirrored.Filled.List),
    BottomNavItem(Screen.SETTINGS, "Settings", Icons.Filled.Settings)
)

/**
 * Key used to pass the captured image path from [CameraScreen] to [FormScreen]
 * via [androidx.navigation.NavBackStackEntry.savedStateHandle].
 */
object CameraResultKeys {
    const val IMAGE_PATH = "cam_image_path"
}

@Composable
fun AppBottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

/** Returns true when the bottom bar (and global FAB) should be visible. */
fun showBottomBar(route: String?): Boolean = route in listOf(
    Screen.HOME, Screen.ITEMS, Screen.SETTINGS
)

/** Returns true when the global Add-Asset FAB should be visible. */
fun showFab(route: String?): Boolean = route == Screen.HOME || route == Screen.ITEMS

// ── NavHost ───────────────────────────────────────────────────────────────────

/**
 * Defines all navigation routes and their composable screens.
 *
 * @param navController The [NavHostController] shared with [AppBottomNavBar].
 * @param outerPadding  The [PaddingValues] from the root Scaffold in MainActivity,
 *   forwarded to screens that need bottom-content clearance.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    outerPadding: PaddingValues = PaddingValues()
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.HOME
    ) {
        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(Screen.HOME) {
            HomeScreen(
                onNavigateToItems  = {
                    navController.navigate(Screen.ITEMS) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                onNavigateToDetail = { itemId -> navController.navigate(Screen.detail(itemId)) },
                outerPadding       = outerPadding
            )
        }

        // ── Asset list ────────────────────────────────────────────────────────
        composable(Screen.ITEMS) {
            ItemListScreen(
                onNavigateToDetail  = { itemId -> navController.navigate(Screen.detail(itemId)) },
                onNavigateToAddItem = { navController.navigate(Screen.formNew()) },
                outerPadding        = outerPadding
            )
        }

        // ── Item detail ───────────────────────────────────────────────────────
        composable(
            route     = Screen.DETAIL,
            arguments = listOf(navArgument(NAV_ARG_ITEM_ID) { type = NavType.StringType })
        ) {
            ItemDetailScreen(
                onNavigateBack   = { navController.popBackStack() },
                onNavigateToEdit = { itemId -> navController.navigate(Screen.formEdit(itemId)) }
            )
        }

        // ── Add / Edit form ───────────────────────────────────────────────────
        composable(
            route     = Screen.FORM,
            arguments = listOf(
                navArgument(NAV_ARG_EDIT_ITEM_ID) {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val formViewModel: FormViewModel = hiltViewModel(backStackEntry)
            val savedStateHandle = backStackEntry.savedStateHandle

            // Consume captured image path delivered by CameraScreen.
            val cameraImagePath = savedStateHandle.get<String>(CameraResultKeys.IMAGE_PATH)
            LaunchedEffect(cameraImagePath) {
                cameraImagePath?.let { path ->
                    formViewModel.onImageAdded(path)
                    savedStateHandle.remove<String>(CameraResultKeys.IMAGE_PATH)
                }
            }

            FormScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(Screen.CAMERA) },
                viewModel          = formViewModel
            )
        }

        // ── Camera ────────────────────────────────────────────────────────────
        composable(Screen.CAMERA) {
            CameraScreen(
                onImageCaptured = { imagePath ->
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(CameraResultKeys.IMAGE_PATH, imagePath)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Settings / Backup ─────────────────────────────────────────────────
        composable(Screen.SETTINGS) {
            SettingsScreen()
        }
    }
}
