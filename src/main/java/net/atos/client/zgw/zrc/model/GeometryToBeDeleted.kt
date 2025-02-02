package net.atos.client.zgw.zrc.model

/**
 * Geometry class to indicate that the current geometry should be deleted.
 */
class GeometryToBeDeleted : Geometry {
    constructor() { setDeleteGeometry() }

    override fun toString() = "Not implemented"
}
