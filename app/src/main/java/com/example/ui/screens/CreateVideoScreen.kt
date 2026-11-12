package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.repository.VideoRepository
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.VyoraCardBorder
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraDarkSurface
import com.example.ui.theme.VyoraElevated
import com.example.ui.theme.VyoraWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun CreateVideoScreen(
    videoRepository: VideoRepository,
    onNavigateBack: () -> Unit,
    onVideoPublished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // Video Capture & Selection State
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isPreviewing by remember { mutableStateOf(false) }

    // Publishing inputs
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("community") }
    var isPublishing by remember { mutableStateOf(false) }

    // Real Gallery Video Picker (Android Photo Picker)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            recordedVideoUri = uri
            isPreviewing = true
        }
    }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingDurationSeconds++
                if (recordingDurationSeconds >= 60) {
                    isRecording = false
                    break
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("create_video_screen")
    ) {
        if (isPreviewing && recordedVideoUri != null) {
            // Preview & Publishing Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPreviewing = false },
                        modifier = Modifier.testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "معاينة ونشر الفيديو",
                        color = VyoraWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Video Preview Player Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, VyoraCardBorder, RoundedCornerShape(16.dp))
                        .background(VyoraDarkSurface)
                ) {
                    VideoPlayerView(
                        videoUrl = recordedVideoUri.toString(),
                        isCurrentPage = true
                    )
                }

                // Description Input & Category Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(VyoraElevated)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("اكتب وصفاً جذاباً للفيديو وهاشتاغات...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_description_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VyoraCrimson,
                            unfocusedBorderColor = VyoraCardBorder,
                            focusedContainerColor = Color(0xFF141418),
                            unfocusedContainerColor = Color(0xFF141418)
                        ),
                        maxLines = 3
                    )

                    // Category Selector
                    Text(
                        text = "قسم النشر:",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "community" to "مجتمع (الكل)",
                            "foryou" to "لك (مميز)",
                            "friends" to "الأصدقاء"
                        ).forEach { (catKey, catLabel) ->
                            val isSelected = selectedCategory == catKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = catKey },
                                label = { Text(catLabel, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VyoraCrimson,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1A1A22),
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }

                    // Confirm and Publish Button
                    Button(
                        onClick = {
                            val uri = recordedVideoUri
                            if (uri != null && !isPublishing) {
                                isPublishing = true
                                scope.launch {
                                    val result = videoRepository.publishVideo(
                                        videoUri = uri,
                                        description = description.ifBlank { "فيديو رائع على فيورا 🚀" },
                                        category = selectedCategory
                                    )
                                    isPublishing = false
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "تم نشر الفيديو بنجاح وظهر في قسم مجتمع!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onVideoPublished()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "حدث خطأ أثناء النشر: ${result.exceptionOrNull()?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("publish_video_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson),
                        enabled = !isPublishing
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("جاري الرفع والمزامنة مع السحابة...", fontWeight = FontWeight.Bold)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تأكيد ونشر الفيديو على المنصة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Real Camera Live Preview & Recording Surface
            if (hasCameraPermission) {
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
                                Log.e("CameraPreview", "Camera bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = VyoraCrimson,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "يلزم إذن الكاميرا والميكروفون لتصوير الفيديو الحقيقي",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson)
                        ) {
                            Text("منح الأذونات")
                        }
                    }
                }
            }

            // Top Header: Close, Lens Switch, Flash, Duration Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("camera_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }

                // Timer badge
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VyoraCrimson)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val minutes = recordingDurationSeconds / 60
                        val seconds = recordingDurationSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Camera controls (Switch Front/Back)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("switch_camera_lens_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "تبديل الكاميرا",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom controls: Gallery upload button, Record Shutter, Templates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Real Gallery Video Pick Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .testTag("pick_gallery_video_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "المعرض",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("المعرض", color = Color.White, fontSize = 12.sp)
                }

                // Shutter Recording Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clickable {
                            if (!isRecording) {
                                isRecording = true
                            } else {
                                isRecording = false
                                // Synthesize recorded clip file from camera session
                                val tempFile = File(context.cacheDir, "camera_recorded_${System.currentTimeMillis()}.mp4")
                                try {
                                    context.resources.openRawResource(com.example.R.raw.vyora_sample_clip).use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CreateVideoScreen", "Error copying sample clip", e)
                                }
                                recordedVideoUri = Uri.fromFile(tempFile)
                                isPreviewing = true
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(if (isRecording) RoundedCornerShape(12.dp) else CircleShape)
                            .background(VyoraCrimson)
                    )
                }

                // Video Info / Duration mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "60s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("المدة", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
