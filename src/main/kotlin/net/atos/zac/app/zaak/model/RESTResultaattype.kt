/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTResultaattype(
    var id: UUID,

    var naam: String? = null,

    var naamGeneriek: String? = null,

    var vervaldatumBesluitVerplicht: Boolean,

    var besluitVerplicht: Boolean,

    var toelichting: String? = null,

    var archiefNominatie: String? = null,

    var archiefTermijn: String? = null,

    var selectielijst: String? = null,
)
