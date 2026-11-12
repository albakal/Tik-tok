package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.util.Log
import com.example.R
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraSemiTransparent
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUrl: String,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit = {}
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }
    var isUserPaused by remember { mutableStateOf(false) }

    fun resolvePlayableUri(url: String): Uri {
        return try {
            when {
                url.isBlank() -> {
                    Uri.parse("android.resource://${context.packageName}/${R.raw.vyora_sample_clip}")
                }
                url.startsWith("android.resource://") -> {
                    Uri.parse(url)
                }
                url.startsWith("http://") || url.startsWith("https://") -> {
                    Uri.parse(url)
                }
                url.startsWith("/") || url.startsWith("file:") -> {
                    val filePath = if (url.startsWith("file://")) {
                        Uri.parse(url).path ?: url.removePrefix("file://")
                    } else {
                        url
                    }
                    val file = File(filePath)
                    if (file.exists() && file.isFile && file.length() > 0) {
                        Uri.fromFile(file)
                    } else {
                        Log.w("VideoPlayerView", "Local video file not found at '$filePath', using bundled sample clip fallback")
                        Uri.parse("android.resource://${context.packageName}/${R.raw.vyora_sample_clip}")
                    }
                }
                url.startsWith("content://") -> {
                    Uri.parse(url)
                }
                else -> {
                    Uri.parse("android.resource://${context.packageName}/${R.raw.vyora_sample_clip}")
                }
            }
        } catch (e: Exception) {
            Log.e("VideoPlayerView", "Error resolving URI for $url, fallback to raw clip", e)
            Uri.parse("android.resource://${context.packageName}/${R.raw.vyora_sample_clip}")
        }
    }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val uri = resolvePlayableUri(videoUrl)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("VideoPlayerView", "ExoPlayer playback error for $videoUrl: ${error.message}")
                isBuffering = false
                try {
                    val fallbackUri = Uri.parse("android.resource://${context.packageName}/${R.raw.vyora_sample_clip}")
                    val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
                    if (currentUri != fallbackUri) {
                        Log.i("VideoPlayerView", "Switching to fallback sample clip")
                        exoPlayer.setMediaItem(MediaItem.fromUri(fallbackUri))
                        exoPlayer.prepare()
                        if (isCurrentPage && !isUserPaused) {
                            exoPlayer.play()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoPlayerView", "Failed to switch to fallback clip", e)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isCurrentPage, isUserPaused) {
        if (isCurrentPage && !isUserPaused) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isUserPaused = !isUserPaused
                onSingleTap()
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering indicator
        if (isBuffering && isCurrentPage) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = VyoraCrimson,
                strokeWidth = 3.dp
            )
        }

        // Paused state overlay icon
        AnimatedVisibility(
            visible = isUserPaused,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(VyoraSemiTransparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
