/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class UserData(
    var id: String,

    var rollen: Set<String>? = null,

    /**
     * Lijst van zaaktype.omschrijving waarvoor de ingelogde gebruiker geautoriseerd is.
     * De waarde null betekend dat de gebruiker geautoriseerd is voor elk zaaktype.
     */
    var zaaktypen: Set<String>? = null
)
