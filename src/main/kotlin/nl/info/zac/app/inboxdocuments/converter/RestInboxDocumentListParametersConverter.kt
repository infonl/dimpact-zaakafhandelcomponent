/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.converter

import net.atos.zac.app.inboxdocumenten.model.RestInboxDocumentListParameters
import net.atos.zac.app.shared.RESTListParametersConverter
import net.atos.zac.documenten.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestInboxDocumentListParametersConverter :
    RESTListParametersConverter<InboxDocumentListParameters, RestInboxDocumentListParameters>() {
    override fun doConvert(
        listParameters: InboxDocumentListParameters?,
        restListParameters: RestInboxDocumentListParameters?
    ) {
        if (listParameters == null || restListParameters == null) return

        listParameters.identificatie = restListParameters.identificatie
        listParameters.titel = restListParameters.titel
        restListParameters.creatiedatum?.let { creatiedatum ->
            if (creatiedatum.hasValue()) {
                listParameters.creatiedatum = DatumRange(creatiedatum.van, creatiedatum.tot)
            }
        }
    }

    override fun getListParameters(): InboxDocumentListParameters {
        return InboxDocumentListParameters()
    }
}
