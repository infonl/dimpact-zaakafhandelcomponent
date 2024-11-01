package net.atos.zac.zoeken.converter

import net.atos.zac.zoeken.model.ZoekObject
import net.atos.zac.zoeken.model.index.ZoekObjectType

abstract class AbstractZoekObjectConverter<out ZOEKOBJECT : ZoekObject> {
    abstract fun supports(objectType: ZoekObjectType): Boolean

    abstract fun convert(id: String): ZOEKOBJECT?
}
