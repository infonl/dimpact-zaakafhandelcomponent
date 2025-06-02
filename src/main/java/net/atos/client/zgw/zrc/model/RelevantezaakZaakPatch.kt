/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbNillable
import nl.info.client.zgw.zrc.model.generated.RelevanteZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.util.AllOpen

class RelevantezaakZaakPatch(@field:JsonbNillable private val relevanteAndereZaken: List<RelevanteZaak>?) : Zaak() {
    override fun getRelevanteAndereZaken() = relevanteAndereZaken
}
