/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.inboxdocument.converter

import nl.info.zac.app.inboxdocument.model.RestInboxDocumentListParameters
import nl.info.zac.app.shared.RESTListParametersConverter
import nl.info.zac.document.inboxdocument.repository.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestInboxDocumentListParametersConverter :
    RESTListParametersConverter<InboxDocumentListParameters, RestInboxDocumentListParameters>() {
    override fun doConvert(
        listParameters: InboxDocumentListParameters,
        restListParameters: RestInboxDocumentListParameters
    ) {
        listParameters.identificatie = restListParameters.identificatie
        listParameters.titel = restListParameters.titel
        restListParameters.creatiedatum?.let { creatiedatum ->
            if (creatiedatum.hasValue()) {
                listParameters.creatiedatum = DatumRange(creatiedatum.van, creatiedatum.tot)
            }
        }
    }

    override fun getListParameters() = InboxDocumentListParameters()
}
