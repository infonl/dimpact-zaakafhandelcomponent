/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.productaanvragen.converter

import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraagListParameters
import nl.info.zac.app.shared.applyCommonParametersTo
import nl.info.zac.productaanvraag.model.InboxProductaanvraagListParameters
import nl.info.zac.search.model.DatumRange

fun RestInboxProductaanvraagListParameters.toInboxProductaanvraagListParameters() =
    InboxProductaanvraagListParameters().also { params ->
        this.applyCommonParametersTo(params)
        params.type = this.type
        params.initiatorID = this.initiatorID
        this.ontvangstdatum?.let { ontvangstdatum ->
            if (ontvangstdatum.hasValue()) {
                params.ontvangstdatum = DatumRange(van = ontvangstdatum.van, tot = ontvangstdatum.tot)
            }
        }
    }
