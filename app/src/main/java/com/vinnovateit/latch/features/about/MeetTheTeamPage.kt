package com.vinnovateit.latch.features.about

import android.content.Intent
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.vinnovateit.latch.R
import com.vinnovateit.latch.common.util.TooltipHint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.core.net.toUri

data class TeamMember(
    val name: String,
    val role: String,
    val imageRes: Int,
    val githubUrl: String,
    val linkedinUrl: String
)

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

    Scaffold(
        topBar = {
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                TooltipHint(tooltipText = "Back") {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 5.dp)

                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .height(70.dp) // your cropped height
                    .width(220.dp)  // keep width
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_vinnovateit),
                    contentDescription = "Vinnovate Logo",
                    modifier = Modifier
                        .size(220.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, "https://vinnovateit.com".toUri())
                            context.startActivity(intent)
                        },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(60.dp))
            // Social Media Logos
            Row(
                horizontalArrangement = Arrangement.spacedBy(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // LinkedIn Logo
                Image(
                    painter = painterResource(id = R.drawable.linkedin),
                    contentDescription = "LinkedIn",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-30).dp)
                        .clickable(
                            indication = null, // Removes ripple effect
                            interactionSource = remember { MutableInteractionSource() } // Removes ripple effect
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://www.linkedin.com/company/v-innovate-it/".toUri())
                            context.startActivity(intent)
                        },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Corrected dynamic color
                )

                // GitHub Logo
                Image(
                    painter = painterResource(id = R.drawable.github),
                    contentDescription = "GitHub",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-30).dp)
                        .clickable(
                            indication = null, // Removes ripple effect
                            interactionSource = remember { MutableInteractionSource() } // Removes ripple effect
                        ){
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/vinnovateit".toUri())
                            context.startActivity(intent)
                        },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Corrected dynamic color
                )

                // Instagram Logo
                Image(
                    painter = painterResource(id = R.drawable.instagram),
                    contentDescription = "Instagram",
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-30).dp)
                        .clickable(
                            indication = null, // Removes ripple effect
                            interactionSource = remember { MutableInteractionSource() } // Removes ripple effect
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://www.instagram.com/vinnovateit/".toUri())
                            context.startActivity(intent)
                        },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Corrected dynamic color
                )
            }

            // Meet The Team Heading
            Text(
                text = "Meet The Team",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily(Font(R.font.outfit_variable))
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, // Changed to dynamic theme color
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Underline
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary) // Removed rounded clipping
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Team Member Cards
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            Column(
                modifier = Modifier
                    .padding(horizontal = if (isLandscape) 32.dp else 16.dp), // Adjust padding for landscape
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 20.dp) // Adjust spacing for landscape
            ) {
                teamMembers.chunked(if (isLandscape) 3 else 2).forEach { rowMembers -> // Adjust chunk size for landscape
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 15.dp else 25.dp), // Adjust spacing for landscape
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowMembers.forEach { member ->
                            TeamMemberCard(
                                teamMember = member,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowMembers.size < (if (isLandscape) 3 else 2)) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp)) // Added empty space at the bottom after the last team member
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
        shape = RoundedCornerShape(0.dp), // Set to no shape for rectangle
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary // Dynamic color for border
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
                    .size(160.dp) // Removed rounded border
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary, // Dynamic color for border
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp

            Text(
                text = teamMember.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily(Font(R.font.moderniz)),
                    fontSize = 10.sp // Reduced font size
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, // Dynamic color
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Role
            Text(
                text = teamMember.role,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily(Font(R.font.satoshi_regular))
                ),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary, // Dynamic color
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Socials
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), // Centered with spacing
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, // Disable ripple effect
                            indication = null
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, teamMember.githubUrl.toUri())
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.github),
                        contentDescription = "GitHub of ${teamMember.name}",
                        modifier = Modifier.size(25.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Dynamic color
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, // Disable ripple effect
                            indication = null
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, teamMember.linkedinUrl.toUri())
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.linkedin),
                        contentDescription = "LinkedIn of ${teamMember.name}",
                        modifier = Modifier.size(25.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Dynamic color
                    )
                }
            }
        }
    }
}
