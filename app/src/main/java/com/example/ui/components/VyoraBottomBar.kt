package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VyoraCrimson
import com.example.ui.theme.VyoraCyan

enum class VyoraTab {
    HOME,
    FRIENDS,
    CREATE,
    MESSAGES,
    PROFILE
}

@Composable
fun VyoraBottomBar(
    currentTab: VyoraTab,
    onTabSelected: (VyoraTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(58.dp)
            .testTag("vyora_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. الرئيسية (Home)
            BottomNavItem(
                label = "الرئيسية",
                icon = if (currentTab == VyoraTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                isSelected = currentTab == VyoraTab.HOME,
                onClick = { onTabSelected(VyoraTab.HOME) },
                testTag = "nav_tab_home"
            )

            // 2. الأصدقاء (Friends)
            BottomNavItem(
                label = "الأصدقاء",
                icon = if (currentTab == VyoraTab.FRIENDS) Icons.Filled.PeopleOutline else Icons.Outlined.PeopleOutline,
                isSelected = currentTab == VyoraTab.FRIENDS,
                onClick = { onTabSelected(VyoraTab.FRIENDS) },
                testTag = "nav_tab_friends"
            )

            // 3. (+) الإضافة (Create)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onTabSelected(VyoraTab.CREATE) }
                    .testTag("nav_tab_create"),
                contentAlignment = Alignment.Center
            ) {
                // Background cyan & crimson split layers
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(VyoraCyan, VyoraCrimson)
                            )
                        )
                )
                // Center white button
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إنشاء فيديو",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 4. الرسائل (Messages)
            BottomNavItem(
                label = "الرسائل",
                icon = if (currentTab == VyoraTab.MESSAGES) Icons.Filled.ChatBubbleOutline else Icons.Outlined.ChatBubbleOutline,
                isSelected = currentTab == VyoraTab.MESSAGES,
                onClick = { onTabSelected(VyoraTab.MESSAGES) },
                testTag = "nav_tab_messages"
            )

            // 5. الشخصية (Profile)
            BottomNavItem(
                label = "الشخصية",
                icon = if (currentTab == VyoraTab.PROFILE) Icons.Filled.PersonOutline else Icons.Outlined.PersonOutline,
                isSelected = currentTab == VyoraTab.PROFILE,
                onClick = { onTabSelected(VyoraTab.PROFILE) },
                testTag = "nav_tab_profile"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
