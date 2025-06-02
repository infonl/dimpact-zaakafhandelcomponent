/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.util

import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.productaanvraag.model.generated.Geometry

/**
 * Converts a [Geometry] object to a [GeoJSONGeometry].
 * Currently only supports [Geometry.Type.POINT] geometry type.
 *
 * @return a [GeoJSONGeometry] representation of the [Geometry].
 * @throws IllegalArgumentException if the geometry type is not supported.
 */
fun Geometry.toGeoJSONGeometry(): GeoJSONGeometry =
    when (this.type) {
        Geometry.Type.POINT -> GeoJSONGeometry().apply {
            type = GeometryTypeEnum.POINT
            coordinates = listOf(
                this@toGeoJSONGeometry.coordinates[1].toBigDecimal(), // longitude
                this@toGeoJSONGeometry.coordinates[0].toBigDecimal() // latitude
            )
        }
        else -> throw IllegalArgumentException("Unsupported geometry type: ${this.type}")
    }
