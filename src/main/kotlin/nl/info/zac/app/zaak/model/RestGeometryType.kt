/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum

enum class RestGeometryType {
    POINT,
    MULTI_POINT,
    LINE_STRING,
    MULTI_LINE_STRING,
    POLYGON,
    MULTI_POLYGON,
    FEATURE,
    FEATURE_COLLECTION,
    GEOMETRY_COLLECTION
}

fun GeometryTypeEnum.toRestGeometryType(): RestGeometryType = RestGeometryType.valueOf(this.name)
