package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Video
import com.example.ui.theme.VyoraCardBorder
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraGold
import com.example.ui.theme.VyoraWhite

@Composable
fun VideoOverlay(
    video: Video,
    isLiked: Boolean,
    isSaved: Boolean,
    isSuperAdmin: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onAdminActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFollowed by remember { mutableStateOf(false) }

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_spin"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
    ) {
        // Gradient shadow at bottom for text contrast
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Left/Right side action tools (in RTL: on the left side of screen)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Author Avatar with (+) follow button
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { onAuthorClick() }
                        .testTag("author_avatar_${video.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (video.authorPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = video.authorPhotoUrl,
                            contentDescription = video.authorName,
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
                                text = video.authorName.take(1).ifBlank { "V" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                if (!isFollowed) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(VyoraCrimson)
                            .clickable { isFollowed = true }
                            .testTag("follow_author_${video.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "متابعة",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Like (Heart) Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("like_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "إعجاب",
                        tint = if (isLiked) VyoraCrimson else Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = "${video.likesCount + if (isLiked) 1 else 0}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comment Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("comment_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "تعليقات",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "${video.commentsCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Save / Bookmark Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("save_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "حفظ",
                        tint = if (isSaved) VyoraGold else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = if (isSaved) "محفوظ" else "حفظ",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Share Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("share_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "${video.sharesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Super Admin Moderation Button
            if (isSuperAdmin) {
                IconButton(
                    onClick = onAdminActionClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VyoraGold.copy(alpha = 0.2f))
                        .testTag("admin_mod_video_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "إدارة الفيديو",
                        tint = VyoraGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Rotating Sound Disc
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E24))
                    .border(1.dp, Color.Gray, CircleShape)
                    .rotate(rotation),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = VyoraCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Content (Author handle, description, tags, admin watermark)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Author handle row with verified badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAuthorClick() }
            ) {
                Text(
                    text = "@${video.authorName.ifBlank { "مستخدم فيورا" }}",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "موثق",
                    tint = VyoraCyan,
                    modifier = Modifier.size(16.dp)
                )

                // Category tag
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VyoraCrimson.copy(alpha = 0.3f))
                        .border(0.5.dp, VyoraCrimson, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val catLabel = when (video.category) {
                        "community" -> "مجتمع"
                        "foryou" -> "لك"
                        "friends" -> "الأصدقاء"
                        else -> "عام"
                    }
                    Text(
                        text = catLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description / Caption
            if (video.description.isNotBlank()) {
                Text(
                    text = video.description,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Admin watermark / note if set
            if (video.adminWatermark.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VyoraGold.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = VyoraGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "إشراف الإدارة: ${video.adminWatermark}",
                        color = VyoraGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Sound Wave banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "الصوت الأصلي - ${video.authorName}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
