/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.productaanvragen.converter

import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraagListParameters
import net.atos.zac.productaanvraag.model.InboxProductaanvraagListParameters
import nl.info.zac.app.shared.RESTListParametersConverter
import nl.info.zac.search.model.DatumRange

class RESTInboxProductaanvraagListParametersConverter :
    RESTListParametersConverter<InboxProductaanvraagListParameters, RESTInboxProductaanvraagListParameters>() {

    override fun doConvert(
        listParameters: InboxProductaanvraagListParameters,
        restListParameters: RESTInboxProductaanvraagListParameters
    ) {
        listParameters.apply {
            type = restListParameters.type
            initiatorID = restListParameters.initiatorID
            if (restListParameters.ontvangstdatum != null && restListParameters.ontvangstdatum.hasValue()) {
                ontvangstdatum = DatumRange(
                    van = restListParameters.ontvangstdatum.van,
                    tot = restListParameters.ontvangstdatum.tot
                )
            }
        }
    }

    override fun getListParameters() = InboxProductaanvraagListParameters()
}
