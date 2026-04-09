/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.converter

import net.atos.zac.app.shared.RESTListParametersConverter
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestDetachedDocumentListParametersConverter :
    RESTListParametersConverter<DetachedDocumentListParameters, RestDetachedDocumentListParameters>() {
    override fun doConvert(
        listParameters: DetachedDocumentListParameters?,
        restListParameters: RestDetachedDocumentListParameters?
    ) {
        if (listParameters == null || restListParameters == null) return

        listParameters.reden = restListParameters.reden
        listParameters.titel = restListParameters.titel

        restListParameters.creatiedatum?.let { creatiedatum ->
            if (creatiedatum.hasValue()) {
                listParameters.creatiedatum = DatumRange(van = creatiedatum.van, tot = creatiedatum.tot)
            }
        }

        restListParameters.ontkoppeldDoor?.let { ontkoppeldDoor ->
            listParameters.ontkoppeldDoor = ontkoppeldDoor.id
        }

        restListParameters.ontkoppeldOp?.let { ontkoppeldOp ->
            if (ontkoppeldOp.hasValue()) {
                listParameters.ontkoppeldOp = DatumRange(van = ontkoppeldOp.van, tot = ontkoppeldOp.tot)
            }
        }

        listParameters.zaakID = restListParameters.zaakID
    }

    override fun getListParameters() = DetachedDocumentListParameters()
}
