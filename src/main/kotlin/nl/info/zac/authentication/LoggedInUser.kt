/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import net.atos.zac.identity.model.User

@Suppress("LongParameterList")
class LoggedInUser(
    id: String,
    firstName: String?,
    lastName: String?,
    displayName: String?,
    email: String?,
    val roles: Set<String>,
    val groupIds: Set<String>,
    /**
     * List of zaaktypen for which the logged-in user is authorised, or 'null' if the user is authorised for all zaaktypen.
     */
    val geautoriseerdeZaaktypen: Set<String>? = null
) : User(id, firstName, lastName, displayName, email) {
    fun isAuthorisedForAllZaaktypen(): Boolean = geautoriseerdeZaaktypen == null

    fun isAuthorisedForZaaktype(zaaktypeOmschrijving: String): Boolean =
        geautoriseerdeZaaktypen == null || geautoriseerdeZaaktypen.contains(zaaktypeOmschrijving)
}
