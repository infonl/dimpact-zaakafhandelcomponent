/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType

abstract class AbstractZoekObjectConverter<ZOEKOBJECT : ZoekObject> {
    abstract fun supports(objectType: ZoekObjectType): Boolean

    abstract fun convert(id: String): ZOEKOBJECT?
}
