package com.vinnovateit.latch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.core.platform.PlatformServices
import com.vinnovateit.latch.desktop.VinnovateItLogo
import com.vinnovateit.latch.ui.components.LatchDetailHeader
import com.vinnovateit.latch.ui.components.LatchIcons

private val ContentMaxWidth = 720.dp

private const val VINNOVATEIT_BLURB = "VinnovateIT is a community of builders and innovators " +
    "exploring technology beyond classrooms, through hands-on learning and real problem " +
    "solving. We don't just learn tech, we build with it. Because real innovation begins " +
    "when ideas meet execution.\n\nSome of our major projects include Messit, BunkBuddies, " +
    "StudyHub, etc."

private const val GITHUB_REPO_URL = "https://github.com/vinnovateit/auto-net-connector"
private const val INSTAGRAM_URL = "https://www.instagram.com/vinnovateit/"
private const val LINKEDIN_URL = "https://www.linkedin.com/company/v-innovate-it/"
private const val GITHUB_ORG_URL = "https://github.com/vinnovateit"

/**
 * "About Latch" -- reachable from the home screen's overflow menu. Takes over
 * the full window rather than living inside the nav rail: it is reference
 * material, not a destination someone flips back to, matching how the
 * credentials screen already takes over the window in [com.vinnovateit.latch.ui.LatchRoot].
 */
@Composable
fun AboutScreen(
    platform: PlatformServices,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LatchDetailHeader(title = "Latch", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                AboutCard(
                    icon = LatchIcons.Wifi,
                    title = "About Latch",
                ) {
                    Text(
                        text = "Latch is an auto-login utility built for the VIT hostel " +
                            "Wi-Fi network. It detects the captive portal, signs you in " +
                            "automatically with your saved credentials, and keeps your " +
                            "session alive in the background -- so you never have to open " +
                            "a browser just to get online.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AboutCard(
                    icon = LatchIcons.Lightbulb,
                    title = "About VinnovateIT",
                ) {
                    Text(
                        text = VINNOVATEIT_BLURB,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ContributeSection(onContribute = { platform.systemActions.openUrl(GITHUB_REPO_URL) })

                SocialLinksRow(
                    onOpenInstagram = { platform.systemActions.openUrl(INSTAGRAM_URL) },
                    onOpenLinkedIn = { platform.systemActions.openUrl(LINKEDIN_URL) },
                    onOpenGitHub = { platform.systemActions.openUrl(GITHUB_ORG_URL) },
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = VinnovateItLogo,
                        contentDescription = "VinnovateIT",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(160.dp)
                            .height(56.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Latch by VinnovateIT",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Version ${platform.buildInfo.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * A rounded card with a circled icon + title header, matching [SettingsSection]'s
 * card language rather than [HowItWorksDialog]'s dialog-row one -- this screen is
 * a full page of reference material, not a transient prompt.
 */
@Composable
private fun AboutCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * Mirrors the Android app's `ContributingSection` in MeetTheTeamPage.kt --
 * same copy, same GitHub-outlined-button treatment, same repo URL.
 */
@Composable
private fun ContributeSection(onContribute: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Contribute",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "We welcome contributions. Visit our GitHub repository to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onContribute) {
            Icon(
                imageVector = LatchIcons.GitHub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Contribute on GitHub")
        }
    }
}

@Composable
private fun SocialLinksRow(
    onOpenInstagram: () -> Unit,
    onOpenLinkedIn: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        SocialIconButton(icon = LatchIcons.Instagram, contentDescription = "Instagram", onClick = onOpenInstagram)
        Spacer(Modifier.width(24.dp))
        SocialIconButton(icon = LatchIcons.LinkedIn, contentDescription = "LinkedIn", onClick = onOpenLinkedIn)
        Spacer(Modifier.width(24.dp))
        SocialIconButton(icon = LatchIcons.GitHub, contentDescription = "GitHub", onClick = onOpenGitHub)
    }
}

@Composable
private fun SocialIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}
