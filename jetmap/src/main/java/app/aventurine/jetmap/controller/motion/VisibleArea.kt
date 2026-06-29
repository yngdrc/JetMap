package app.aventurine.jetmap.controller.motion

data class VisibleArea(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    operator fun times(value: Int): VisibleArea {
        return this.copy(
            left = left * value,
            top = top * value,
            right = right * value,
            bottom = bottom * value
        )
    }
}