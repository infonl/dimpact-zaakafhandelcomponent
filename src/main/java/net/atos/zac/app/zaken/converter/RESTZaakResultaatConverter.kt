/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.zaken.model.RESTZaakResultaat
import java.net.URI

class RESTZaakResultaatConverter {
    @Inject
    private lateinit var zrcClientService: ZRCClientService

    @Inject
    private lateinit var restResultaattypeConverter: RESTResultaattypeConverter

    fun convert(resultaatURI: URI): RESTZaakResultaat {
        val resultaat = zrcClientService.readResultaat(resultaatURI)
        val restZaakResultaat = RESTZaakResultaat()
        restZaakResultaat.toelichting = resultaat.toelichting
        restZaakResultaat.resultaattype = restResultaattypeConverter.convertResultaattypeUri(resultaat.resultaattype)
        return restZaakResultaat
    }
}
