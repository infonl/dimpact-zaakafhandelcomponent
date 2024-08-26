/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.zaak.model.toRestResultaatType
import java.net.URI

class RESTResultaattypeConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    fun convertResultaattypeUri(resultaattypeURI: URI) =
        ztcClientService.readResultaattype(resultaattypeURI).toRestResultaatType()
}
