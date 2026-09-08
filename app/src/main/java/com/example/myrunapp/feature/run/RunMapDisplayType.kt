package com.example.myrunapp.feature.run

import com.amap.api.maps.AMap

enum class RunMapDisplayType(
    val label: String,
    internal val amapType: Int
) {
    Normal("标准", AMap.MAP_TYPE_NORMAL),
    Satellite("卫星", AMap.MAP_TYPE_SATELLITE),
    Night("夜间", AMap.MAP_TYPE_NIGHT);

    fun next(): RunMapDisplayType {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}
