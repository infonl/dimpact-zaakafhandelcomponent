/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryCollection
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Polygon
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestGeometry(
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
    type = this.type.name,
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
