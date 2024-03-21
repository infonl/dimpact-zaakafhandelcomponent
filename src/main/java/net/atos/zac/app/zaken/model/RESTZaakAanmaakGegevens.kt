/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import jakarta.validation.Valid
import net.atos.zac.app.bag.model.RESTBAGObject
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag

data class RESTZaakAanmaakGegevens(
    @Valid
    val zaak: RESTZaak,

    val inboxProductaanvraag: RESTInboxProductaanvraag? = null,

    val bagObjecten: List<RESTBAGObject>? = null
)
