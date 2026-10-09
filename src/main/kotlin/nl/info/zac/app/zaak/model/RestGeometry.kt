/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.bag.model.generated.PointGeoJSON
import nl.info.client.bag.model.generated.PuntOfVlak
import nl.info.client.bag.model.generated.Surface
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.math.BigDecimal

@AllOpen
@NoArgConstructor
data class RestGeometry(
    var type: RestGeometryType,

    var point: RestCoordinates? = null,

    var polygon: List<List<RestCoordinates>>? = null,

    var geometrycollection: List<RestGeometry>? = null
)

/**
 * Converts a [RestGeometry] to a [GeoJSONGeometry].
 * Only supports [RestGeometryType.POINT] geometry type for now.
 */
fun RestGeometry.toGeoJSONGeometry(): GeoJSONGeometry =
    when (this.type) {
        RestGeometryType.POINT -> GeoJSONGeometry().apply {
            type = GeometryTypeEnum.POINT
            coordinates = listOf(
                this@toGeoJSONGeometry.point?.longitude?.toBigDecimal(),
                this@toGeoJSONGeometry.point?.latitude?.toBigDecimal()
            )
        }
        else -> {
            throw IllegalArgumentException("Unsupported geometry type: ${this.type}")
        }
    }

fun GeoJSONGeometry.toRestGeometry() = RestGeometry(
    type = this.type.toRestGeometryType(),
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

fun Surface.toRestGeometry() = RestGeometry(
    type = RestGeometryType.POLYGON,
    polygon = coordinates.map { ring -> ring.map { it.toRestCoordinates() } }
)

fun PointGeoJSON.toRestGeometry() = RestGeometry(
    type = RestGeometryType.POINT,
    point = coordinates.toRestCoordinates()
)

fun PuntOfVlak.toRestGeometry() = punt?.toRestGeometry() ?: vlak?.toRestGeometry()

private fun List<BigDecimal>.toRestCoordinates() = RestCoordinates(
    longitude = this[0].toDouble(),
    latitude = this[1].toDouble()
)
