/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.zaak.model.RestZaakResultaat
import net.atos.zac.app.zaak.model.toRestResultaatType
import java.net.URI

class RestZaakResultaatConverter @Inject constructor(
    val zrcClientService: ZrcClientService,
    val ztcClientService: ZtcClientService
) {
    fun convert(resultaatURI: URI): RestZaakResultaat =
        zrcClientService.readResultaat(resultaatURI).let { resultaat ->
            ztcClientService.readResultaattype(resultaat.resultaattype).let {
                RestZaakResultaat(
                    toelichting = it.toelichting,
                    resultaattype = it.toRestResultaatType()
                )
            }
        }
}
