/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.Valid
import nl.info.zac.app.bag.model.RestBagObject
import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraag
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaakAanmaakGegevens(
    @field:Valid
    var zaak: RestZaakCreateData,

    var inboxProductaanvraag: RestInboxProductaanvraag? = null,

    var bagObjecten: List<RestBagObject>? = null
)
