/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.search.model.RestDateRange
import nl.info.zac.app.shared.RestListParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestDetachedDocumentListParameters : RestListParameters() {
    var titel: String? = null

    var reden: String? = null

    var creatiedatum: RestDateRange? = null

    var ontkoppeldDoor: RestUser? = null

    var ontkoppeldOp: RestDateRange? = null

    var zaakID: String? = null
}
