package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.repository.VideoRepository
import com.example.ui.components.CommentBottomSheet
import com.example.ui.components.VideoOverlay
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraDarkSurface
import com.example.ui.theme.VyoraWhite
import kotlinx.coroutines.launch

enum class FeedTab {
    LIVE,
    COMMUNITY,
    FOLLOWING,
    FOR_YOU
}

@Composable
fun HomeScreen(
    videoRepository: VideoRepository,
    currentUserProfile: UserProfile?,
    onRequestSignIn: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToLive: () -> Unit,
    onNavigateToCreator: (authorId: String, authorName: String, authorPhotoUrl: String) -> Unit,
    onAdminModerateVideo: (Video) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(FeedTab.COMMUNITY) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var selectedVideoForComments by remember { mutableStateOf<Video?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val likedVideos = remember { mutableStateMapOf<String, Boolean>() }
    val savedVideos = remember { mutableStateMapOf<String, Boolean>() }
    var showBurstHeart by remember { mutableStateOf(false) }

    LaunchedEffect(currentTab) {
        when (currentTab) {
            FeedTab.COMMUNITY -> {
                videoRepository.observeCommunityVideos().collect {
                    videos = it
                }
            }
            FeedTab.FOR_YOU -> {
                videoRepository.observeForYouVideos().collect {
                    videos = it
                }
            }
            FeedTab.FOLLOWING -> {
                videoRepository.observeCommunityVideos().collect {
                    videos = it
                }
            }
            FeedTab.LIVE -> {
                onNavigateToLive()
            }
        }
    }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) videos
        else videos.filter {
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.authorName.contains(searchQuery, ignoreCase = true)
        }
    }

    val pagerState = rememberPagerState(pageCount = { filteredVideos.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("home_screen")
    ) {
        if (filteredVideos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Official VYORA Logo in empty state
                Image(
                    painter = painterResource(id = R.drawable.vyora_official_logo),
                    contentDescription = "VYORA Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(22.dp))
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "قسم مجتمع الفيديوهات في VYORA",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "كن أول من يصور وينشر فيديو حقيقي ليظهر لجميع المستخدمين على المنصة!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_create_video_button")
                ) {
                    Text("تصوير ونشر فيديو الآن (+)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Vertical Pager with full-bleed videos
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = filteredVideos[page]
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
                                    if (!current) "تم حفظ الفيديو في المفضلة" else "تم إلغاء الحفظ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShareClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "شاهد فيديو @${video.authorName} على منصة VYORA: ${video.videoUrl}")
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
                label = "heart_burst"
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

        // Top Navigation Bar: Official Logo, Live, Feed Tabs, Search & Quick Gmail Sign In
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Official VYORA Logo Icon in Top Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.vyora_official_logo),
                    contentDescription = "VYORA",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                IconButton(
                    onClick = onNavigateToLive,
                    modifier = Modifier.testTag("top_live_tab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = "لايف",
                        tint = if (currentTab == FeedTab.LIVE) VyoraCrimson else Color.White
                    )
                }
            }

            // Feed Tabs Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopFeedTabItem(
                    title = "مجتمع",
                    isSelected = currentTab == FeedTab.COMMUNITY,
                    onClick = { currentTab = FeedTab.COMMUNITY },
                    testTag = "feed_tab_community"
                )

                TopFeedTabItem(
                    title = "يتابع",
                    isSelected = currentTab == FeedTab.FOLLOWING,
                    onClick = { currentTab = FeedTab.FOLLOWING },
                    testTag = "feed_tab_following"
                )

                TopFeedTabItem(
                    title = "لك",
                    isSelected = currentTab == FeedTab.FOR_YOU,
                    onClick = { currentTab = FeedTab.FOR_YOU },
                    testTag = "feed_tab_foryou"
                )
            }

            // Right side: Search
            IconButton(
                onClick = { showSearchDialog = true },
                modifier = Modifier.testTag("top_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = Color.White
                )
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

        // Search Dialog
        if (showSearchDialog) {
            AlertDialog(
                onDismissRequest = { showSearchDialog = false },
                containerColor = VyoraDarkSurface,
                title = { Text("بحث في منصة VYORA", color = VyoraWhite, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن فيديو أو صانع محتوى...", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showSearchDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VyoraCrimson)
                    ) {
                        Text("بحث")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        searchQuery = ""
                        showSearchDialog = false
                    }) {
                        Text("مسح", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
private fun TopFeedTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = if (isSelected) 16.sp else 14.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.5.dp)
                .background(if (isSelected) Color.White else Color.Transparent)
        )
    }
}
