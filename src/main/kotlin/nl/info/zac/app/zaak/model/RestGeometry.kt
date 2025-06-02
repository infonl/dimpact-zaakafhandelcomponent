/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestGeometry(
    /**
     * The type of the geometry as defined by the values of [GeometryTypeEnum].
     * E.g. "Point", "Polygon", etc.
     * We should refactor this to use [GeometryTypeEnum] directly in the future.
     */
    var type: String,

    var point: RestCoordinates? = null,

    var polygon: List<List<RestCoordinates>>? = null,

    var geometrycollection: List<RestGeometry>? = null
)

/**
 * Converts a [RestGeometry] to a [GeoJSONGeometry].
 * Only supports [GeometryTypeEnum.POINT] geometry type for now.
 */
fun RestGeometry.toGeoJSONGeometry(): GeoJSONGeometry =
    when (this.type.uppercase()) {
        GeometryTypeEnum.POINT.name -> GeoJSONGeometry().apply {
            type = GeometryTypeEnum.POINT
            coordinates = listOf(
                this@toGeoJSONGeometry.point?.longitude?.toBigDecimal(),
                this@toGeoJSONGeometry.point?.latitude?.toBigDecimal(),
            )
        }
        else -> {
            throw IllegalArgumentException("Unsupported geometry type: ${this.type.uppercase()}")
        }
    }

fun GeoJSONGeometry.toRestGeometry() = RestGeometry(
    // we currently use the value of [GeometryTypeEnum] as a string
    type = this.type.toString(),
    point = if (this.type == GeometryTypeEnum.POINT) {
        RestCoordinates(
            longitude = this.coordinates[0].toDouble(),
            latitude = this.coordinates[1].toDouble(),
        )
    } else {
        null
    },
    // not supported currently
    polygon = null,
    // not supported currently
    geometrycollection = null
)
