/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import net.atos.zac.app.mail.model.RESTMailGegevens
import java.util.UUID

class RESTUserEventListenerData {
    var zaakUuid: UUID? = null

    var planItemInstanceId: String? = null

    var actie: UserEventListenerActie? = null

    var zaakOntvankelijk: Boolean? = null

    var resultaatToelichting: String? = null

    var resultaattypeUuid: UUID? = null

    var restMailGegevens: RESTMailGegevens? = null
}
