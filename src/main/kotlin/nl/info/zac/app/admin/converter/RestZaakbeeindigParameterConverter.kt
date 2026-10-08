/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden
import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter.convertZaakbeeindigReden
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.app.admin.model.RestZaakbeeindigParameter
import nl.info.zac.app.zaak.model.toRestResultaatType

class RestZaakbeeindigParameterConverter @Inject constructor(
    private val resultaattypeReferenceService: ResultaattypeReferenceService
) {
    fun convertZaakbeeindigParameters(
        zaakbeeindigRedenen: Set<ZaaktypeCompletionParameters>
    ): List<RestZaakbeeindigParameter> =
        zaakbeeindigRedenen.mapNotNull { convertZaakbeeindigParameter(it) }

    fun toZaaktypeCompletionParameters(
        restZaakbeeindigParameters: List<RestZaakbeeindigParameter>
    ): List<ZaaktypeCompletionParameters> =
        restZaakbeeindigParameters.map { restZaakbeeindigParameter ->
            ZaaktypeCompletionParameters().apply {
                id = restZaakbeeindigParameter.id
                zaakbeeindigReden = convertRESTZaakbeeindigReden(restZaakbeeindigParameter.zaakbeeindigReden)
                resultaattypeOmschrijving =
                    resultaattypeReferenceService.readOmschrijving(restZaakbeeindigParameter.resultaattype.id)
            }
        }

    private fun convertZaakbeeindigParameter(
        zaaktypeCompletionParameters: ZaaktypeCompletionParameters
    ): RestZaakbeeindigParameter? =
        resultaattypeReferenceService.findResultaattype(zaaktypeCompletionParameters)?.let {
            RestZaakbeeindigParameter(
                id = zaaktypeCompletionParameters.id,
                zaakbeeindigReden = convertZaakbeeindigReden(zaaktypeCompletionParameters.zaakbeeindigReden),
                resultaattype = it.toRestResultaatType()
            )
        }
}
