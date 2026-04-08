/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.document.detacheddocument.model

import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.ListParameters

class DetachedDocumentListParameters : ListParameters() {
    var titel: String? = null

    var creatiedatum: DatumRange? = null

    var zaakID: String? = null

    var ontkoppeldDoor: String? = null

    var ontkoppeldOp: DatumRange? = null

    var reden: String? = null
}
