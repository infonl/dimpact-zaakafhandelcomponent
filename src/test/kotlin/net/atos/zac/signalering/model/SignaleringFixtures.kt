/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering.model

import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createZaak

fun createSignalering(
    zaak: Zaak = createZaak()
) = Signalering().apply {
    this.type = SignaleringType().apply {
        type = SignaleringType.Type.ZAAK_OP_NAAM
        subjecttype = SignaleringSubject.ZAAK
    }
    this.setSubject(zaak)
}

@Suppress("LongParameterList")
fun createSignaleringInstellingen(
    id: Long = 1234L,
    type: SignaleringType = SignaleringType().apply {
        this.type = SignaleringType.Type.ZAAK_OP_NAAM
        this.subjecttype = SignaleringSubject.ZAAK
    },
    groep: String = "dummyGroep",
    medewerker: String = "dummyMedewerker",
    isDashboard: Boolean = true,
    isMail: Boolean = true,
) =
    SignaleringInstellingen().apply {
        this.id = id
        this.type = type
        this.groep = groep
        this.medewerker = medewerker
        this.isDashboard = isDashboard
        this.isMail = isMail
    }
