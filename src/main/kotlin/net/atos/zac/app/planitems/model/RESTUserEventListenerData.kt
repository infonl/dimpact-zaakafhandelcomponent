/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import net.atos.zac.app.mail.model.RESTMailGegevens
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
data class RESTUserEventListenerData(
    var zaakUuid: UUID? = null,

    var planItemInstanceId: String? = null,

    var actie: UserEventListenerActie,

    var zaakOntvankelijk: Boolean = false,

    var resultaatToelichting: String? = null,

    var resultaattypeUuid: UUID,

    var restMailGegevens: RESTMailGegevens? = null
)
