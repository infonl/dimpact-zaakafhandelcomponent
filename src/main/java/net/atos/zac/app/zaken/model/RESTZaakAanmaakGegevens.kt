/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import net.atos.zac.app.bag.model.RESTBAGObject
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTZaakAanmaakGegevens(
    var zaak: RESTZaak,

    var inboxProductaanvraag: RESTInboxProductaanvraag? = null,

    var bagObjecten: List<RESTBAGObject>? = null
)
