/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.model.audit.besluiten.BesluitInformatieobjectWijziging
import nl.info.client.zgw.shared.model.audit.besluiten.BesluitWijziging
import nl.info.client.zgw.shared.model.audit.documenten.EnkelvoudigInformatieobjectWijziging
import nl.info.client.zgw.shared.model.audit.documenten.GebruiksrechtenWijziging
import nl.info.client.zgw.shared.model.audit.documenten.ObjectInformatieobjectWijziging
import java.lang.reflect.Type

enum class ObjectType(private val url: String, val auditClass: Type) {
    ENKELVOUDIG_INFORMATIEOBJECT(
        "/documenten/api/v1/enkelvoudiginformatieobjecten/",
        EnkelvoudigInformatieobjectWijziging::class.java
    ),

    GEBRUIKSRECHTEN("/documenten/api/v1/gebruiksrechten", GebruiksrechtenWijziging::class.java),

    OBJECT_INFORMATIEOBJECT("documenten/api/v1/objectinformatieobjecten", ObjectInformatieobjectWijziging::class.java),

    BESLUIT("/besluiten/api/v1/besluiten", BesluitWijziging::class.java),

    BESLUIT_INFORMATIEOBJECT("/besluiten/api/v1/besluitinformatieobjecten", BesluitInformatieobjectWijziging::class.java);

    companion object {
        fun getObjectType(url: String): ObjectType =
            entries.firstOrNull { url.contains(it.url) }
                ?: throw ZgwRuntimeException("URL '$url' wordt niet ondersteund")
    }
}
