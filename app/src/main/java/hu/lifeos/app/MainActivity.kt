package hu.lifeos.app

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hu.lifeos.app.food.FoodViewModel
import hu.lifeos.app.satori.SatoriViewModel
import hu.lifeos.app.ui.AppRoot
import hu.lifeos.app.ui.LockScreen
import hu.lifeos.app.ui.MainViewModel
import hu.lifeos.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {

    private val locked = mutableStateOf(false)
    private val pendingAction = MutableStateFlow<String?>(null)
    private var backgroundedAt = 0L

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) { backgroundedAt = System.currentTimeMillis() }
        override fun onStart(owner: LifecycleOwner) {
            mainVm.refreshDay()
            val lockOn = mainVm.settings.value?.lockEnabled == true
            if (lockOn && backgroundedAt > 0 && System.currentTimeMillis() - backgroundedAt > 30_000) {
                locked.value = true
                authenticate()
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        super.onDestroy()
    }

    private lateinit var mainVm: MainViewModel
    private lateinit var satoriVm: SatoriViewModel
    private lateinit var foodVm: FoodViewModel
    private lateinit var modsVm: hu.lifeos.app.mods.ModsViewModel

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_ACTION)?.let { pendingAction.value = it }
    }

    override fun onStop() {
        super.onStop()
        hu.lifeos.app.widget.WidgetUpdater.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) intent?.getStringExtra(EXTRA_ACTION)?.let { pendingAction.value = it }
        enableEdgeToEdge()
        val c = container
        val factory = viewModelFactory {
            initializer { MainViewModel(c) }
            initializer { SatoriViewModel(c) }
            initializer { FoodViewModel(c) }
            initializer { hu.lifeos.app.mods.ModsViewModel(c) }
        }
        val provider = ViewModelProvider(this, factory)
        mainVm = provider[MainViewModel::class.java]
        satoriVm = provider[SatoriViewModel::class.java]
        foodVm = provider[FoodViewModel::class.java]
        modsVm = provider[hu.lifeos.app.mods.ModsViewModel::class.java]

        // Hidegindításkor a zár azonnal él (a beállítás gyorsan, helyben olvasható).
        if (savedInstanceState == null) {
            locked.value = runCatching { runBlocking { c.settings.flow.first().lockEnabled } }.getOrDefault(false)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)

        setContent {
            val settings by mainVm.settings.collectAsState()
            AppTheme(darkMode = settings?.darkMode ?: "system") {
                if (locked.value) {
                    LockScreen(onUnlock = { authenticate() })
                } else {
                    AppRoot(mainVm, satoriVm, foodVm, modsVm, pendingAction, consumeAction = { pendingAction.value = null })
                }
            }
        }
        if (locked.value) authenticate()
    }

    companion object { const val EXTRA_ACTION = "action" }

    private fun authenticate() {
        val bm = BiometricManager.from(this)
        if (bm.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
            // Nincs képernyőzár beállítva a telefonon: nem zárjuk ki a felhasználót a saját adataiból.
            locked.value = false
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { locked.value = false }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_title))
            .setSubtitle(getString(R.string.lock_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }
}
