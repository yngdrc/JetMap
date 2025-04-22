package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.Parcelable

interface TileProvider : Parcelable {
    fun getTileBitmap(
        x: Int,
        y: Int,
        assetManager: AssetManager
    ): Bitmap?
}