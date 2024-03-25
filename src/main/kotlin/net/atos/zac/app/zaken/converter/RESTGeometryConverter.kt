/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryCollection
import net.atos.client.zgw.zrc.model.GeometryType
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Point2D
import net.atos.client.zgw.zrc.model.Polygon
import net.atos.zac.app.zaken.model.RESTCoordinates
import net.atos.zac.app.zaken.model.RESTGeometry

class RESTGeometryConverter {
    fun convert(geometry: Geometry): RESTGeometry = RESTGeometry(
        type = geometry.type.toValue(),
        point = if (geometry.type == GeometryType.POINT) createRESTPoint(geometry as Point) else null,
        polygon = if (geometry.type == GeometryType.POLYGON) createRESTPolygon(geometry as Polygon) else null,
        geometrycollection = if (
            geometry.type == GeometryType.GEOMETRYCOLLECTION
        ) {
            createRESTGeometryCollection(geometry as GeometryCollection)
        } else {
            null
        }
    )

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    fun convert(restGeometry: RESTGeometry) =
        when (GeometryType.fromValue(restGeometry.type)) {
            GeometryType.POINT -> createPoint(restGeometry)
            GeometryType.POLYGON -> createPolygon(restGeometry)
            GeometryType.GEOMETRYCOLLECTION -> createGeometryCollection(restGeometry)
        }

    private fun createRESTPoint(point: Point) = RESTCoordinates(
        point.coordinates.x.toDouble(),
        point.coordinates.y.toDouble()
    )

    private fun createPoint(restGeometry: RESTGeometry) = Point(Point2D(restGeometry.point!!.x, restGeometry.point!!.y))

    private fun createRESTPolygon(polygon: Polygon) =
        polygon.coordinates.stream()
            .map { point2DS ->
                point2DS.stream()
                    .map { point2D: Point2D -> RESTCoordinates(point2D.x.toDouble(), point2D.y.toDouble()) }
                    .toList()
            }
            .toList()

    private fun createPolygon(restGeometry: RESTGeometry) =
        Polygon(
            restGeometry.polygon!!.stream()
                .map { polygon ->
                    polygon.stream()
                        .map { coordinates: RESTCoordinates? -> Point2D(coordinates!!.x, coordinates.y) }
                        .toList()
                }
                .toList()
        )

    private fun createRESTGeometryCollection(geometryCollection: GeometryCollection) =
        geometryCollection.geometries.stream()
            .map { convert(geometryCollection) }
            .toList()

    private fun createGeometryCollection(restGeometry: RESTGeometry): GeometryCollection =
        GeometryCollection(
            restGeometry.geometrycollection!!.stream().map { restGeometry1 ->
                convert(restGeometry1)
            }.toList()
        )
}
