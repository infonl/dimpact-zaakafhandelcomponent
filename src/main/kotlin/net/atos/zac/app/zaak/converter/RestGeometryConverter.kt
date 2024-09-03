/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryCollection
import net.atos.client.zgw.zrc.model.GeometryType
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Point2D
import net.atos.client.zgw.zrc.model.Polygon
import net.atos.zac.app.zaak.model.RestCoordinates
import net.atos.zac.app.zaak.model.RestGeometry

class RestGeometryConverter {
    fun convert(geometry: Geometry): RestGeometry = RestGeometry(
        type = geometry.type.toValue(),
        point = if (geometry.type == GeometryType.POINT) createRESTPoint(geometry as Point) else null,
        polygon = if (geometry.type == GeometryType.POLYGON) createRestPolygon(geometry as Polygon) else null,
        geometrycollection = if (
            geometry.type == GeometryType.GEOMETRYCOLLECTION
        ) {
            createRESTGeometryCollection(geometry as GeometryCollection)
        } else {
            null
        }
    )

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    fun convert(restGeometry: RestGeometry) =
        when (GeometryType.fromValue(restGeometry.type)) {
            GeometryType.POINT -> createPoint(restGeometry)
            GeometryType.POLYGON -> createPolygon(restGeometry)
            GeometryType.GEOMETRYCOLLECTION -> createGeometryCollection(restGeometry)
        }

    private fun createRESTPoint(point: Point) = RestCoordinates(
        point.coordinates.latitude.toDouble(),
        point.coordinates.longitude.toDouble(),
    )

    private fun createPoint(restGeometry: RestGeometry) = Point(
        Point2D(restGeometry.point!!.latitude, restGeometry.point!!.longitude)
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

    private fun createPolygon(restGeometry: RestGeometry) =
        Polygon(
            restGeometry.polygon?.map { polygon ->
                polygon.map { Point2D(it.latitude, it.longitude) }.toList()
            }
        )

    private fun createRESTGeometryCollection(geometryCollection: GeometryCollection) =
        geometryCollection.geometries.map { convert(geometryCollection) }

    private fun createGeometryCollection(restGeometry: RestGeometry): GeometryCollection =
        GeometryCollection(
            restGeometry.geometrycollection?.map { convert(it) }
        )
}
