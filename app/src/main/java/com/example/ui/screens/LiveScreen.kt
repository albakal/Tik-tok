package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.LiveStream
import com.example.data.model.UserProfile
import com.example.data.repository.LiveRepository
import com.example.ui.theme.VyoraCardBorder
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraDarkSurface
import com.example.ui.theme.VyoraElevated
import com.example.ui.theme.VyoraGold
import com.example.ui.theme.VyoraGreen
import com.example.ui.theme.VyoraWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveScreen(
    liveRepository: LiveRepository,
    currentUserProfile: UserProfile?,
    onRequestSignIn: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var activeStreams by remember { mutableStateOf<List<LiveStream>>(emptyList()) }
    var isBroadcasting by remember { mutableStateOf(false) }
    var currentBroadcastStream by remember { mutableStateOf<LiveStream?>(null) }
    var selectedStreamToWatch by remember { mutableStateOf<LiveStream?>(null) }

    // Live Room State
    var broadcastTitle by remember { mutableStateOf("") }
    var isStartingBroadcast by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    // Floating Live Chat
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage("1", "user1", "أحمد الشمري", "منور يا غالي! 🔥"),
            ChatMessage("2", "user2", "سارة", "تحياتنا لك من الرياض 🇸🇦"),
            ChatMessage("3", "user3", "عمر الفهد", "بث فخم جداً ❤️")
        )
    }
    var newCommentText by remember { mutableStateOf("") }
    var heartsCount by remember { mutableIntStateOf(142) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        liveRepository.observeActiveLiveStreams().collect {
            activeStreams = it
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("live_screen")
    ) {
        if (isBroadcasting) {
            // Live Broadcaster Room with CameraX preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            Log.e("LiveCamera", "Camera error", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Live Broadcaster UI Overlay
            LiveRoomOverlay(
                title = currentBroadcastStream?.title ?: "بث مباشر حي",
                hostName = currentUserProfile?.displayName ?: "المذيع",
                hostAvatar = currentUserProfile?.photoUrl.orEmpty(),
                viewerCount = currentBroadcastStream?.viewerCount ?: 124L,
                heartsCount = heartsCount,
                chatMessages = chatMessages,
                newCommentText = newCommentText,
                onCommentChange = { newCommentText = it },
                onSendComment = {
                    if (newCommentText.isNotBlank()) {
                        chatMessages.add(
                            ChatMessage(
                                id = "${System.currentTimeMillis()}",
                                senderId = currentUserProfile?.uid.orEmpty(),
                                senderName = currentUserProfile?.displayName ?: "أنا",
                                text = newCommentText
                            )
                        )
                        newCommentText = ""
                    }
                },
                onHeartClick = { heartsCount += 1 },
                onSwitchCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                },
                onClose = {
                    currentBroadcastStream?.let { stream ->
                        scope.launch {
                            liveRepository.endLiveStream(stream.id)
                        }
                    }
                    isBroadcasting = false
                }
            )
        } else if (selectedStreamToWatch != null) {
            // Live Viewer Room
            val stream = selectedStreamToWatch!!
            Box(modifier = Modifier.fillMaxSize()) {
                // Background simulated video / camera streamer visual
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E112A), Color(0xFF0F0B18), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .border(3.dp, VyoraCrimson, CircleShape)
                        ) {
                            AsyncImage(
                                model = stream.hostAvatar.ifBlank { "https://picsum.photos/200" },
                                contentDescription = stream.hostName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stream.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "بث مباشر للمبدع @${stream.hostName}",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }

                LiveRoomOverlay(
                    title = stream.title,
                    hostName = stream.hostName,
                    hostAvatar = stream.hostAvatar,
                    viewerCount = stream.viewerCount + 1,
                    heartsCount = heartsCount,
                    chatMessages = chatMessages,
                    newCommentText = newCommentText,
                    onCommentChange = { newCommentText = it },
                    onSendComment = {
                        if (currentUserProfile == null) {
                            onRequestSignIn()
                        } else if (newCommentText.isNotBlank()) {
                            chatMessages.add(
                                ChatMessage(
                                    id = "${System.currentTimeMillis()}",
                                    senderId = currentUserProfile.uid,
                                    senderName = currentUserProfile.displayName,
                                    text = newCommentText
                                )
                            )
                            newCommentText = ""
                        }
                    },
                    onHeartClick = { heartsCount += 1 },
                    onSwitchCamera = null,
                    onClose = { selectedStreamToWatch = null }
                )
            }
        } else {
            // Main Live Discovery Hub
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("live_hub_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = VyoraCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بثوث VYORA المباشرة",
                            color = VyoraWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Live Broadcast Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(VyoraCrimson.copy(alpha = 0.9f), Color(0xFF880E4F))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ابدأ بثك المباشر الحقيقي الآن",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تواصل مع متابعيك مباشرة عبر كاميرا الهاتف، وتلقى التعليقات والتفاعلات الحية لحظة بلحظة.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            placeholder = { Text("عنوان البث المباشر...", color = Color.White.copy(alpha = 0.7f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_broadcast_title_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (currentUserProfile == null) {
                                    onRequestSignIn()
                                } else if (!hasCameraPermission) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    isStartingBroadcast = true
                                    scope.launch {
                                        val title = broadcastTitle.ifBlank { "بث مباشر مع ${currentUserProfile.displayName}" }
                                        val result = liveRepository.startLiveStream(title)
                                        isStartingBroadcast = false
                                        if (result.isSuccess) {
                                            currentBroadcastStream = result.getOrNull()
                                            isBroadcasting = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "فشل بدء البث: ${result.exceptionOrNull()?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_live_broadcast_button"),
                            enabled = !isStartingBroadcast
                        ) {
                            if (isStartingBroadcast) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = VyoraCrimson
                                )
                            } else {
                                Text(
                                    text = "بدء البث المباشر (LIVE)",
                                    color = VyoraCrimson,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Active Live Streams List
                Text(
                    text = "البثوث المباشرة النشطة الآن",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (activeStreams.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لا توجد بثوث نشطة حالياً. كن أول من يفتح بثاً مباشراً!",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeStreams, key = { it.id }) { stream ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(VyoraElevated)
                                    .clickable { selectedStreamToWatch = stream }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, VyoraCrimson, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = stream.hostAvatar.ifBlank { "https://picsum.photos/200" },
                                        contentDescription = stream.hostName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stream.title,
                                        color = VyoraWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "@${stream.hostName}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }

                                // LIVE badge
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VyoraCrimson)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stream.viewerCount}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRoomOverlay(
    title: String,
    hostName: String,
    hostAvatar: String,
    viewerCount: Long,
    heartsCount: Int,
    chatMessages: List<ChatMessage>,
    newCommentText: String,
    onCommentChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onHeartClick: () -> Unit,
    onSwitchCamera: (() -> Unit)?,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Broadcaster info pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = hostAvatar.ifBlank { "https://picsum.photos/200" },
                        contentDescription = hostName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = hostName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$viewerCount مشاهد",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSwitchCamera != null) {
                    IconButton(
                        onClick = onSwitchCamera,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "تبديل الكاميرا",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إنهاء",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Live Chat Stream at Bottom Left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.8f)
                .padding(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chatMessages.takeLast(5).forEach { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row {
                        Text(
                            text = "${msg.senderName}: ",
                            color = VyoraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = msg.text,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Bottom Input and Heart Reactions
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = onCommentChange,
                placeholder = { Text("أرسل رسالة للبث...", color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("live_chat_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VyoraCyan,
                    unfocusedBorderColor = VyoraCardBorder,
                    focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendComment,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VyoraCyan)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Floating Heart Button
            IconButton(
                onClick = onHeartClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VyoraCrimson)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "إعجاب",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
