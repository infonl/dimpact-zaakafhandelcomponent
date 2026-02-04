/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.app.zaak.model.toRestResultaatType

class RESTZaakbeeindigParameterConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    fun convertZaakbeeindigParameters(
        zaakbeeindigRedenen: Set<ZaaktypeCompletionParameters>
    ): List<RESTZaakbeeindigParameter> =
        zaakbeeindigRedenen.map { convertZaakbeeindigParameter(it) }

    private fun convertZaakbeeindigParameter(
        zaaktypeCompletionParameters: ZaaktypeCompletionParameters
    ): RESTZaakbeeindigParameter = RESTZaakbeeindigParameter().apply {
        id = zaaktypeCompletionParameters.id
        zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden(
            zaaktypeCompletionParameters.zaakbeeindigReden
        )
        resultaattype = ztcClientService.readResultaattype(
            zaaktypeCompletionParameters.resultaattype
        ).toRestResultaatType()
    }
}

        fun convertRESTZaakbeeindigParameters(
            restZaakbeeindigParameters: List<RESTZaakbeeindigParameter>
        ): List<ZaaktypeCompletionParameters> =
            restZaakbeeindigParameters.map { convertRESTZaakbeeindigParameter(it) }

        private fun convertRESTZaakbeeindigParameter(
            restZaakbeeindigParameter: RESTZaakbeeindigParameter
        ): ZaaktypeCompletionParameters = ZaaktypeCompletionParameters().apply {
            id = restZaakbeeindigParameter.id
            zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(
                restZaakbeeindigParameter.zaakbeeindigReden
            )
            resultaattype = restZaakbeeindigParameter.resultaattype.id
        }

