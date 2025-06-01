/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class GeoJSONGeometryWithDeletionSupport(
    /**
     * Indicates that the geometry should be deleted.
     * This is used to mark a geometry field for deletion in the ZGW ZRC API by writing a `null` value
     * as per the ZGW ZRX API specification.
     */
    val markGeometryForDeletion: Boolean = false
) : GeoJSONGeometry()
