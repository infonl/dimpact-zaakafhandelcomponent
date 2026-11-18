/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments.converter

import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.shared.applyCommonParametersTo
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters

fun RestDetachedDocumentListParameters.toDetachedDocumentListParameters() =
    DetachedDocumentListParameters().also { params ->
        this.applyCommonParametersTo(params)
        params.reden = this.reden
        params.titel = this.titel
        params.zaakID = this.zaakID
        params.creatiedatum = this.creatiedatum?.toDateRange()
        this.ontkoppeldDoor?.let { ontkoppeldDoor ->
            params.ontkoppeldDoor = ontkoppeldDoor.username
        }
        params.ontkoppeldOp = this.ontkoppeldOp?.toDateRange()
    }
