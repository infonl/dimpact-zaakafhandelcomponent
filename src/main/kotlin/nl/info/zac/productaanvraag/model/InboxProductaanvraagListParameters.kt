/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.model

import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.ListParameters

class InboxProductaanvraagListParameters : ListParameters() {
    var ontvangstdatum: DatumRange? = null
    var type: String? = null
    var initiatorID: String? = null

    override fun toString() =
        "InboxProductaanvraagListParameters{ontvangstdatum=$ontvangstdatum, type=$type, " +
            "initiatorID=$initiatorID, sorting=$sorting, paging=$paging}"
}
