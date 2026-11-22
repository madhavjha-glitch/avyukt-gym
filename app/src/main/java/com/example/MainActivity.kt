package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.GymRepository
import com.example.ui.admin.AdminShellScreen
import com.example.ui.admin.AdminSignInScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.public.GymHomeScreen
import com.example.ui.theme.AmitGymTheme
import com.example.util.observeIsOnline
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable
data object GymHomeRoute

@Serializable
data object AdminGateRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            AmitGymTheme {
                AmitGymNavGraph()
            }
        }
    }
}

@Composable
fun AmitGymNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = GymHomeRoute
    ) {
        composable<GymHomeRoute> {
            GymHomeScreen(
                onOpenAdminPanel = {
                    navController.navigate(AdminGateRoute)
                }
            )
        }

        composable<AdminGateRoute> {
            AdminAuthGate(
                onBackToPublicHome = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun AdminAuthGate(
    onBackToPublicHome: () -> Unit
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    if (user == null) {
        AdminSignInScreen(
            onAuthSuccess = {},
            onBackToHome = onBackToPublicHome
        )
    } else {
        val uid = user.uid
        key(uid) {
            val handleSignOut = {
                signOutUser(
                    credentialManager = credentialManager,
                    scope = coroutineScope
                )
            }

            val adminViewModel: AdminViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        AdminViewModel(
                            repository = GymRepository(db, auth),
                            currentUserId = uid,
                            isOnlineFlow = observeIsOnline(app)
                        )
                    }
                }
            )
            AdminShellScreen(
                viewModel = adminViewModel,
                onSignOut = handleSignOut,
                onBackToPublicHome = onBackToPublicHome
            )
        }
    }
}
