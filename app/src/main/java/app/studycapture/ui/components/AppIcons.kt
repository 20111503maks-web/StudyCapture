package app.studycapture.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icons drawn from the design canvas (24×24 grid). Kept in code so the
 * app needs no icon library; Icon() tints them with the current content color.
 */
object AppIcons {
    private fun icon(name: String, vararg paths: String, stroke: Float = 1.9f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Home = icon("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
    val Library = icon(
        "library",
        "M4 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "M11 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
        "m17 5.5 3-.8 2.2 14.6-3 .8z",
    )
    val Capture = icon(
        "capture",
        "M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3",
        "M15.5 12a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0z",
        stroke = 2f,
    )
    val Cards = icon("cards", "M5 7h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z", "M7 3h12a2 2 0 0 1 2 2v12")
    val Profile = icon("profile", "M16 8a4 4 0 1 1-8 0a4 4 0 1 1 8 0z", "M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6")
    val Search = icon("search", "M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0z", "m20 20-3.5-3.5")
    val Back = icon("back", "m15 6-6 6 6 6", stroke = 2f)
    val Close = icon("close", "M6 6l12 12M18 6 6 18")
    val Chevron = icon("chevron", "m9 6 6 6-6 6", stroke = 2f)
    val Check = icon("check", "m5 12 5 5 9-10", stroke = 2.4f)
    val Gallery = icon("gallery", "M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M11 10a2 2 0 1 1-4 0a2 2 0 1 1 4 0z", "m3 18 5-5 4 4 3-3 6 6")
    val Text = icon("text", "M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M9 9h6M9 13h6M9 17h4")
    val Document = icon("document", "M6 3h8l5 5v12a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "M14 3v5h5")
    val Warning = icon("warning", "M12 4 2.5 20h19z", "M12 10v4M12 17v.5", stroke = 2f)
    val Lock = icon("lock", "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Clock = icon("clock", "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z", "M12 8v4l3 2")
    val Shield = icon("shield", "M12 3 4 6v6c0 4.5 3.4 8.3 8 9 4.6-.7 8-4.5 8-9V6z", "m8.5 12 2.5 2.5 4.5-5")
    val Trash = icon("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13M9 7V4h6v3")
    val Edit = icon("edit", "M4 20h4L19 9l-4-4L4 16z", "m13.5 6.5 4 4")
    val Refresh = icon("refresh", "M20 11a8 8 0 1 0-2.3 5.7", "M20 5v6h-6")
}
