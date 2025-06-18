/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.model

import net.atos.zac.app.mail.model.RESTMailGegevens
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
data class RESTUserEventListenerData(
    var zaakUuid: UUID,

    var planItemInstanceId: String? = null,

    var actie: UserEventListenerActie,

    var zaakOntvankelijk: Boolean = false,

    var resultaatToelichting: String? = null,

    var resultaattypeUuid: UUID? = null,

    var restMailGegevens: RESTMailGegevens? = null
)
