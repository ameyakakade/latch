package com.vinnovateit.latch.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinnovateit.latch.core.platform.PlatformServices
import com.vinnovateit.latch.core.updater.UpdateState
import com.vinnovateit.latch.desktop.LatchIcon
import com.vinnovateit.latch.desktop.LatchMark
import com.vinnovateit.latch.desktop.VinnovateItLogo
import com.vinnovateit.latch.ui.components.LatchDetailHeader
import com.vinnovateit.latch.ui.components.LatchIcons
import com.vinnovateit.latch.ui.theme.satoshiFontFamily

private val ContentMaxWidth = 720.dp

private const val LATCH_BLURB = "Latch is an auto-login utility built for the VIT hostel " +
    "Wi-Fi network. It detects the captive portal, signs you in " +
    "automatically with your saved credentials, and keeps your " +
    "session alive in the background — so you never have to open " +
    "a browser just to get online."

private const val VINNOVATEIT_BLURB = "VinnovateIT is a community of builders and innovators " +
    "exploring technology beyond classrooms, through hands-on learning and real problem " +
    "solving. We don't just learn tech, we build with it. Because real innovation begins " +
    "when ideas meet execution.\n\nSome of our major projects include Messit, BunkBuddies, " +
    "StudyHub, etc."

private const val GITHUB_REPO_URL = "https://github.com/vinnovateit/latch"
private const val INSTAGRAM_URL = "https://www.instagram.com/vinnovateit/"
private const val LINKEDIN_URL = "https://www.linkedin.com/company/v-innovate-it/"
private const val GITHUB_ORG_URL = "https://github.com/vinnovateit"

@Composable
fun AboutScreen(
    platform: PlatformServices,
    onBack: () -> Unit,
    updateState: UpdateState = UpdateState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onInstallUpdate: (String) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LatchDetailHeader(
            title = "About",
            onBack = onBack,
        )

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
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                // ── Latch ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = LatchMark,
                        contentDescription = "Latch logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Latch",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = satoshiFontFamily(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    // Version + inline refresh
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val versionLabel = buildString {
                            append("v")
                            append(platform.buildInfo.versionName)
                            if (platform.buildInfo.isDebug) append(" (debug)")
                            if (!platform.buildInfo.isInstalled) append(" — dev")
                        }
                        val isChecking = updateState is UpdateState.Checking
                        val statusSuffix = when (updateState) {
                            is UpdateState.UpToDate -> " · Latest"
                            is UpdateState.UpdateAvailable -> " · v${updateState.version} available"
                            is UpdateState.Downloaded -> " · v${updateState.version} ready"
                            is UpdateState.Dismissed -> " · Update postponed"
                            is UpdateState.Error -> " · Check failed"
                            else -> ""
                        }
                        Text(
                            text = versionLabel + statusSuffix,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = satoshiFontFamily(),
                        )
                        Spacer(Modifier.width(2.dp))
                        IconButton(
                            onClick = onCheckForUpdates,
                            enabled = !isChecking,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = LatchIcons.Autorenew,
                                contentDescription = "Check for updates",
                                tint = if (isChecking)
                                    MaterialTheme.colorScheme.outline
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    // Show download/install actions when update is available or downloaded
                    when (updateState) {
                        is UpdateState.UpdateAvailable -> {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = onDownloadUpdate) {
                                Text("Download v${updateState.version}")
                            }
                        }
                        is UpdateState.Downloaded -> {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { onInstallUpdate(updateState.filePath) }) {
                                Text("Install and restart")
                            }
                        }
                        else -> {}
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = LATCH_BLURB,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(32.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(32.dp))

                // ── VinnovateIT ───────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "VinnovateIT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = satoshiFontFamily(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = VINNOVATEIT_BLURB,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    SocialLinksRow(
                        onOpenInstagram = { platform.systemActions.openUrl(INSTAGRAM_URL) },
                        onOpenLinkedIn = { platform.systemActions.openUrl(LINKEDIN_URL) },
                        onOpenGitHub = { platform.systemActions.openUrl(GITHUB_ORG_URL) },
                    )
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(24.dp))

                ContributeSection(onContribute = { platform.systemActions.openUrl(GITHUB_REPO_URL) })

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ContributeSection(onContribute: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Contribute",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
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
