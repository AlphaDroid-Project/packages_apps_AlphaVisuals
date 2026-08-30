/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alpha.settings.ui.ui

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.android.internal.util.alpha.Utils
import com.alpha.settings.ui.ui.UiStylePreviewDetailScreen
import com.alpha.settings.ui.viewmodel.ThemeStoreViewModel

/** Routes a caller may deep link into through MainActivity.EXTRA_DESTINATION. */
private val DEEP_LINK_ROUTES = setOf("ui_styles", "monet_settings", "installed_components")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(viewModel: ThemeStoreViewModel, destination: String? = null) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var deepLinkConsumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(destination) {
        if (!deepLinkConsumed && destination != null && DEEP_LINK_ROUTES.contains(destination)) {
            deepLinkConsumed = true
            navController.navigate(destination)
        }
    }

    var showLegacySystemUiRestartDialog by remember { mutableStateOf(false) }
    val deferredThemeOp by viewModel.deferredSystemUiThemeOp.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.systemUiRestartPrompt.collect {
            showLegacySystemUiRestartDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.restartSystemUiAfterDeferred.collect {
            Toast.makeText(
                context,
                com.android.internal.R.string.systemui_restart_process,
                Toast.LENGTH_LONG,
            ).show()
            Handler(Looper.getMainLooper()).postDelayed({
                Utils.restartSystemUI()
            }, 2000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkInstallStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    DisposableEffect(context) {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                viewModel.loadThemes(forceRefresh = true)
                viewModel.loadIconPacks()
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        
        context.registerReceiver(packageReceiver, filter)
        onDispose {
            context.unregisterReceiver(packageReceiver)
        }
    }
    
    val motionScheme = MaterialTheme.motionScheme
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "themes",
            enterTransition = { fadeIn(motionScheme.defaultEffectsSpec()) },
            exitTransition = { fadeOut(motionScheme.defaultEffectsSpec()) },
            popEnterTransition = { fadeIn(motionScheme.defaultEffectsSpec()) },
            popExitTransition = { fadeOut(motionScheme.defaultEffectsSpec()) },
        ) {
            composable("themes") {
                ThemeStoreScreen(
                    viewModel = viewModel,
                    onThemeClick = { theme ->
                        navController.navigate("detail/${theme.id}")
                    },
                    onNavigateToInstalledComponents = {
                        navController.navigate("installed_components")
                    },
                    onNavigateToUiStyles = {
                        navController.navigate("ui_styles")
                    },
                    onNavigateToMonetSettings = {
                        navController.navigate("monet_settings")
                    },
                )
            }

            composable("monet_settings") {
                MonetSettingsScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable("ui_styles") {
                UiStylePreviewDetailScreen(
                    viewModel = viewModel,
                    styleId = viewModel.systemUiStyleId.value,
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable("installed_components") {
                InstalledComponentsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = "category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                categoryId?.let {
                    CategoryThemesScreen(
                        categoryId = it,
                        viewModel = viewModel,
                        onThemeClick = { theme ->
                            navController.navigate("detail/${theme.id}")
                        },
                        onBackClick = {
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable(
                route = "ui_style_preview/{styleId}",
                arguments = listOf(navArgument("styleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val styleId = backStackEntry.arguments?.getString("styleId").orEmpty()
                UiStylePreviewDetailScreen(
                    viewModel = viewModel,
                    styleId = styleId,
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(
                route = "detail/{themeId}",
                arguments = listOf(navArgument("themeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val themeId = backStackEntry.arguments?.getString("themeId")
                val uiState = viewModel.uiState.value
                val theme = uiState.themes.find { it.id == themeId }

                if (theme != null) {
                    ThemeDetailScreen(
                        theme = theme,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(themeId) {
                        navController.popBackStack()
                    }
                }
            }
        }

        if (deferredThemeOp != null) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.finalizeDeferredSystemUiThemeOp(restartAfter = false)
                },
                title = {
                    Text(stringResource(com.android.internal.R.string.systemui_restart_title))
                },
                text = {
                    Text(stringResource(com.android.internal.R.string.systemui_restart_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.finalizeDeferredSystemUiThemeOp(restartAfter = true)
                        },
                    ) {
                        Text(stringResource(com.android.internal.R.string.systemui_restart_yes))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.finalizeDeferredSystemUiThemeOp(restartAfter = false)
                        },
                    ) {
                        Text(stringResource(com.android.internal.R.string.systemui_restart_not_now))
                    }
                },
            )
        } else if (showLegacySystemUiRestartDialog) {
            AlertDialog(
                onDismissRequest = { showLegacySystemUiRestartDialog = false },
                title = {
                    Text(stringResource(com.android.internal.R.string.systemui_restart_title))
                },
                text = {
                    Text(stringResource(com.android.internal.R.string.systemui_restart_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLegacySystemUiRestartDialog = false
                            Toast.makeText(
                                context,
                                com.android.internal.R.string.systemui_restart_process,
                                Toast.LENGTH_LONG,
                            ).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                Utils.restartSystemUI()
                            }, 2000)
                        },
                    ) {
                        Text(stringResource(com.android.internal.R.string.systemui_restart_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLegacySystemUiRestartDialog = false }) {
                        Text(stringResource(com.android.internal.R.string.systemui_restart_not_now))
                    }
                },
            )
        }
    }
}
