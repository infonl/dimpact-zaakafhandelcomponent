/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import net.atos.zac.admin.model.ZaakAfzender
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTZaakAfzender(
    var id: Long? = null,
    var defaultMail: Boolean = false,
    // TODO: should this be non-nullable?
    var mail: String? = null,
    var suffix: String? = null,
    var replyTo: String? = null,
    var speciaal: Boolean = false
) {
    constructor(speciaal: ZaakAfzender.Speciaal) : this(
        mail = speciaal.name,
        speciaal = true
    )
}
