package app.aventurine.jetmap.utils

import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.gesture.GestureApi
import app.aventurine.jetmap.controller.motion.MotionApi

val JetMapController.motionApi: MotionApi
    get() = motionController as MotionApi

val JetMapController.gestureApi: GestureApi
    get() = gestureController as GestureApi