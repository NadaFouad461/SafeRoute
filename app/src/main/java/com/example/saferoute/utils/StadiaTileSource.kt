package com.example.saferoute.util

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex


object StadiaTileSource {


    private const val STADIA_API_KEY = "d6f9ce81-c3be-4a2a-b826-6b50bc094aef"

    // "alidade_smooth" = clean, general-purpose light style. Other options:
    // "alidade_smooth_dark", "outdoors", "osm_bright" - see stadiamaps.com/products/map-styles
    private const val STYLE = "alidade_smooth"

    val INSTANCE: OnlineTileSourceBase = object : OnlineTileSourceBase(
        "StadiaMaps",
        0, 20, 256, ".png",
        arrayOf("https://tiles.stadiamaps.com/tiles/$STYLE/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$zoom/$x/$y.png?api_key=$STADIA_API_KEY"
        }
    }
}