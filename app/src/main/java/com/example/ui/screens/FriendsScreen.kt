package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.components.CommentBottomSheet
import com.example.ui.components.VideoOverlay
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraWhite
import kotlinx.coroutines.launch

data class SuggestedCreator(
    val id: String,
    val name: String,
    val handle: String,
    val photoUrl: String = ""
)

@Composable
fun FriendsScreen(
    videoRepository: VideoRepository,
    currentUserProfile: UserProfile?,
    onRequestSignIn: () -> Unit,
    onNavigateToCreator: (authorId: String, authorName: String, authorPhotoUrl: String) -> Unit,
    onAdminModerateVideo: (Video) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var friendsVideos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var selectedVideoForComments by remember { mutableStateOf<Video?>(null) }

    val likedVideos = remember { mutableStateMapOf<String, Boolean>() }
    val savedVideos = remember { mutableStateMapOf<String, Boolean>() }
    val followedCreators = remember { mutableStateMapOf<String, Boolean>() }
    var showSuggestedBar by remember { mutableStateOf(true) }
    var showBurstHeart by remember { mutableStateOf(false) }

    val suggestedCreators = remember {
        listOf(
            SuggestedCreator("creator_1", "أحمد الشمري", "@ahmed_sh"),
            SuggestedCreator("creator_2", "سارة القحطاني", "@sara_q"),
            SuggestedCreator("creator_3", "عمر فاروق", "@omar_vlogs"),
            SuggestedCreator("creator_4", "نورا عبد الله", "@noura_tech"),
            SuggestedCreator("creator_5", "خالد العتيبي", "@khaled_live")
        )
    }

    LaunchedEffect(Unit) {
        videoRepository.observeCommunityVideos().collect {
            friendsVideos = it
        }
    }

    val pagerState = rememberPagerState(pageCount = { friendsVideos.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("friends_screen")
    ) {
        if (friendsVideos.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(VyoraCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = VyoraCyan,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "فيديوهات الأصدقاء",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "تابع أصدقاءك وصناع المحتوى المفضلين لتظهر فيديوهاتهم هنا بنظام ملء الشاشة!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Fullscreen Vertical Pager for Friends Clips (replacing box cards)
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = friendsVideos[page]
                val isLiked = likedVideos[video.id] ?: false
                val isSaved = savedVideos[video.id] ?: false
                val isSuperAdmin = currentUserProfile?.role == "super_admin"

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(video.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (!isLiked) {
                                        likedVideos[video.id] = true
                                        scope.launch { videoRepository.likeVideo(video.id) }
                                    }
                                    showBurstHeart = true
                                }
                            )
                        }
                ) {
                    VideoPlayerView(
                        videoUrl = video.videoUrl,
                        isCurrentPage = (pagerState.currentPage == page)
                    )

                    VideoOverlay(
                        video = video,
                        isLiked = isLiked,
                        isSaved = isSaved,
                        isSuperAdmin = isSuperAdmin,
                        onLikeClick = {
                            if (currentUserProfile == null) {
                                onRequestSignIn()
                            } else {
                                val current = likedVideos[video.id] ?: false
                                likedVideos[video.id] = !current
                                scope.launch { videoRepository.likeVideo(video.id) }
                            }
                        },
                        onCommentClick = {
                            selectedVideoForComments = video
                        },
                        onSaveClick = {
                            if (currentUserProfile == null) {
                                onRequestSignIn()
                            } else {
                                val current = savedVideos[video.id] ?: false
                                savedVideos[video.id] = !current
                                Toast.makeText(
                                    context,
                                    if (!current) "تم حفظ الفيديو" else "تم إلغاء الحفظ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShareClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "شاهد فيديو لصديقي على VYORA: ${video.videoUrl}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
                        },
                        onAuthorClick = {
                            onNavigateToCreator(video.authorId, video.authorName, video.authorPhotoUrl)
                        },
                        onAdminActionClick = {
                            onAdminModerateVideo(video)
                        }
                    )
                }
            }
        }

        // Animated double-tap heart burst
        if (showBurstHeart) {
            val heartScale by animateFloatAsState(
                targetValue = if (showBurstHeart) 1.4f else 0.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                finishedListener = { showBurstHeart = false },
                label = "heart_burst_friends"
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = VyoraCrimson.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(100.dp)
                        .scale(heartScale)
                )
            }
        }

        // Top Suggested Creators Carousel
        if (showSuggestedBar) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = VyoraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اقتراحات لمتابعة أصدقاء جدد",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { showSuggestedBar = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(suggestedCreators, key = { it.id }) { creator ->
                        val isFollowed = followedCreators[creator.id] ?: false
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(74.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    onNavigateToCreator(creator.id, creator.name, creator.photoUrl)
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, VyoraCyan, CircleShape)
                                    .background(VyoraCrimson.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = creator.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = creator.name,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFollowed) Color(0xFF2A2A35) else VyoraCrimson)
                                    .clickable {
                                        if (currentUserProfile == null) {
                                            onRequestSignIn()
                                        } else {
                                            val nextState = !isFollowed
                                            followedCreators[creator.id] = nextState
                                            Toast.makeText(
                                                context,
                                                if (nextState) "تمت متابعة ${creator.name}" else "تم إلغاء المتابعة",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isFollowed) "متابع" else "+ متابعة",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Comments Bottom Sheet
        selectedVideoForComments?.let { video ->
            CommentBottomSheet(
                videoId = video.id,
                videoRepository = videoRepository,
                onDismiss = { selectedVideoForComments = null },
                onRequestSignIn = onRequestSignIn
            )
        }
    }
}
