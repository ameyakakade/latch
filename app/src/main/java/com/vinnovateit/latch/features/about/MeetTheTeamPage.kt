package com.vinnovateit.latch.features.about

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.util.TooltipHint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration

// Data class for team members
data class TeamMember(
    val name: String,
    val role: String,
    val imageRes: Int,
    val githubUrl: String,
    val linkedinUrl: String
)

// List of 12 team members
val teamMembers = listOf(
    TeamMember("SOUMOJIT GANGULY", "Project Manager", R.drawable.syro, "https://github.com/soumojit2004", "https://linkedin.com/in/soumojit-ganguly"),
    TeamMember("AYUSH KUMAR", "Tech Head", R.drawable.ayush1, "https://github.com/AyushK0808", "https://linkedin.com/in/ayush-kumar-061a58251"),
    TeamMember("AYUSH KUMAR", "Projects Head", R.drawable.ayush2, "https://github.com/thecoder-001", "https://linkedin.com/in/ayush-kumar-cs"),
    TeamMember("MIHIR JOSHI", "Creative Head", R.drawable.mihir, "https://github.com/J-Mihir", "https://linkedin.com/in/mihir-shekhar-joshi"),
    TeamMember("LAKSHYA GUPTA", "Developer", R.drawable.lakshya, "https://github.com/2005lakshya", "https://linkedin.com/in/lakshya-gupta2005"),
    TeamMember("SARTHAK MIGLANI", "Developer", R.drawable.sarthak, "https://github.com/SarthakMiglani", "https://www.linkedin.com/in/sarthak--miglani"),
    TeamMember("TANMOY SAHA", "Developer", R.drawable.tanmoy, "https://github.com/TSaha4", "https://linkedin.com/in/tanmoy-saha-4b0ab228a"),
    TeamMember("LAVAN", "Developer", R.drawable.lavan, "https://github.com/lavan8t", "https://linkedin.com/in/lavan8t"),
    TeamMember("VIVEK VATTEM", "Designer", R.drawable.vivek, "https://github.com/vivekvattem", "https://www.linkedin.com/in/vivek-vattem-3102662a8"),
    TeamMember("ARYAMAN BHATIA", "Designer", R.drawable.aryaman, "https://github.com/aryamanbhatia1", "https://linkedin.com/in/aryaman-bhatia-97b99b256"),
    TeamMember("ARCHIT NIGAM", "Designer", R.drawable.archit, "https://github.com/architnigam", "https://linkedin.com/in/archit-nigam-a18895314"),
    TeamMember("KRISH MEHTA", "Project Manager", R.drawable.krish, "https://github.com/krxsh007", "http://www.linkedin.com/in/krishmmehta-0t")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetTheTeamPage(onBackClick: () -> Unit) {
    BackHandler {
        onBackClick() // Navigate back to the previous screen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp)) // just a small gap below back button

            // Vinnovate Logo
            val context = LocalContext.current // Retrieve context once
            Image(
                painter = painterResource(id = R.drawable.vinnovate),
                contentDescription = "Vinnovate Logo",
                modifier = Modifier
                    .size(180.dp) // Adjust size as needed
                    .offset(y = (-8).dp) // Move the logo slightly upward
                    .clickable(
                        indication = null, // Removes the grey highlight effect
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vinnovateit.com"))
                        context.startActivity(intent)
                    },
                contentScale = ContentScale.Fit
            )

            // Social Media Logos
            Row(
                horizontalArrangement = Arrangement.spacedBy(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // LinkedIn Logo
                val context = LocalContext.current // Retrieve context once
                Image(
                    painter = painterResource(id = R.drawable.linkedin),
                    contentDescription = "LinkedIn",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-30).dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/company/v-innovate-it/"))
                            context.startActivity(intent)
                        }
                )

                // GitHub Logo
                Image(
                    painter = painterResource(id = R.drawable.github),
                    contentDescription = "GitHub",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-30).dp)
                        .clickable(
                            indication = null, // Removes the grey highlight effect
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/vinnovateit"))
                            context.startActivity(intent)
                        }
                )

                // Instagram Logo
                Image(
                    painter = painterResource(id = R.drawable.instagram),
                    contentDescription = "Instagram",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-30).dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/vinnovateit/"))
                            context.startActivity(intent)
                        }
                )
            }

            // Meet The Team Heading
            Text(
                text = "Meet The Team",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily(Font(R.font.outfit_variable))
                ),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC01221),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Underline
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(3.dp)
                    .background(Color(0xFFC01221))
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Team Member Cards
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                teamMembers.chunked(2).forEach { rowMembers ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(25.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowMembers.forEach { member ->
                            TeamMemberCard(
                                teamMember = member,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowMembers.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Floating circular back button (overlayed)
        TooltipHint(tooltipText = "Back") {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(top = 40.dp, start = 6.dp) // Increased top padding to move the button further down
                    .size(50.dp) // Increased size of the button
                    .background(
                        color = MaterialTheme.colorScheme.surface, // same as background
                        shape = CircleShape
                    )
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary // Match the tint color from SettingsActivity
                )
            }
        }
    }
}


@Composable
fun TeamMemberCard(
    teamMember: TeamMember,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.graphicsLayer(
            shadowElevation = 20f,
            clip = true
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFFF1500)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Profile Image (circle)
            Image(
                painter = painterResource(id = teamMember.imageRes),
                contentDescription = "Profile picture of ${teamMember.name}",
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFF1500),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val dynamicFontSize = if (screenWidth < 360.dp) 8.sp else 10.sp // Adjust font size based on screen width

            Text(
                text = teamMember.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily(Font(R.font.moderniz)),
                    fontSize = dynamicFontSize
                ),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC01221),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Role
            Text(
                text = teamMember.role,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily(Font(R.font.satoshi_regular)),
                    fontSize = 11.sp
                ),
                fontWeight = FontWeight.Normal,
                color = Color(0xFFC01221),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Socials
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(teamMember.githubUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.github),
                        contentDescription = "GitHub of ${teamMember.name}",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(teamMember.linkedinUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.linkedin),
                        contentDescription = "LinkedIn of ${teamMember.name}",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
