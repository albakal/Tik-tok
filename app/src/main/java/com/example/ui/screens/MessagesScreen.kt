package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.VyoraCardBorder
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan
import com.example.ui.theme.VyoraDarkSurface
import com.example.ui.theme.VyoraElevated
import com.example.ui.theme.VyoraWhite

data class ConversationItem(
    val id: String,
    val senderName: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int
)

@Composable
fun MessagesScreen(
    currentUserProfile: UserProfile?,
    onRequestSignIn: () -> Unit
) {
    val conversations = remember {
        mutableStateListOf(
            ConversationItem("1", "فريق فيورا الرسمي", "أهلاً بك في منصة VYORA! استمتع بصناعة ومشاركة أروع الفيديوهات.", "الآن", 1),
            ConversationItem("2", "خالد المنصوري", "الفيديو الأخير كان خورافي يا بطل 🔥", "10 د", 2),
            ConversationItem("3", "مريم العلي", "ممكن نشترك معاً في تحدي قادم؟ 🎬", "1 س", 0),
            ConversationItem("4", "إدارة المنصة", "تم توثيق حسابك ومطابقة البيانات السحابية بنجاح.", "أمس", 0)
        )
    }

    var selectedChat by remember { mutableStateOf<ConversationItem?>(null) }
    var directMessageText by remember { mutableStateOf("") }
    val threadMessages = remember {
        mutableStateListOf(
            "مرحباً بك! يسعدنا تواصلك دائماً.",
            "كيف يمكننا مساعدتك اليوم في فيورا؟"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)
            .testTag("messages_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = VyoraCrimson,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "صندوق الرسائل والمحادثات",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "الإشعارات",
                    tint = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activities & Alerts Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(VyoraElevated)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VyoraCrimson),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "تنبيهات التفاعل والمتابعين",
                    color = VyoraWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "إعجابات جديدة وتعليقات على فيديوهاتك في قسم مجتمع",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "المحادثات المباشرة",
            color = Color.LightGray,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(conversations, key = { it.id }) { conv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(VyoraDarkSurface)
                        .border(1.dp, VyoraCardBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            if (currentUserProfile == null) {
                                onRequestSignIn()
                            } else {
                                selectedChat = conv
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(VyoraCrimson, VyoraCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conv.senderName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = conv.senderName,
                                color = VyoraWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = conv.time,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = conv.lastMessage,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    if (conv.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(VyoraCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${conv.unreadCount}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Direct Chat Dialog
        selectedChat?.let { chat ->
            AlertDialog(
                onDismissRequest = { selectedChat = null },
                containerColor = VyoraDarkSurface,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chat.senderName,
                            color = VyoraWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { selectedChat = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(threadMessages) { msg ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VyoraElevated)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = directMessageText,
                                onValueChange = { directMessageText = it },
                                placeholder = { Text("اكتب رسالة...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = VyoraCyan,
                                    unfocusedBorderColor = VyoraCardBorder
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (directMessageText.isNotBlank()) {
                                        threadMessages.add(directMessageText)
                                        directMessageText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(VyoraCrimson)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}
