package net.atos.client.zgw.zrc.model

/**
 * Geometry class to indicate that the current geometry should be deleted.
 * Used to mark a geometry field for deletion in the ZGW ZRC API using
 * the [nl.info.client.zgw.zrc.jsonb.GeometryJsonbSerializer].
 */
class GeometryToBeDeleted : Geometry {
    constructor() { setMarkGeometryForDeletion() }

    override fun toString() = "Not implemented"
}
