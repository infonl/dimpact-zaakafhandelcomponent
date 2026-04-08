/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import net.atos.zac.app.shared.RESTListParameters
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestDetachedDocumentListParameters : RESTListParameters() {
    var titel: String? = null

    var reden: String? = null

    var creatiedatum: RestDatumRange? = null

    var ontkoppeldDoor: RestUser? = null

    var ontkoppeldOp: RestDatumRange? = null

    var zaakID: String? = null
}
