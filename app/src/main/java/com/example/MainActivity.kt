package com.example

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleAuthHelper
import com.example.data.AppThemeMode
import com.example.data.FirestoreOvertimeRepository
import com.example.data.LocalAppPreferences
import com.example.data.PreferencesStorage
import com.example.ui.OvertimeViewModel
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.AbsenLemburkuSplashScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppShell
import com.example.ui.theme.OvertimeTrackerTheme
import com.example.util.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    /**
     * Lock app font scale to 1.0f at the Activity Configuration layer so changing the
     * system font size in Android device settings never alters typography inside the app.
     */
    override fun attachBaseContext(newBase: Context) {
        val fixedConfig = Configuration(newBase.resources.configuration).apply {
            fontScale = 1.0f
        }
        val fixedContext = newBase.createConfigurationContext(fixedConfig)
        super.attachBaseContext(fixedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OvertimeAppRoot()
        }
    }
}

@Composable
private fun OvertimeAppRoot() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    var showSplash by rememberSaveable { mutableStateOf(true) }

    val preferencesStorage = remember(appContext) { PreferencesStorage(appContext) }
    val localPrefs by preferencesStorage.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = LocalAppPreferences()
    )

    val authFlow = remember { GoogleAuthHelper.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    val credentialManager = remember(context) { CredentialManager.create(context) }

    OvertimeTrackerTheme(
        darkTheme = localPrefs.themeMode == AppThemeMode.DARK
    ) {
        Crossfade(
            targetState = showSplash,
            animationSpec = tween(durationMillis = 380),
            label = "splash_crossfade"
        ) { isShowingSplash ->
            if (isShowingSplash) {
                AbsenLemburkuSplashScreen(
                    onFinished = { showSplash = false }
                )
            } else if (currentUser == null) {
                AuthScreen(
                    strings = stringsFor(localPrefs.language),
                    onAuthSuccess = { /* authStateFlow automatically transitions to authenticated UI */ }
                )
            } else {
                val user = currentUser!!
                key(user.uid) {
                    val databaseId = remember(appContext) {
                        appContext.getString(R.string.firestore_database_id)
                    }
                    val viewModel: OvertimeViewModel = viewModel(
                        key = user.uid,
                        factory = viewModelFactory {
                            initializer {
                                val firestore = FirebaseFirestore.getInstance(databaseId)
                                val repository = FirestoreOvertimeRepository(firestore)
                                val networkMonitor = NetworkMonitor(appContext)
                                OvertimeViewModel(
                                    repository = repository,
                                    preferencesStorage = preferencesStorage,
                                    networkMonitor = networkMonitor,
                                    currentUserId = user.uid,
                                    currentUserDisplayName = user.displayName.orEmpty(),
                                    currentUserEmail = user.email.orEmpty()
                                )
                            }
                        }
                    )
                    MainAppShell(
                        viewModel = viewModel,
                        onSignOut = {
                            GoogleAuthHelper.signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }
}
