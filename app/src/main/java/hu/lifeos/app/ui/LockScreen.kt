package hu.lifeos.app.ui

import androidx.compose.runtime.Composable
import hu.lifeos.app.ui.screens.LockScreenContent

@Composable
fun LockScreen(onUnlock: () -> Unit) = LockScreenContent(onUnlock)
