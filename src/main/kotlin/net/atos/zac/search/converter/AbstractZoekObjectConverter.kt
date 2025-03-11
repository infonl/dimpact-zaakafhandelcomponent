/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.search.converter

import net.atos.zac.search.model.zoekobject.ZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType

abstract class AbstractZoekObjectConverter<ZOEKOBJECT : ZoekObject> {
    abstract fun supports(objectType: ZoekObjectType): Boolean

    abstract fun convert(id: String): ZOEKOBJECT?
}
