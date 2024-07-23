/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.zaak.model.RESTZaakResultaat
import java.net.URI

class RESTZaakResultaatConverter {
    @Inject
    private lateinit var zrcClientService: ZRCClientService

    @Inject
    private lateinit var restResultaattypeConverter: RESTResultaattypeConverter

    fun convert(resultaatURI: URI): RESTZaakResultaat {
        zrcClientService.readResultaat(resultaatURI).let {
            return RESTZaakResultaat(
                toelichting = it.toelichting,
                resultaattype = restResultaattypeConverter.convertResultaattypeUri(it.resultaattype)
            )
        }
    }
}
