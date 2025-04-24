package app.aventurine.jetmap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}

fun Rect.rotateBy(angle: Float): Rect {
    val matrix = Matrix()
    matrix.rotateZ(angle)
    return matrix.map(this)
}