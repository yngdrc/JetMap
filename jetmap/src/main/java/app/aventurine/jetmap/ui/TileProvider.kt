package app.aventurine.jetmap.ui

import android.content.res.AssetManager
import android.os.Parcelable
import java.io.InputStream

interface TileProvider : Parcelable {
    fun getTileInputStream(
        x: Int,
        y: Int,
        assetManager: AssetManager
    ): InputStream?
}