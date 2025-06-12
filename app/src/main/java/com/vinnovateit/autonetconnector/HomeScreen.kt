@file:OptIn(ExperimentalMaterial3Api::class)


package com.vinnovateit.autonetconnector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    isConnected: Boolean = false,
    onPowerClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.home_page),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Sidebar with icons
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            IconButton(
                onClick = onHelpClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = "Help",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOGO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "VinnovateIT",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Power button
            Button(
                onClick = onPowerClick,
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFF8BC34A) else Color(0xFF7CB342)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Power,
                    contentDescription = "Power",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status text
            Text(
                text = if (isConnected) "You're Online" else "You're Offline",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Download
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "DOWNLOAD",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(
                        progress = 91f / 100f, // 91 Mbps
                        modifier = Modifier.size(80.dp),
                        color = Color(0xFF2196F3),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFF333333)
                    )

                    Text(
                        text = "91Mbps",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Upload
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = "Upload",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "UPLOAD",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(
                        progress = 50f / 100f, // 50 Mbps
                        modifier = Modifier.size(80.dp),
                        color = Color(0xFF9C27B0),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFF333333)
                    )

                    Text(
                        text = "50Mbps",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Data Usage
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier.size(20.dp)
                        ) {
                            drawCircle(
                                color = Color(0xFFF44336),
                                radius = size.minDimension / 4,
                                center = center
                            )
                        }
                        Text(
                            text = "DATA USAGE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(
                        progress = 50f / 100f, // 50GB
                        modifier = Modifier.size(80.dp),
                        color = Color(0xFFF44336),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFF333333)
                    )

                    Text(
                        text = "50gb",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Menu dots
            Text(
                text = "•••",
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Charts placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Charts Placeholder",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        isConnected = false,
        onPowerClick = { },
        onHelpClick = { },
        onProfileClick = { },
        onSettingsClick = { }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenOnlinePreview() {
    HomeScreen(
        isConnected = true,
        onPowerClick = { },
        onHelpClick = { },
        onProfileClick = { },
        onSettingsClick = { }
    )
}