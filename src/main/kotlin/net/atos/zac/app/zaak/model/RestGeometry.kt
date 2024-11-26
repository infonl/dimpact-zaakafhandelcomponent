/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryCollection
import net.atos.client.zgw.zrc.model.GeometryType
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Point2D
import net.atos.client.zgw.zrc.model.Polygon
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestGeometry(
    var type: String,

    var point: RestCoordinates? = null,

    var polygon: List<List<RestCoordinates>>? = null,

    var geometrycollection: List<RestGeometry>? = null
)

fun RestGeometry.toGeometry(): Geometry =
    when (GeometryType.fromValue(this.type)) {
        GeometryType.POINT -> createPoint(this)
        GeometryType.POLYGON -> createPolygon(this)
        GeometryType.GEOMETRYCOLLECTION -> createGeometryCollection(this)
    }

fun Geometry.toRestGeometry() = RestGeometry(
    type = this.type.toValue(),
    point = (this as? Point)?.let { createRESTPoint(it) },
    polygon = (this as? Polygon)?.let { createRestPolygon(it) },
    geometrycollection = (this as? GeometryCollection)?.let { createRestGeometryCollection(it) }
)

private fun createPoint(restGeometry: RestGeometry) = Point(
    Point2D(restGeometry.point!!.latitude, restGeometry.point!!.longitude)
)

private fun createPolygon(restGeometry: RestGeometry) =
    Polygon(
        restGeometry.polygon?.map { polygon ->
            polygon.map { Point2D(it.latitude, it.longitude) }.toList()
        }
    )

private fun createGeometryCollection(restGeometry: RestGeometry): GeometryCollection =
    GeometryCollection(
        restGeometry.geometrycollection?.map { it.toGeometry() }
    )

private fun createRESTPoint(point: Point) = RestCoordinates(
    point.coordinates.latitude.toDouble(),
    point.coordinates.longitude.toDouble(),
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
