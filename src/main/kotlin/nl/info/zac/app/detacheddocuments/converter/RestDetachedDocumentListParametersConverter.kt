/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments.converter

import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.shared.applyCommonParametersTo
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.search.model.DatumRange

fun RestDetachedDocumentListParameters.toDetachedDocumentListParameters() =
    DetachedDocumentListParameters().also { params ->
        this.applyCommonParametersTo(params)
        params.reden = this.reden
        params.titel = this.titel
        params.zaakID = this.zaakID
        this.creatiedatum?.let { creatiedatum ->
            if (creatiedatum.hasValue()) {
                params.creatiedatum = DatumRange(van = creatiedatum.van, tot = creatiedatum.tot)
            }
        }
        this.ontkoppeldDoor?.let { ontkoppeldDoor ->
            params.ontkoppeldDoor = ontkoppeldDoor.id
        }
        this.ontkoppeldOp?.let { ontkoppeldOp ->
            if (ontkoppeldOp.hasValue()) {
                params.ontkoppeldOp = DatumRange(van = ontkoppeldOp.van, tot = ontkoppeldOp.tot)
            }
        }
    }
