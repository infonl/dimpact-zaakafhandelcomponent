package net.atos.zac.zoeken.model

import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import java.util.UUID

fun createZaakZoekObject(
    uuidAsString: String = UUID.randomUUID().toString(),
    type: ZoekObjectType = ZoekObjectType.ZAAK
) =
    ZaakZoekObject().apply {
        this.uuid = uuidAsString
        this.type = type
    }
