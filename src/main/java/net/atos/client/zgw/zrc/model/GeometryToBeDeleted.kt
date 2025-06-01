/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model

/**
 * Geometry class to indicate that the current geometry should be deleted.
 * Used to mark a geometry field for deletion in the ZGW ZRC API using
 * the [nl.info.client.zgw.zrc.jsonb.GeoJSONGeometryJsonbSerializer].
 */
class GeometryToBeDeleted : Geometry {
    constructor() { setMarkGeometryForDeletion() }

    override fun toString() = "Not implemented"
}
