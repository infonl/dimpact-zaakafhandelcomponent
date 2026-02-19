/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.converter

import net.atos.zac.app.inboxdocumenten.model.RESTInboxDocumentListParameters
import net.atos.zac.app.shared.RESTListParametersConverter
import net.atos.zac.documenten.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange

class RESTInboxDocumentListParametersConverter :
    RESTListParametersConverter<InboxDocumentListParameters?, RESTInboxDocumentListParameters?>() {
    override fun doConvert(
        listParameters: InboxDocumentListParameters?,
        restListParameters: RESTInboxDocumentListParameters?
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
