package com.vinnovateit.latch.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Material symbol icons not available in material-icons-core, vendored as
 * ImageVector path data so the 36 MB material-icons-extended jar is never needed.
 *
 * Every fill is black; Icon's tint parameter overrides it at the call site.
 */
internal object LatchIcons {

    val PowerSettingsNew: ImageVector by lazy {
        icon(
            "PowerSettingsNew",
            "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42C17.99 7.86 19 9.81 19 12" +
                "c0 3.87-3.13 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17" +
                "C4.23 6.82 3 9.26 3 12c0 4.97 4.03 9 9 9s9-4.03 9-9" +
                "c0-2.74-1.23-5.18-3.17-6.83z",
        )
    }

    val Wifi: ImageVector by lazy {
        icon(
            "Wifi",
            "M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9z" +
                "m8 8l3 3 3-3c-1.65-1.66-4.34-1.66-6 0z" +
                "m-4-4l2 2c2.76-2.76 7.24-2.76 10 0l2-2C15.14 9.14 8.87 9.14 5 13z",
        )
    }

    val Speed: ImageVector by lazy {
        icon(
            "Speed",
            "M20.38 8.57l-1.23 1.85a8 8 0 01-.22 7.58H5.07A8 8 0 0115.58 6.85l1.85-1.23" +
                "A10 10 0 003.35 19a2 2 0 001.72 1h13.85a2 2 0 001.74-1" +
                " 10 10 0 00-.28-11.43zm-9.79 6.84a2 2 0 002.83 0l5.66-8.49-8.49 5.66a2 2 0 000 2.83z",
        )
    }

    val DarkMode: ImageVector by lazy {
        icon(
            "DarkMode",
            "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9" +
                "c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26" +
                "-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z",
        )
    }

    val LightMode: ImageVector by lazy {
        icon(
            "LightMode",
            "M6.76 4.84l-1.8-1.79-1.41 1.41 1.79 1.79zM4 10.5H1v2h3zm9-9.95h-2V3.5h2z" +
                "m7.45 3.91l-1.41-1.41-1.79 1.79 1.41 1.41zM17.24 19.16l1.79 1.8 1.41-1.41-1.8-1.79z" +
                "M20 10.5v2h3v-2zm-8-5c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6z" +
                "m-1 16.95h2V19.5h-2zm-7.45-3.91l1.41 1.41 1.79-1.8-1.41-1.41z",
        )
    }

    val DesktopWindows: ImageVector by lazy {
        icon(
            "DesktopWindows",
            "M21 2H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h7l-2 3v1h8v-1l-2-3h7" +
                "c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 12H3V4h18v10z",
        )
    }

    val InvertColors: ImageVector by lazy {
        icon(
            "InvertColors",
            "M17.66 7.93L12 2.27 6.34 7.93c-3.12 3.12-3.12 8.19 0 11.31" +
                "C7.9 20.8 9.95 21.58 12 21.58c2.05 0 4.1-.78 5.66-2.34" +
                " 3.12-3.12 3.12-8.19 0-11.31zM12 19.59c-1.6 0-3.11-.62-4.24-1.76" +
                "C6.62 16.69 6 15.19 6 13.59s.62-3.11 1.76-4.24L12 5.1v14.49z",
        )
    }

    val Autorenew: ImageVector by lazy {
        icon(
            "Autorenew",
            "M12 6v3l4-4-4-4v3c-4.42 0-8 3.58-8 8 0 1.57.46 3.03 1.24 4.26" +
                "L6.7 14.8c-.45-.83-.7-1.79-.7-2.8 0-3.31 2.69-6 6-6z" +
                "m6.76 1.74L17.3 9.2c.44.84.7 1.79.7 2.8 0 3.31-2.69 6-6 6v-3l-4 4 4 4v-3" +
                "c4.42 0 8-3.58 8-8 0-1.57-.46-3.03-1.24-4.26z",
        )
    }

    val ArrowOutward: ImageVector by lazy {
        icon("ArrowOutward", "M6 6v2h8.59L5 17.59 6.41 19 16 9.41V18h2V6z")
    }

    val ArrowDownward: ImageVector by lazy {
        icon(
            "ArrowDownward",
            "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z",
        )
    }

