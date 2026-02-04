/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden
import net.atos.zac.app.admin.model.RestZaakbeeindigParameter
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.app.zaak.model.toRestResultaatType

class RestZaakbeeindigParameterConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    fun convertZaakbeeindigParameters(
        zaakbeeindigRedenen: Set<ZaaktypeCompletionParameters>
    ): List<RestZaakbeeindigParameter> =
        zaakbeeindigRedenen.map { convertZaakbeeindigParameter(it) }

    private fun convertZaakbeeindigParameter(
        zaaktypeCompletionParameters: ZaaktypeCompletionParameters
    ): RestZaakbeeindigParameter = RestZaakbeeindigParameter(
        id = zaaktypeCompletionParameters.id,
        zaakbeeindigReden = convertZaakbeeindigReden(
            zaaktypeCompletionParameters.zaakbeeindigReden
        ),
        resultaattype = ztcClientService.readResultaattype(
            zaaktypeCompletionParameters.resultaattype
        ).toRestResultaatType()
    )
}

        fun convertRESTZaakbeeindigParameters(
            restZaakbeeindigParameters: List<RestZaakbeeindigParameter>
        ) = restZaakbeeindigParameters.map { convertRESTZaakbeeindigParameter(it) }

        private fun convertRESTZaakbeeindigParameter(
            restZaakbeeindigParameter: RestZaakbeeindigParameter
        ) = ZaaktypeCompletionParameters().apply {
            id = restZaakbeeindigParameter.id
            zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(
                restZaakbeeindigParameter.zaakbeeindigReden
            )
            resultaattype = restZaakbeeindigParameter.resultaattype.id
        }

