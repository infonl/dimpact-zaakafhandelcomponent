/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.zac.util.time.PeriodUtil
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.Period
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestResultaattype(
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

fun ResultaatType.toRestResultaatType() = RestResultaattype(
    id = this.url.extractUuid(),
    naam = this.omschrijving,
    naamGeneriek = this.omschrijvingGeneriek,
    toelichting = this.toelichting,
    archiefNominatie = this.archiefnominatie.name,
    archiefTermijn = this.archiefactietermijn?.let {
        PeriodUtil.format(Period.parse(it))
    },
    besluitVerplicht = this.isBesluitVerplicht(),
    vervaldatumBesluitVerplicht = this.isVervaldatumBesluitVerplicht()
)

fun List<ResultaatType>.toRestResultaatTypes(): List<RestResultaattype> = this.map { it.toRestResultaatType() }

fun ResultaatType.isBesluitVerplicht() = brondatumArchiefprocedure?.afleidingswijze?.let {
    it == AfleidingswijzeEnum.VERVALDATUM_BESLUIT || it == AfleidingswijzeEnum.INGANGSDATUM_BESLUIT
} == true

fun ResultaatType.isVervaldatumBesluitVerplicht() = brondatumArchiefprocedure?.afleidingswijze?.let {
    it == AfleidingswijzeEnum.VERVALDATUM_BESLUIT
} == true
