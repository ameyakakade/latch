package com.vinnovateit.latch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Section wrapper
// ---------------------------------------------------------------------------

@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/** 3 dp gap between rows inside a [SettingsSection]. */
@Composable
internal fun SettingsRowGap() {
    Spacer(Modifier.height(3.dp))
}

// ---------------------------------------------------------------------------
// Individual row
// ---------------------------------------------------------------------------

@Composable
internal fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null && enabled) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.38f,
                    ),
                )
            }
        }

        if (trailingContent != null) {
            Spacer(Modifier.width(12.dp))
            trailingContent()
        }
    }
}

// ---------------------------------------------------------------------------
// Selection dialog
// ---------------------------------------------------------------------------

data class SelectionOption(
    val label: String,
    val icon: ImageVector? = null,
    val displayLabel: String = label,
)

@Composable
internal fun SettingsSelectionDialog(
    title: String,
    description: String? = null,
    options: List<SelectionOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    bottomContent: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                options.forEach { option ->
                    val isSelected = option.label == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(option.label)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = option.displayLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = LatchIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                if (bottomContent != null) {
                    Spacer(Modifier.height(12.dp))
                    bottomContent()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ---------------------------------------------------------------------------
// Destructive action dialog
// ---------------------------------------------------------------------------

@Composable
internal fun SettingsActionDialog(
    title: String,
    description: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelText) }
        },
    )
}

// ---------------------------------------------------------------------------
// Accent colour picker
// ---------------------------------------------------------------------------

@Composable
internal fun AccentColorPicker(
    selectedColorName: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.vinnovateit.latch.ui.theme.AccentSeeds.ordered.forEach { (name, color) ->
            val isSelected = name == selectedColorName
            Surface(
                onClick = { onColorSelected(name) },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = color,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    null
                },
            ) {
                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = LatchIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 24 dp circular swatch used as a trailing decoration in the accent SettingsItem.
 * In monochrome mode it shows a grey fill.
 */
@Composable
internal fun AccentSwatch(accentColor: Color, useMonochrome: Boolean) {
    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = if (useMonochrome) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
    ) {}
}
