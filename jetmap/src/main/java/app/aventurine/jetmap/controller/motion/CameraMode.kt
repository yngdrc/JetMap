package app.aventurine.jetmap.controller.motion

/**
 * Camera behaviour, mirroring the modes used by turn-by-turn navigation apps.
 */
enum class CameraMode {
    /** User controls the camera. No automatic following. */
    FREE,

    /** Camera keeps the tracked position centered, north up. */
    FOLLOW,

    /** Camera rotates with the travel bearing and offsets the position towards the bottom. */
    FOLLOW_BEARING,

    /** Camera shows the whole route. */
    OVERVIEW
}

