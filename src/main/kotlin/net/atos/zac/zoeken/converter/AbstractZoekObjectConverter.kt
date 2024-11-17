/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.converter

import net.atos.zac.zoeken.model.zoekobject.ZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType

abstract class AbstractZoekObjectConverter<ZOEKOBJECT : ZoekObject> {
    abstract fun supports(objectType: ZoekObjectType): Boolean

    abstract fun convert(id: String): ZOEKOBJECT?
}
