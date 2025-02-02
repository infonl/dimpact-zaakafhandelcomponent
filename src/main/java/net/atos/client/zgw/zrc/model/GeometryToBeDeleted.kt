package net.atos.client.zgw.zrc.model

/**
 * Geometry class to indicate that the current geometry should be deleted.
 */
class GeometryToBeDeleted : Geometry {
    constructor() { setMarkGeometryForDeletion() }

    override fun toString() = "Not implemented"
}
