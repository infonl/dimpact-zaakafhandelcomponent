/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

abstract class AbstractZoekObjectConverter<ZOEKOBJECT : ZoekObject> {
    abstract fun supports(objectType: ZoekObjectType): Boolean

    abstract fun convert(id: String): ZOEKOBJECT?

    /**
     * Converts [id], looking up the zaakspecifiek geautoriseerd flag through [isZaakspecifiekGeautoriseerd]
     * instead of always deriving it directly, so that callers converting several zoekobjecten linked to
     * the same zaak (e.g. [nl.info.zac.search.IndexingService]) can share one memoized lookup.
     */
    abstract fun convert(id: String, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): ZOEKOBJECT?
}
