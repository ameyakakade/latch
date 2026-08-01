package com.vinnovateit.latch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vinnovateit.latch.ui.theme.AccentSeeds
import kotlin.math.abs

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
    var showCustomDialog by remember { mutableStateOf(false) }
    val customColor = AccentSeeds.parseHexOrNull(selectedColorName)

    // Scrollable rather than wrapping: 6 presets + the custom swatch (308dp+ of
    // circles alone, before gaps) is wider than the dialog on the compact window
    // this app opens at, and AlertDialog clips content that overflows its Surface
    // rather than letting it spill -- without this, the last swatch or two were
    // simply invisible with no way to reach them.
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentSeeds.ordered.forEach { (name, color) ->
            AccentSwatchButton(
                color = color,
                isSelected = name == selectedColorName,
                onClick = { onColorSelected(name) },
            )
        }

        // Custom swatch: shows the last-picked colour once one is active, or a
        // rainbow "add" affordance beforehand. There is deliberately no separate
        // "last custom colour" setting -- switching to a preset and back starts
        // the picker fresh from that preset, which is a fine trade for not
        // persisting a second value alongside accentColor.
        if (customColor != null) {
            AccentSwatchButton(
                color = customColor,
                isSelected = true,
                onClick = { showCustomDialog = true },
            )
        } else {
            Surface(
                onClick = { showCustomDialog = true },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(2.dp, Brush.sweepGradient(RainbowSweep)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = LatchIcons.Add,
                        contentDescription = "Custom colour",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomColorDialog(
            initialColor = customColor ?: AccentSeeds.forName(selectedColorName),
            onConfirm = { picked ->
                with(AccentSeeds) { onColorSelected(picked.toHexString()) }
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false },
        )
    }
}

@Composable
private fun AccentSwatchButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // The border is always present (transparent when unselected) rather than
    // conditionally null -- a border that only appears on the selected swatch
    // nudged just that circle's rendered size/position relative to its plain
    // neighbours, which read as the row being unevenly spaced.
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = color,
        border = BorderStroke(
            3.dp,
            if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        ),
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

/** Sampled every 30deg of hue at the picker's fixed saturation/lightness. */
private val RainbowSweep: List<Color> =
    (0..360 step 30).map { hslToColor(it.toFloat(), CustomColorSaturation, CustomColorLightness) }

// ---------------------------------------------------------------------------
// Custom colour picker dialog
// ---------------------------------------------------------------------------

/** Fixed saturation/lightness for the hue slider -- matches the presets' depth. */
private const val CustomColorSaturation = 0.75f
private const val CustomColorLightness = 0.42f

@Composable
internal fun CustomColorDialog(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var hue by remember { mutableFloatStateOf(colorToHue(initialColor)) }
    var hexText by remember { mutableStateOf(with(AccentSeeds) { initialColor.toHexString() }) }
    var previewColor by remember { mutableStateOf(initialColor) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    fun applyHue(fraction: Float) {
        hue = (fraction * 360f).coerceIn(0f, 360f)
        previewColor = hslToColor(hue, CustomColorSaturation, CustomColorLightness)
        hexText = with(AccentSeeds) { previewColor.toHexString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom colour") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = previewColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {}
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { typed ->
                            hexText = typed
                            AccentSeeds.parseHexOrNull(typed)?.let { parsed ->
                                previewColor = parsed
                                hue = colorToHue(parsed)
                            }
                        },
                        label = { Text("Hex") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(FullHueGradient))
                        .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectTapGestures { offset -> applyHue(offset.x / trackWidthPx) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                applyHue(change.position.x / trackWidthPx)
                            }
                        },
                ) {
                    // Thumb: a white ring at the current hue position.
                    val thumbFraction = (hue / 360f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .offset {
                                    IntOffset(
                                        (thumbFraction * (trackWidthPx - 28.dp.toPx())).toInt(),
                                        0,
                                    )
                                },
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(3.dp, Color.White),
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(previewColor) },
                enabled = AccentSeeds.parseHexOrNull(hexText) != null,
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val FullHueGradient: List<Color> =
    (0..360 step 15).map { hslToColor(it.toFloat(), CustomColorSaturation, CustomColorLightness) }

/** Standard HSL -> RGB, hue in [0, 360), saturation/lightness in [0, 1]. */
private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}

/** Approximate hue (0-360) of an arbitrary RGB colour, for seeding the slider. */
private fun colorToHue(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta == 0f) return 0f
    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return if (hue < 0f) hue + 360f else hue
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
