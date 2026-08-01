package com.vinnovateit.latch.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.desktop.LatchMark
import com.vinnovateit.latch.desktop.resources.Res
import com.vinnovateit.latch.desktop.resources.home_status_connected
import com.vinnovateit.latch.desktop.resources.home_status_disconnected
import com.vinnovateit.latch.ui.theme.modernizFontFamily
import org.jetbrains.compose.resources.stringResource

/**
 * The home screen top bar: Latch mark on the left, Moderniz wordmark centred,
 * overflow menu on the right.
 *
 * When the navigation rail is visible the overflow menu hides Stats and Settings
 * (they're already one click away on the rail), keeping only "How it works".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LatchHomeTopBar(
    onHowItWorks: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showNavigationItems: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        navigationIcon = {
            Icon(
                imageVector = LatchMark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp).size(28.dp),
            )
        },
        title = {
            Text(
                text = "Latch",
                fontFamily = modernizFontFamily(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = LatchIcons.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (showNavigationItems) {
                        DropdownMenuItem(
                            text = { Text("Stats") },
                            leadingIcon = {
                                Icon(LatchIcons.BarChart, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenStats()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = {
                                Icon(LatchIcons.SettingsOutlined, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("How it works") },
                        onClick = {
                            menuExpanded = false
                            onHowItWorks()
                        },
                    )
                }
            }
        },
    )
}

/**
 * Secondary-screen header: optional back button + large Moderniz title.
 * [onBack] is null when the navigation rail is visible (no back arrow needed).
 */
@Composable
internal fun LatchDetailHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier.padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            FilledIconButton(
                onClick = onBack,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(
                    imageVector = LatchIcons.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = modernizFontFamily(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The LATCHED / DISCONNECTED pill that fades in after a power button press
 * and auto-dismisses after 5 seconds.
 */
@Composable
internal fun StatusPill(
    visible: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            AnimatedContent(
                targetState = isConnected,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "PillText",
            ) { connected ->
                Text(
                    text = if (connected) {
                        stringResource(Res.string.home_status_connected)
                    } else {
                        stringResource(Res.string.home_status_disconnected)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
