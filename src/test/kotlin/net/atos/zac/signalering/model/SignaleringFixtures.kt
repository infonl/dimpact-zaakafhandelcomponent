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
