package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.LiveRepository
import com.example.data.repository.VideoRepository
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.components.VyoraBottomBar
import com.example.ui.components.VyoraTab
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CreateVideoScreen
import com.example.ui.screens.CreatorProfileScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class ScreenState {
    FEED,
    CREATE_VIDEO,
    LIVE_ROOM,
    ADMIN_DASHBOARD,
    CREATOR_PROFILE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authRepository = AuthRepository(this)
        val videoRepository = VideoRepository(this)
        val liveRepository = LiveRepository(this)
        val adminRepository = AdminRepository(this)

        setContent {
            MyApplicationTheme {
                VyoraMainApp(
                    authRepository = authRepository,
                    videoRepository = videoRepository,
                    liveRepository = liveRepository,
                    adminRepository = adminRepository
                )
            }
        }
    }
}

@Composable
fun VyoraMainApp(
    authRepository: AuthRepository,
    videoRepository: VideoRepository,
    liveRepository: LiveRepository,
    adminRepository: AdminRepository
) {
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(VyoraTab.HOME) }
    var screenState by remember { mutableStateOf(ScreenState.FEED) }
    var showSignInDialog by remember { mutableStateOf(false) }

    var currentUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Selected Creator for Profile Navigation
    var selectedCreatorId by remember { mutableStateOf("") }
    var selectedCreatorName by remember { mutableStateOf("") }
    var selectedCreatorPhotoUrl by remember { mutableStateOf("") }

    // Observe Firebase Auth state
    LaunchedEffect(Unit) {
        authRepository.attemptAutoSignIn(
            scope = scope,
            onSuccess = { user ->
                scope.launch {
                    val profile = authRepository.syncUserProfile(user)
                    currentUserProfile = profile
                }
            },
            onUnauthenticated = {
                currentUserProfile = null
            }
        )

        authRepository.observeAuthState().collect { firebaseUser ->
            if (firebaseUser != null) {
                authRepository.observeUserProfile(firebaseUser.uid).collect { profile ->
                    currentUserProfile = profile
                }
            } else {
                currentUserProfile = null
            }
        }
    }

    // Android back button navigation handler
    BackHandler(enabled = screenState != ScreenState.FEED || currentTab != VyoraTab.HOME) {
        when {
            screenState == ScreenState.CREATOR_PROFILE -> {
                screenState = ScreenState.FEED
            }
            screenState != ScreenState.FEED -> {
                screenState = ScreenState.FEED
            }
            currentTab != VyoraTab.HOME -> {
                currentTab = VyoraTab.HOME
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        bottomBar = {
            if (screenState == ScreenState.FEED) {
                VyoraBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        when (tab) {
                            VyoraTab.HOME -> {
                                currentTab = VyoraTab.HOME
                            }
                            VyoraTab.FRIENDS -> {
                                currentTab = VyoraTab.FRIENDS
                            }
                            VyoraTab.CREATE -> {
                                if (currentUserProfile == null) {
                                    showSignInDialog = true
                                } else {
                                    screenState = ScreenState.CREATE_VIDEO
                                }
                            }
                            VyoraTab.MESSAGES -> {
                                currentTab = VyoraTab.MESSAGES
                            }
                            VyoraTab.PROFILE -> {
                                if (currentUserProfile == null) {
                                    showSignInDialog = true
                                } else {
                                    currentTab = VyoraTab.PROFILE
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (screenState == ScreenState.FEED) innerPadding.calculateBottomPadding() else Color.Transparent.run { androidx.compose.ui.unit.Dp(0f) })
        ) {
            when (screenState) {
                ScreenState.CREATE_VIDEO -> {
                    CreateVideoScreen(
                        videoRepository = videoRepository,
                        onNavigateBack = { screenState = ScreenState.FEED },
                        onVideoPublished = {
                            screenState = ScreenState.FEED
                            currentTab = VyoraTab.HOME
                        }
                    )
                }
                ScreenState.LIVE_ROOM -> {
                    LiveScreen(
                        liveRepository = liveRepository,
                        currentUserProfile = currentUserProfile,
                        onRequestSignIn = { showSignInDialog = true },
                        onNavigateBack = { screenState = ScreenState.FEED }
                    )
                }
                ScreenState.ADMIN_DASHBOARD -> {
                    AdminDashboardScreen(
                        adminRepository = adminRepository,
                        videoRepository = videoRepository,
                        liveRepository = liveRepository,
                        onNavigateBack = { screenState = ScreenState.FEED }
                    )
                }
                ScreenState.CREATOR_PROFILE -> {
                    CreatorProfileScreen(
                        authorId = selectedCreatorId,
                        authorName = selectedCreatorName,
                        authorPhotoUrl = selectedCreatorPhotoUrl,
                        videoRepository = videoRepository,
                        currentUserProfile = currentUserProfile,
                        onRequestSignIn = { showSignInDialog = true },
                        onNavigateBack = { screenState = ScreenState.FEED }
                    )
                }
                ScreenState.FEED -> {
                    when (currentTab) {
                        VyoraTab.HOME -> {
                            HomeScreen(
                                videoRepository = videoRepository,
                                currentUserProfile = currentUserProfile,
                                onRequestSignIn = { showSignInDialog = true },
                                onNavigateToCreate = {
                                    if (currentUserProfile == null) {
                                        showSignInDialog = true
                                    } else {
                                        screenState = ScreenState.CREATE_VIDEO
                                    }
                                },
                                onNavigateToLive = {
                                    screenState = ScreenState.LIVE_ROOM
                                },
                                onNavigateToCreator = { authorId, authorName, authorPhotoUrl ->
                                    selectedCreatorId = authorId
                                    selectedCreatorName = authorName
                                    selectedCreatorPhotoUrl = authorPhotoUrl
                                    screenState = ScreenState.CREATOR_PROFILE
                                },
                                onAdminModerateVideo = { video: Video ->
                                    screenState = ScreenState.ADMIN_DASHBOARD
                                }
                            )
                        }
                        VyoraTab.FRIENDS -> {
                            FriendsScreen(
                                videoRepository = videoRepository,
                                currentUserProfile = currentUserProfile,
                                onRequestSignIn = { showSignInDialog = true },
                                onNavigateToCreator = { authorId, authorName, authorPhotoUrl ->
                                    selectedCreatorId = authorId
                                    selectedCreatorName = authorName
                                    selectedCreatorPhotoUrl = authorPhotoUrl
                                    screenState = ScreenState.CREATOR_PROFILE
                                },
                                onAdminModerateVideo = { video: Video ->
                                    screenState = ScreenState.ADMIN_DASHBOARD
                                }
                            )
                        }
                        VyoraTab.MESSAGES -> {
                            MessagesScreen(
                                currentUserProfile = currentUserProfile,
                                onRequestSignIn = { showSignInDialog = true }
                            )
                        }
                        VyoraTab.PROFILE -> {
                            ProfileScreen(
                                currentUserProfile = currentUserProfile,
                                authRepository = authRepository,
                                videoRepository = videoRepository,
                                onRequestSignIn = { showSignInDialog = true },
                                onNavigateToAdminDashboard = {
                                    screenState = ScreenState.ADMIN_DASHBOARD
                                }
                            )
                        }
                        VyoraTab.CREATE -> {
                            // Handled by state switch
                        }
                    }
                }
            }
        }
    }

    // Google Sign-In Modal Dialog
    if (showSignInDialog) {
        GoogleSignInDialog(
            authRepository = authRepository,
            onDismiss = { showSignInDialog = false },
            onSuccess = { user ->
                showSignInDialog = false
                scope.launch {
                    val profile = authRepository.syncUserProfile(user)
                    currentUserProfile = profile
                    if (currentTab == VyoraTab.PROFILE) {
                        currentTab = VyoraTab.PROFILE
                    }
                }
            }
        )
    }
}
