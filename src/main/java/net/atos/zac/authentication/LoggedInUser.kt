/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.authentication

import net.atos.zac.identity.model.User

class LoggedInUser(
    id: String,
    firstName: String?,
    lastName: String?,
    displayName: String?,
    email: String?,
    val roles: Set<String>,
    val groupIds: Set<String>,
    /**
     * Lijst van zaaktype.omschrijving waarvoor de ingelogde gebruiker geautoriseerd is.
     * De waarde 'null' betekend dat de gebruiker geautoriseerd is voor elk zaaktype.
     */
    val geautoriseerdeZaaktypen: Set<String>? = null
) : User(id, firstName, lastName, displayName, email) {
    val isGeautoriseerdVoorAlleZaaktypen: Boolean
        get() = geautoriseerdeZaaktypen == null

    // TODO
//    fun getGeautoriseerdeZaaktypen(): Set<String> {
//        if (geautoriseerdeZaaktypen != null) {
//            return geautoriseerdeZaaktypen
//        } else {
//            throw IllegalStateException(
//                "Ingelogde gebruiker is geautoriseerd voor alle zaaktypen. Deze kunnen echter niet worden opgevraagd."
//            )
//        }
//    }

    fun isGeautoriseerdZaaktype(zaaktypeOmschrijving: String): Boolean =
        geautoriseerdeZaaktypen == null || geautoriseerdeZaaktypen.contains(zaaktypeOmschrijving)
}
