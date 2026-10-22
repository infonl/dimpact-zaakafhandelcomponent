/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.inboxdocument.model

import nl.info.zac.app.shared.RestListParameters
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestInboxDocumentListParameters : RestListParameters() {
    var titel: String? = null

    var identificatie: String? = null

    var creatiedatum: RestDatumRange? = null
}
