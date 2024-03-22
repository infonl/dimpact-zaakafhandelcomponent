/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.util.UUID

class RESTResultaattype {
    var id: UUID? = null

    var naam: String? = null

    var naamGeneriek: String? = null

    var vervaldatumBesluitVerplicht: Boolean = false

    var besluitVerplicht: Boolean = false

    var toelichting: String? = null

    var archiefNominatie: String? = null

    var archiefTermijn: String? = null

    var selectielijst: String? = null
}
