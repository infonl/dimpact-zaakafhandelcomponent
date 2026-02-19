/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.model

import net.atos.zac.app.shared.RESTListParameters
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RESTInboxDocumentListParameters : RESTListParameters() {
    var titel: String? = null

    var identificatie: String? = null

    var creatiedatum: RestDatumRange? = null
}
