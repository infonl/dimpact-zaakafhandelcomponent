/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
abstract class AbstractRestZoekObject {
    var id: String? = null

    var type: ZoekObjectType? = null

    var identificatie: String? = null
}
