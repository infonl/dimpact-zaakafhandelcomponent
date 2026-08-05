/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.zac.app.zaak.model.RestZaakResultaat
import nl.info.zac.app.zaak.model.toRestResultaatType
import java.net.URI

@Suppress("NestedBlockDepth")
class RestZaakResultaatConverter @Inject constructor(
    val zrcClientService: ZrcClientService,
    val ztcClientService: ZtcClientService
) {
    fun convert(resultaatURI: URI, zaaktypeURI: URI): RestZaakResultaat =
        zrcClientService.readResultaat(resultaatURI).let { resultaat ->
            ztcClientService.readResultaattype(resultaat.resultaattype).let {
                val resultaat = RestZaakResultaat(
                    toelichting = it.toelichting,
                    resultaattype = it.toRestResultaatType()
                )
                if (resultaat.resultaattype?.bronArchiefprocedure?.afleidingswijze == AfleidingswijzeEnum.EIGENSCHAP) {
                    val eigenschappen = ztcClientService.readEigenschappen(zaaktypeURI)
                    eigenschappen
                        .find { it.naam == resultaat.resultaattype?.bronArchiefprocedure?.datumkenmerk }
                        ?.definitie
                        ?.takeIf { it.isNotBlank() }
                        ?.let { resultaat.resultaattype?.datumKenmerkOmschrijving = it }
                }
                return resultaat
            }
        }
}
