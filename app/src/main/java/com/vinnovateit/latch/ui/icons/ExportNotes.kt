package com.vinnovateit.latch.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val ExportNotes: ImageVector
  get() {
    if (_export_notes != null) {
      return _export_notes!!
    }
    _export_notes =
      ImageVector.Builder(
          name = "export_notes",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(16.2f, 20.5f)
            lineTo(19f, 17.7f)
            verticalLineTo(20f)
            horizontalLineToRelative(1f)
            verticalLineTo(16f)
            horizontalLineTo(16f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(2.3f)
            lineToRelative(-2.8f, 2.8f)
            lineToRelative(0.7f, 0.7f)
            close()
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineToRelative(6.7f)
            quadTo(20.53f, 11.48f, 20.03f, 11.31f)
            reflectiveQuadTo(19f, 11.08f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineToRelative(6.05f)
            quadToRelative(0.07f, 0.55f, 0.24f, 1.05f)
            reflectiveQuadTo(11.68f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 18f)
            quadToRelative(0f, 0.27f, 0f, 0.51f)
            reflectiveQuadTo(5f, 19f)
            verticalLineTo(5f)
            verticalLineToRelative(6.07f)
            quadTo(5f, 11.02f, 5f, 11.01f)
            reflectiveQuadTo(5f, 11f)
            reflectiveQuadToRelative(0f, 2.05f)
            reflectiveQuadTo(5f, 18f)
            close()
            moveTo(7f, 17f)
            horizontalLineToRelative(4.08f)
            quadToRelative(0.07f, -0.52f, 0.24f, -1.03f)
            quadTo(11.48f, 15.48f, 11.68f, 15f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveTo(7f, 13f)
            horizontalLineToRelative(6.1f)
            quadToRelative(0.8f, -0.75f, 1.79f, -1.25f)
            reflectiveQuadTo(17f, 11.08f)
            verticalLineTo(11f)
            horizontalLineTo(7f)
            verticalLineToRelative(2f)
            close()
            moveTo(7f, 9f)
            horizontalLineTo(17f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            verticalLineTo(9f)
            close()
            moveTo(18f, 23f)
            quadToRelative(-2.07f, 0f, -3.54f, -1.46f)
            reflectiveQuadTo(13f, 18f)
            reflectiveQuadToRelative(1.46f, -3.54f)
            reflectiveQuadTo(18f, 13f)
            reflectiveQuadToRelative(3.54f, 1.46f)
            reflectiveQuadTo(23f, 18f)
            reflectiveQuadToRelative(-1.46f, 3.54f)
            reflectiveQuadTo(18f, 23f)
            close()
          }
        }
        .build()
    return _export_notes!!
  }

private var _export_notes: ImageVector? = null
