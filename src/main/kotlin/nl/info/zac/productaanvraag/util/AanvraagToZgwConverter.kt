/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.util

import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.productaanvraag.model.generated.Geometry

fun Geometry.toGeoJSONGeometry() =
    when (this.type) {
        Geometry.Type.POINT -> GeoJSONGeometry().apply {
            type = GeometryTypeEnum.POINT
            coordinates = listOf(
                listOf(
                    listOf(
                        listOf(
                            this@toGeoJSONGeometry.coordinates[0].toBigDecimal(),
                            this@toGeoJSONGeometry.coordinates[1].toBigDecimal()
                        )
                    )
                )
            )
        }
        else -> throw IllegalArgumentException("Unsupported geometry type: ${this.type}")
    }
