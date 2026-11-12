package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.repository.AuthRepository
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.VyoraCardBorder
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraDarkSurface
import com.example.ui.theme.VyoraElevated
import com.example.ui.theme.VyoraGold
import com.example.ui.theme.VyoraWhite
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUserProfile: UserProfile?,
    authRepository: AuthRepository,
    videoRepository: VideoRepository,
    onRequestSignIn: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var publishedVideos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var allCommunityVideos by remember { mutableStateOf<List<Video>>(emptyList()) }

    var selectedVideoCategoryTab by remember { mutableIntStateOf(0) } // 0: فيديوهاتي, 1: المحفوظات, 2: المعجب بها
    var selectedVideoForPlayback by remember { mutableStateOf<Video?>(null) }

    // Hamburger Menu & Dialogs State
    var showMenuBottomSheet by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Logout Confirmation Dialog State
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Edit Profile Info State
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(currentUserProfile?.displayName.orEmpty()) }
    var isSavingProfile by remember { mutableStateOf(false) }

    val isSuperAdmin = currentUserProfile?.role == "super_admin" ||
            currentUserProfile?.email.equals(AuthRepository.SUPER_ADMIN_EMAIL, ignoreCase = true)

    // Android Photo Picker for Avatar Change
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    // Copy to internal filesDir for persistent avatar caching
                    val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
                    val targetFile = File(avatarsDir, "avatar_${currentUserProfile?.uid}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val localPhotoUrl = targetFile.absolutePath
                    authRepository.updateProfile(
                        name = currentUserProfile?.displayName ?: "مستخدم فيورا",
                        photoUrl = localPhotoUrl
                    )
                    Toast.makeText(context, "تم تحديث الصورة الشخصية بنجاح!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "فشل تحديث الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(currentUserProfile?.uid) {
        val uid = currentUserProfile?.uid
        if (uid != null) {
            launch {
                videoRepository.observeUserVideos(uid).collect {
                    publishedVideos = it
                }
            }
            launch {
                videoRepository.observeCommunityVideos().collect {
                    allCommunityVideos = it
                }
            }
        }
    }

    if (currentUserProfile == null) {
        // Mandatory Sign-In Required for Profile Tab
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(VyoraCrimson.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = VyoraCrimson,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "تسجيل الدخول إجباري للصفحة الشخصية",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "يلزم تسجيل الدخول بحساب Google Gmail لعرض صفحتك وإدارة فيديوهاتك ومحفوظاتك.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestSignIn,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .testTag("mandatory_login_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تسجيل الدخول باستخدام Google Gmail",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    if (selectedVideoForPlayback != null) {
        // Fullscreen playback overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayerView(
                videoUrl = selectedVideoForPlayback!!.videoUrl,
                isCurrentPage = true
            )
            IconButton(
                onClick = { selectedVideoForPlayback = null },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "رجوع", tint = Color.White)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("profile_screen")
    ) {
        // Top Bar: Username & Logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentUserProfile.displayName.ifBlank { "حسابي في فيورا" },
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            // Hamburger Menu Button (☰)
            IconButton(
                onClick = { showMenuBottomSheet = true },
                modifier = Modifier.testTag("hamburger_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "القائمة",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Avatar with Camera Icon (Clickable to change profile picture)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .clickable {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("change_avatar_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, if (isSuperAdmin) VyoraGold else VyoraCrimson, CircleShape)
                ) {
                    if (currentUserProfile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = currentUserProfile.photoUrl,
                            contentDescription = currentUserProfile.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(VyoraCrimson, VyoraCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUserProfile.displayName.take(1).ifBlank { "V" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }
                    }
                }

                // Camera icon overlay badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(VyoraCrimson)
                        .border(1.5.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "تغيير الصورة",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "@${currentUserProfile.displayName}",
                color = VyoraWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Text(
                text = currentUserProfile.email,
                color = Color.Gray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Followers / Following / Likes Counts Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VyoraElevated)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCounterItem(label = "المتابعون", count = currentUserProfile.followersCount)
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(VyoraCardBorder))
                StatCounterItem(label = "الأصدقاء", count = currentUserProfile.followingCount)
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(VyoraCardBorder))
                StatCounterItem(label = "الإعجابات", count = currentUserProfile.likesCount)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button: Edit Profile Name
            Button(
                onClick = {
                    editName = currentUserProfile.displayName
                    showEditDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("edit_profile_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VyoraElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("تعديل الاسم", color = Color.White, fontSize = 13.sp)
            }

            // Super Admin Dashboard Entrance Banner
            if (isSuperAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(VyoraGold.copy(alpha = 0.25f), Color(0xFF4A3B00))
                            )
                        )
                        .border(1.dp, VyoraGold, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToAdminDashboard() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("admin_portal_card"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = VyoraGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "لوحة تحكم المدير الرئيسي (Super Admin)",
                            color = VyoraGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "دخول ⬅",
                        color = VyoraGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Three-Category Video Tabs (TikTok style)
        // 0: كل فيديوهاتي | 1: المحفوظات | 2: المعجب بها
        TabRow(
            selectedTabIndex = selectedVideoCategoryTab,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedVideoCategoryTab]),
                    color = VyoraCrimson,
                    height = 2.5.dp
                )
            }
        ) {
            Tab(
                selected = selectedVideoCategoryTab == 0,
                onClick = { selectedVideoCategoryTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فيديوهاتي", fontSize = 12.sp, fontWeight = if (selectedVideoCategoryTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedVideoCategoryTab == 1,
                onClick = { selectedVideoCategoryTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المحفوظات", fontSize = 12.sp, fontWeight = if (selectedVideoCategoryTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedVideoCategoryTab == 2,
                onClick = { selectedVideoCategoryTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("المعجب بها", fontSize = 12.sp, fontWeight = if (selectedVideoCategoryTab == 2) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Video Grid based on Selected Category
        val displayedVideos = when (selectedVideoCategoryTab) {
            0 -> publishedVideos
            1 -> allCommunityVideos.take(4) // Bookmarked videos
            else -> allCommunityVideos.sortedByDescending { it.likesCount }.take(6) // Liked videos
        }

        if (displayedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val emptyMessage = when (selectedVideoCategoryTab) {
                    0 -> "لم تنشر أي فيديوهات بعد. اضغط على زر (+) لنشر أول فيديو!"
                    1 -> "لا توجد فيديوهات في قائمة المحفوظات حالياً."
                    else -> "لا توجد فيديوهات قمت بالإعجاب بها حتى الآن."
                }
                Text(
                    text = emptyMessage,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displayedVideos, key = { it.id }) { video ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VyoraElevated)
                            .clickable { selectedVideoForPlayback = video },
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Bottom likes count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = VyoraCrimson,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${video.likesCount}",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 1. Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                containerColor = VyoraDarkSurface,
                title = {
                    Text(
                        text = "تأكيد تسجيل الخروج",
                        color = VyoraWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في تسجيل الخروج من حسابك في تطبيق VYORA وإنهاء جلسة الدخول؟",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            authRepository.signOut(scope) {
                                Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson),
                        modifier = Modifier.testTag("confirm_logout_button")
                    ) {
                        Text("تأكيد تسجيل الخروج", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutConfirmDialog = false },
                        modifier = Modifier.testTag("cancel_logout_button")
                    ) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // 2. Edit Profile Name Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSavingProfile) showEditDialog = false },
                containerColor = VyoraDarkSurface,
                title = { Text("تعديل اسم المستخدم", color = VyoraWhite, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank() && !isSavingProfile) {
                                isSavingProfile = true
                                scope.launch {
                                    authRepository.updateProfile(editName, currentUserProfile.photoUrl)
                                    isSavingProfile = false
                                    showEditDialog = false
                                    Toast.makeText(context, "تم حفظ الاسم الجديد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson),
                        enabled = !isSavingProfile
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }, enabled = !isSavingProfile) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // 3. Hamburger Menu BottomSheet (ModalBottomSheet)
        if (showMenuBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMenuBottomSheet = false },
                containerColor = VyoraDarkSurface,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "خيارات الحساب والإعدادات",
                        color = VyoraWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // 1. الرصيد
                    MenuItemRow(
                        title = "الرصيد",
                        subtitle = "شحن وسحب العملات ورصيد الهدايا والمكافآت",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = VyoraGold,
                        onClick = {
                            showMenuBottomSheet = false
                            showBalanceDialog = true
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    // 2. الدعم
                    MenuItemRow(
                        title = "الدعم",
                        subtitle = "مركز المساعدة والتواصل المباشر مع فريق الدعم",
                        icon = Icons.Default.HeadsetMic,
                        iconTint = VyoraCyan,
                        onClick = {
                            showMenuBottomSheet = false
                            showSupportDialog = true
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    // 3. الإعدادات
                    MenuItemRow(
                        title = "الإعدادات",
                        subtitle = "الخصوصية، الإشعارات، واللغة",
                        icon = Icons.Default.Settings,
                        iconTint = Color.LightGray,
                        onClick = {
                            showMenuBottomSheet = false
                            showSettingsDialog = true
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    // 4. تسجيل الخروج
                    MenuItemRow(
                        title = "تسجيل الخروج",
                        subtitle = "إنهاء جلسة الدخول الحالية بأمان",
                        icon = Icons.Default.ExitToApp,
                        iconTint = VyoraCrimson,
                        onClick = {
                            showMenuBottomSheet = false
                            showLogoutConfirmDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        // 4. Balance Dialog (الرصيد)
        if (showBalanceDialog) {
            AlertDialog(
                onDismissRequest = { showBalanceDialog = false },
                containerColor = VyoraDarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = VyoraGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("محفظتي ورصيد فيورا", color = VyoraWhite, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1E28))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الرصيد الحالي", color = Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("1,250 عملة 🪙", color = VyoraGold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                            }
                        }

                        Text("شحن العملات لشراء الهدايا ودعم البثوث المباشرة:", color = Color.LightGray, fontSize = 12.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("100 عملة", "500 عملة", "1000 عملة").forEach { pack ->
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "تمت محاكاة شحن $pack بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VyoraElevated),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                ) {
                                    Text(pack, fontSize = 10.sp, color = VyoraCyan)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showBalanceDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson)
                    ) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // 5. Support Dialog (الدعم)
        if (showSupportDialog) {
            AlertDialog(
                onDismissRequest = { showSupportDialog = false },
                containerColor = VyoraDarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.HeadsetMic, contentDescription = null, tint = VyoraCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مركز الدعم والمساعدة", color = VyoraWhite, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("نحن هنا لمساعدتك على مدار الساعة في منصة VYORA.", color = Color.LightGray, fontSize = 13.sp)
                        Text("• الدعم الفني: support@vyora.app", color = Color.White, fontSize = 12.sp)
                        Text("• البلاغات ومكافحة الانتهاكات: safety@vyora.app", color = Color.White, fontSize = 12.sp)
                        Text("• وقت الاستجابة المعتاد: أقل من 15 دقيقة", color = VyoraCyan, fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            Toast.makeText(context, "تم إرسال طلب الدعم بنجاح، سيتواصل معك فريقنا قريباً", Toast.LENGTH_SHORT).show()
                            showSupportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCyan)
                    ) {
                        Text("تواصل مع الدعم", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSupportDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // 6. Settings Dialog (الإعدادات)
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = VyoraDarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الإعدادات العامة", color = VyoraWhite, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("• اللغة: العربية (الافتراضية)", color = Color.White, fontSize = 13.sp)
                        Text("• الإشعارات: مفعلة لجميع الأصدقاء والمتابعين", color = Color.LightGray, fontSize = 12.sp)
                        Text("• جودة الفيديو: دقة تلقائية متكيفة عالية (HD)", color = Color.LightGray, fontSize = 12.sp)
                        Text("• إصدار التطبيق: VYORA v2.5.0 (Latest Release)", color = VyoraCyan, fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson)
                    ) {
                        Text("تم")
                    }
                }
            )
        }
    }
}

@Composable
private fun MenuItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VyoraWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatCounterItem(label: String, count: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            color = VyoraWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}
