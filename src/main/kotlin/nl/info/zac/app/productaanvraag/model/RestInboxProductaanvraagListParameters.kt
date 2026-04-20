/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag.model

import net.atos.zac.app.shared.RESTListParameters
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestInboxProductaanvraagListParameters : RESTListParameters() {
    var type: String? = null
    var ontvangstdatum: RestDatumRange? = null
    var initiatorID: String? = null
}
