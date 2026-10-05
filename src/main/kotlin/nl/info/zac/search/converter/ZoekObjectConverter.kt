/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import nl.info.zac.search.model.ZaakAutorisatieGegevens
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

interface ZoekObjectConverter<ZOEKOBJECT : ZoekObject> {
    fun supports(objectType: ZoekObjectType): Boolean

    fun convert(id: String): ZOEKOBJECT?

    /**
     * Converts [id], looking up the zaak-level data through [zaakAutorisatieGegevens] instead of always
     * deriving it directly, so that callers converting several zoekobjecten linked to the same zaak
     * (e.g. [nl.info.zac.search.IndexingService]) can share one memoized lookup.
     */
    fun convert(id: String, zaakAutorisatieGegevens: (UUID) -> ZaakAutorisatieGegevens): ZOEKOBJECT?
}
