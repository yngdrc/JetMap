package app.aventurine.jetmap.utils

import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.gesture.GestureApi
import app.aventurine.jetmap.controller.motion.MotionApi
import app.aventurine.jetmap.controller.path.PathApi
import app.aventurine.jetmap.controller.tile.TileApi
import app.aventurine.jetmap.controller.ui.UIApi

val JetMapController.motionApi: MotionApi
    get() = motionController as MotionApi

val JetMapController.tileApi: TileApi
    get() = tileController as TileApi

val JetMapController.gestureApi: GestureApi
    get() = gestureController as GestureApi

val JetMapController.pathApi: PathApi
    get() = pathController as PathApi

val JetMapController.uiApi: UIApi
    get() = uiController as UIApi