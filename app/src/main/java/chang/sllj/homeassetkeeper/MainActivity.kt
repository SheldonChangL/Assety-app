package chang.sllj.homeassetkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import chang.sllj.homeassetkeeper.ui.navigation.AppBottomNavBar
import chang.sllj.homeassetkeeper.ui.navigation.AppNavHost
import chang.sllj.homeassetkeeper.ui.navigation.Screen
import chang.sllj.homeassetkeeper.ui.navigation.showBottomBar
import chang.sllj.homeassetkeeper.ui.navigation.showFab
import chang.sllj.homeassetkeeper.ui.theme.HomeAssetKeeperTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. @AndroidEntryPoint enables Hilt injection into this
 * activity and all Composables that use hiltViewModel() within its scope.
 *
 * A single [NavHostController] is created here and shared with both the
 * [AppNavHost] (which drives the NavHost routes) and the [AppBottomNavBar]
 * (which reads the current back-stack entry to highlight the correct tab).
 * This ensures the bottom nav and the navigation graph are always in sync.
 *
 * The global Add-Asset FAB is owned by this root Scaffold so it is
 * automatically hidden on screens that do not belong to the bottom nav
 * (Detail, Form, Camera). The Scaffold's innerPadding is forwarded to
 * AppNavHost so nested screens can apply the correct bottom clearance
 * (BottomNavigationBar + FAB + system navigation bar).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent {
            HomeAssetKeeperTheme {
                val navController     = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute      = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar(currentRoute)) {
                            AppBottomNavBar(navController = navController)
                        }
                    },
                    floatingActionButton = {
                        // Show the FAB only on the Home and Assets tabs.
                        // On Detail, Form, Camera, and Settings the FAB is hidden so
                        // it does not compete with the screen-specific Save/Edit actions.
                        if (showFab(currentRoute)) {
                            FloatingActionButton(
                                onClick = { navController.navigate(Screen.formNew()) }
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.Add,
                                    contentDescription = "Add asset"
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Pass the root Scaffold's innerPadding to AppNavHost so that
                    // screens with nested Scaffolds (HomeScreen, ItemListScreen) can
                    // apply the correct bottom content padding.
                    AppNavHost(
                        navController = navController,
                        outerPadding  = innerPadding
                    )
                }
            }
        }
    }
}
