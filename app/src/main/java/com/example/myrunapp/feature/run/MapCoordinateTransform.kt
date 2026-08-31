package com.example.myrunapp.feature.run

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MapCoordinate(
    val latitude: Double,
    val longitude: Double
)

private const val EARTH_RADIUS = 6378245.0
private const val ECCENTRICITY_SQUARED = 0.00669342162296594323

fun wgs84ToGcj02(latitude: Double, longitude: Double): MapCoordinate {
    if (isOutsideChina(latitude, longitude)) {
        return MapCoordinate(latitude, longitude)
    }

    var dLat = transformLatitude(longitude - 105.0, latitude - 35.0)
    var dLon = transformLongitude(longitude - 105.0, latitude - 35.0)
    val radLat = latitude / 180.0 * PI
    var magic = sin(radLat)
    magic = 1 - ECCENTRICITY_SQUARED * magic * magic
    val sqrtMagic = sqrt(magic)
    dLat = (dLat * 180.0) / ((EARTH_RADIUS * (1 - ECCENTRICITY_SQUARED)) / (magic * sqrtMagic) * PI)
    dLon = (dLon * 180.0) / (EARTH_RADIUS / sqrtMagic * cos(radLat) * PI)

    return MapCoordinate(
        latitude = latitude + dLat,
        longitude = longitude + dLon
    )
}

private fun isOutsideChina(latitude: Double, longitude: Double): Boolean {
    return longitude < 72.004 ||
        longitude > 137.8347 ||
        latitude < 0.8293 ||
        latitude > 55.8271
}

private fun transformLatitude(x: Double, y: Double): Double {
    var result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
    result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    result += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
    result += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
    return result
}

private fun transformLongitude(x: Double, y: Double): Double {
    var result = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
    result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    result += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
    result += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
    return result
}