    val ArrowUpward: ImageVector by lazy {
        icon(
            "ArrowUpward",
            "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z",
        )
    }

    val Update: ImageVector by lazy {
        icon(
            "Update",
            "M21 10.12h-6.78l2.74-2.82c-2.73-2.7-7.15-2.8-9.88-.1" +
                "a7 7 0 000 9.79 7 7 0 009.88 0C18.32 15.65 19 14.08 19 12.1h2" +
                "c0 1.98-.88 4.55-2.64 6.29-3.51 3.48-9.21 3.48-12.72 0" +
                "-3.5-3.47-3.53-9.11-.02-12.58 3.51-3.47 9.14-3.47 12.65 0L21 3v7.12z" +
                "M12.5 8v4.25l3.5 2.08-.72 1.21L11 13V8h1.5z",
        )
    }

    val InfoOutline: ImageVector by lazy {
        icon(
            "InfoOutline",
            "M11 17h2v-6h-2v6zm1-15C6.48 2 2 6.48 2 12s4.48 10 10 10" +
                " 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8" +
                "s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-12h2V7h-2v1zm0 2h2" +
                "c1.1 0 2 .9 2 2v6h-2v-6h-2v-2z",
        )
    }

    val HelpOutline: ImageVector by lazy {
        icon(
            "HelpOutline",
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-3h2v2h-2v-2zm1.71-9.71c-.38-.38-.88-.59-1.42-.59-1.12 0-2 .88-2 2H8c0-2.21 1.79-4 4-4 1.06 0 2.08.42 2.83 1.17.75.75 1.17 1.77 1.17 2.83 0 1.44-.8 2.2-1.49 2.86-.59.57-1.01.98-1.01 1.84V15h-2v-.89c0-1.75.9-2.6 1.65-3.3.52-.5.85-.84.85-1.48 0-.54-.21-1.04-.59-1.42z",
        )
    }

    val SystemUpdateAlt: ImageVector by lazy {
        icon(
            "SystemUpdateAlt",
            "M5 20h14v-2H5v2zm7-18L5.33 8h3.84V14h4.66V8h3.84L12 2z",
        )
    }

    val VersionTag: ImageVector by lazy {
        icon(
            "VersionTag",
            "M7 4h10l3 4v12H4V4h3zm0 2v12h12V8.73L16.05 6H7zm2 2h2v2H9V8zm0 4h6v2H9v-2zm0 4h4v2H9v-2z",
        )
    }

    val BarChart: ImageVector by lazy {
        icon("BarChart", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z")
    }

    val Restore: ImageVector by lazy {
        icon(
            "Restore",
            "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6" +
                "c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7" +
                "c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21" +
                "c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z",
        )
    }

    val Login: ImageVector by lazy {
        icon(
            "Login",
            "M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5z" +
                "m9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z",
        )
    }

    val MoreVert: ImageVector by lazy {
        icon(
            "MoreVert",
            "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z" +
                "m0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" +
                "m0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        )
    }

    val Check: ImageVector by lazy {
        icon("Check", "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    }

    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z")
    }

    val Person: ImageVector by lazy {
        icon(
            "Person",
            "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4z" +
                "m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
        )
    }

    // Custom window title bar controls.

    val Minimize: ImageVector by lazy {
        icon("Minimize", "M6 11h12v2H6z")
    }

    val Close: ImageVector by lazy {
        icon(
            "Close",
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41" +
                " 17.59 19 19 17.59 13.41 12z",
        )
    }

    val Lock: ImageVector by lazy {
        icon(
            "Lock",
            "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10" +
                "c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2z" +
                "m-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" +
                "m3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z",
        )
    }

    // Used as navigation rail icons; vendored to avoid relying on material-icons-core internals.

    val HomeOutlined: ImageVector by lazy {
        icon(
            "Home",
            "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z",
        )
    }

    val SettingsOutlined: ImageVector by lazy {
        icon(
            "Settings",
            "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58" +
                "c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96" +
                "c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84" +
                "c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96" +
                "c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58" +
                "c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61" +
                "l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54" +
                "c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54" +
                "c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32" +
                "c.12-.22.07-.47-.12-.61l-2.01-1.58z" +
                "M12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z",
        )
    }
}

private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
