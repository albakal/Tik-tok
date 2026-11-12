package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveStream
import com.example.data.model.Moderator
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.repository.AdminRepository
import com.example.data.repository.LiveRepository
import com.example.data.repository.VideoRepository
import com.example.ui.theme.VyoraCardBorder
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraDarkSurface
import com.example.ui.theme.VyoraElevated
import com.example.ui.theme.VyoraGold
import com.example.ui.theme.VyoraGreen
import com.example.ui.theme.VyoraWhite
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    adminRepository: AdminRepository,
    videoRepository: VideoRepository,
    liveRepository: LiveRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var liveStreams by remember { mutableStateOf<List<LiveStream>>(emptyList()) }
    var moderators by remember { mutableStateOf<List<Moderator>>(emptyList()) }

    // Dialog state for adding moderator
    var showAddModDialog by remember { mutableStateOf(false) }
    var modEmail by remember { mutableStateOf("") }
    val modPermDeleteVideo = remember { mutableStateOf(true) }
    val modPermBanUser = remember { mutableStateOf(false) }
    val modPermModerateLive = remember { mutableStateOf(true) }

    // Dialog state for setting video watermark
    var selectedVideoForWatermark by remember { mutableStateOf<Video?>(null) }
    var watermarkText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch { adminRepository.observeAllUsers().collect { users = it } }
        launch { videoRepository.observeAllVideosForAdmin().collect { videos = it } }
        launch { liveRepository.observeActiveLiveStreams().collect { liveStreams = it } }
        launch { adminRepository.observeModerators().collect { moderators = it } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("admin_dashboard_screen")
    ) {
        // Super Admin Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VyoraElevated)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = VyoraGold,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "لوحة تحكم المدير الرئيسي (Super Admin)",
                        color = VyoraGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "الحساب المعتمد: mahmoudomran324@gmail.com",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("admin_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = Color.White
                )
            }
        }

        // Dashboard Tabs
        val tabs = listOf(
            "نظرة عامة",
            "المستخدمون (${users.size})",
            "الفيديوهات (${videos.size})",
            "المشرفون (${moderators.size})",
            "البثوث (${liveStreams.size})"
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = Color.White,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = VyoraGold,
                    height = 2.5.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) VyoraGold else Color.Gray,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // 1. Overview Tab
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "إحصائيات المنصة الحية (Firebase Realtime)",
                            color = VyoraWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "إجمالي المستخدمين",
                                value = "${users.size}",
                                icon = Icons.Default.People,
                                color = VyoraCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "إجمالي الفيديوهات",
                                value = "${videos.size}",
                                icon = Icons.Default.Videocam,
                                color = VyoraCrimson,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "البثوث النشطة",
                                value = "${liveStreams.size}",
                                icon = Icons.Default.LiveTv,
                                color = VyoraGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "طاقم المشرفين",
                                value = "${moderators.size}",
                                icon = Icons.Default.AdminPanelSettings,
                                color = VyoraGold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(VyoraElevated)
                                .border(1.dp, VyoraCardBorder, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "صلاحيات المدير الرئيسي الكاملة",
                                    color = VyoraGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• إدارة المستخدمين: حظر وإلغاء الحظر وتعيين المشرفين\n" +
                                            "• إدارة المحتوى: حذف الفيديوهات وحظرها ووضع العلامات الرسمية\n" +
                                            "• إدارة البث المباشر: الإشراف وإنهاء البثوث المخالفة\n" +
                                            "• أمان السحابة: الصلاحيات مفعلة بقواعد Firestore الأمنية المشددة",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // 2. Users Management Tab
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(users, key = { it.uid }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VyoraDarkSurface)
                                    .border(1.dp, VyoraCardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName.ifBlank { "مستخدم فيورا" },
                                            color = VyoraWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (user.role == "super_admin") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "مدير رئيسي",
                                                color = VyoraGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = user.email,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                    if (user.isBanned) {
                                        Text(
                                            text = "محظور من المنصة ⛔",
                                            color = VyoraCrimson,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (user.role != "super_admin") {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                adminRepository.setUserBannedStatus(user.uid, !user.isBanned)
                                                Toast.makeText(
                                                    context,
                                                    if (!user.isBanned) "تم حظر المستخدم" else "تم إلغاء الحظر",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (user.isBanned) VyoraGreen else VyoraCrimson
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = if (user.isBanned) "إلغاء الحظر" else "حظر",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // 3. Videos Moderation Tab
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(videos, key = { it.id }) { video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VyoraDarkSurface)
                                    .border(1.dp, VyoraCardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.description.ifBlank { "بدون وصف" },
                                        color = VyoraWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "الناشر: @${video.authorName}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                    if (video.isBlocked) {
                                        Text(
                                            text = "محظور من العرض ⛔",
                                            color = VyoraCrimson,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (video.adminWatermark.isNotBlank()) {
                                        Text(
                                            text = "علامة الإدارة: ${video.adminWatermark}",
                                            color = VyoraGold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Watermark Button
                                    IconButton(
                                        onClick = {
                                            selectedVideoForWatermark = video
                                            watermarkText = video.adminWatermark
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "علامة مائية",
                                            tint = VyoraGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Block / Unblock Button
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                videoRepository.toggleBlockVideo(video.id, !video.isBlocked)
                                                Toast.makeText(
                                                    context,
                                                    if (!video.isBlocked) "تم حظر الفيديو" else "تم فك حظر الفيديو",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = "حظر",
                                            tint = if (video.isBlocked) VyoraGreen else VyoraGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Video Button
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                videoRepository.deleteVideo(video.id)
                                                Toast.makeText(context, "تم حذف الفيديو نهائياً", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = VyoraCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // 4. Moderators Management Tab
                    Column {
                        Button(
                            onClick = { showAddModDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VyoraGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("add_moderator_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة مشرف جديد وتحديد صلاحياته", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (moderators.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لم يتم تعيين أي مشرفين حتى الآن. يمكنك إضافة مشرف بالبريد الإلكتروني وتحديد صلاحياته.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(moderators, key = { it.id }) { mod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(VyoraDarkSurface)
                                            .border(1.dp, VyoraCardBorder, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mod.email,
                                                color = VyoraWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "الصلاحيات: ${mod.permissions.joinToString("، ")}",
                                                color = VyoraCyan,
                                                fontSize = 11.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    adminRepository.removeModerator(mod.id)
                                                    Toast.makeText(context, "تمت إزالة المشرف", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "إزالة المشرف",
                                                tint = VyoraCrimson
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // 5. Live Streams Moderation Tab
                    if (liveStreams.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد بثوث مباشرة نشطة حالياً للإشراف عليها.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(liveStreams, key = { it.id }) { stream ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VyoraDarkSurface)
                                        .border(1.dp, VyoraCardBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stream.title,
                                            color = VyoraWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "المذيع: @${stream.hostName} • المشاهدون: ${stream.viewerCount}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                liveRepository.endLiveStream(stream.id)
                                                Toast.makeText(context, "تم إنهاء البث فوراً كإجراء إداري", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("إنهاء البث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Moderator Dialog
        if (showAddModDialog) {
            AlertDialog(
                onDismissRequest = { showAddModDialog = false },
                containerColor = VyoraDarkSurface,
                title = { Text("إضافة مشرف جديد", color = VyoraWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = modEmail,
                            onValueChange = { modEmail = it },
                            label = { Text("البريد الإلكتروني للمشرف") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "تحديد اختصاصات وصلاحيات المشرف:",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = modPermDeleteVideo.value,
                                onCheckedChange = { modPermDeleteVideo.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = VyoraGold)
                            )
                            Text("حذف وحظر الفيديوهات المخالفة", color = Color.White, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = modPermBanUser.value,
                                onCheckedChange = { modPermBanUser.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = VyoraGold)
                            )
                            Text("حظر المستخدمين المتجاوزين", color = Color.White, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = modPermModerateLive.value,
                                onCheckedChange = { modPermModerateLive.value = it },
                                colors = CheckboxDefaults.colors(checkedColor = VyoraGold)
                            )
                            Text("مراقبة وإنهاء البث المباشر", color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (modEmail.isNotBlank()) {
                                val perms = mutableListOf<String>()
                                if (modPermDeleteVideo.value) perms.add("حذف الفيديوهات")
                                if (modPermBanUser.value) perms.add("حظر المستخدمين")
                                if (modPermModerateLive.value) perms.add("إدارة البث")

                                scope.launch {
                                    adminRepository.addModerator(modEmail.trim(), perms)
                                    showAddModDialog = false
                                    modEmail = ""
                                    Toast.makeText(context, "تمت إضافة المشرف بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraGold)
                    ) {
                        Text("إضافة", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddModDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // Add Watermark Dialog
        selectedVideoForWatermark?.let { video ->
            AlertDialog(
                onDismissRequest = { selectedVideoForWatermark = null },
                containerColor = VyoraDarkSurface,
                title = { Text("وضع علامة الإدارة على الفيديو", color = VyoraWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "اكتب النص أو العلامة الإدارية التي ستظهر على الفيديو لجميع المستخدمين:",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            placeholder = { Text("مثال: محتوى مميز موثق من الإدارة ⭐") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                videoRepository.setAdminWatermark(video.id, watermarkText.trim())
                                selectedVideoForWatermark = null
                                Toast.makeText(context, "تم تحديث علامة الإدارة على الفيديو", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraGold)
                    ) {
                        Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedVideoForWatermark = null }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(VyoraElevated)
            .border(1.dp, VyoraCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color.Gray, fontSize = 12.sp)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = VyoraWhite,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }
    }
}
