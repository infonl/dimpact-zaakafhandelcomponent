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

// TODO
fun RestGeometry.toGeoJSONGeometry(): GeoJSONGeometry =
    when (this.type) {
        "Point" -> GeoJSONGeometry().apply {
            type = GeometryTypeEnum.POINT
            coordinates = listOf(
                listOf(
                    listOf(
                        listOf(
                            this@toGeoJSONGeometry.point!!.latitude.toBigDecimal(),
                            this@toGeoJSONGeometry.point!!.longitude.toBigDecimal()
                        )
                    )
                )
            )
        }
        else -> {
            throw IllegalArgumentException("Unsupported geometry type: ${this.type}")
        }
    }

fun Geometry.toRestGeometry() = RestGeometry(
    type = this.type.toValue(),
    point = (this as? Point)?.let { createRestCoordinates(it) },
    polygon = (this as? Polygon)?.let { createRestPolygon(it) },
    geometrycollection = (this as? GeometryCollection)?.let { createRestGeometryCollection(it) }
)

private fun createRestCoordinates(point: Point) = RestCoordinates(
    point.coordinates.latitude.toDouble(),
    point.coordinates.longitude.toDouble(),
)

fun GeoJSONGeometry.toRestGeometry() = RestGeometry(
    type = this.type.name,
    point = if (this.type == GeometryTypeEnum.POINT) {
        RestCoordinates(
            latitude = this.coordinates.first().first().first()[0].toDouble(),
            longitude = this.coordinates.first().first().first()[1].toDouble()
        )
    } else {
        null
    },
    // not supported in ZAC
    polygon = null,
    // not supported in ZAC
    geometrycollection = null
)

private fun createRestPolygon(polygon: Polygon) =
    polygon.coordinates
        .map { point2D ->
            point2D.map {
                RestCoordinates(
                    it.latitude.toDouble(),
                    it.longitude.toDouble()
                )
            }
        }

private fun createRestGeometryCollection(geometryCollection: GeometryCollection): List<RestGeometry> =
    geometryCollection.geometries.map { it.toRestGeometry() }
