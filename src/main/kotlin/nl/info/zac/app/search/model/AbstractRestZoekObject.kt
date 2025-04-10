/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
abstract class AbstractRestZoekObject(
    open val id: String? = null,
    open val type: ZoekObjectType? = null,
    open val identificatie: String? = null
)
