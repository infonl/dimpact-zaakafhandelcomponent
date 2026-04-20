/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.productaanvraag.model

import net.atos.zac.app.shared.RESTResultaat
import nl.info.zac.util.AllOpen

@AllOpen
class RestInboxProductaanvraagResultaat(
    resultaten: List<RestInboxProductaanvraag>,
    aantalTotaal: Long
) : RESTResultaat<RestInboxProductaanvraag>(resultaten, aantalTotaal) {
    var filterType: List<String> = emptyList()
}
