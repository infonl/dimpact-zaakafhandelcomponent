/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.productaanvraag.converter

import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraagListParameters
import nl.info.zac.app.shared.applyCommonParametersTo
import nl.info.zac.productaanvraag.model.InboxProductaanvraagListParameters

fun RestInboxProductaanvraagListParameters.toInboxProductaanvraagListParameters() =
    InboxProductaanvraagListParameters().also { params ->
        this.applyCommonParametersTo(params)
        params.type = this.type
        params.initiatorID = this.initiatorID
        params.ontvangstdatum = this.ontvangstdatum?.toDateRange()
    }
