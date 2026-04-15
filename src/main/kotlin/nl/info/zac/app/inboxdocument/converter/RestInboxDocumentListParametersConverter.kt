/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.inboxdocument.converter

import nl.info.zac.app.inboxdocument.model.RestInboxDocumentListParameters
import nl.info.zac.app.shared.applyCommonParametersTo
import nl.info.zac.document.inboxdocument.repository.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange

fun RestInboxDocumentListParameters.toInboxDocumentListParameters() =
    InboxDocumentListParameters().also { params ->
        this.applyCommonParametersTo(params)
        params.identificatie = this.identificatie
        params.titel = this.titel
        this.creatiedatum?.let { creatiedatum ->
            if (creatiedatum.hasValue()) {
                params.creatiedatum = DatumRange(van = creatiedatum.van, tot = creatiedatum.tot)
            }
        }
    }
