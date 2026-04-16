/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.Valid
import net.atos.zac.app.bag.model.RESTBAGObject
import nl.info.zac.app.productaanvraag.model.RestInboxProductaanvraag
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTZaakAanmaakGegevens(
    @field:Valid
    var zaak: RestZaakCreateData,

    var inboxProductaanvraag: RestInboxProductaanvraag? = null,

    var bagObjecten: List<RESTBAGObject>? = null
)
