/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
abstract class AbstractRestZoekObject(
    open val id: String? = null,
    open val type: ZoekObjectType? = null,
    open val identificatie: String? = null
)
