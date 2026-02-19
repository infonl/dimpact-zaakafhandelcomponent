/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.inboxdocumenten.model

import java.time.LocalDate
import java.util.UUID

class RESTInboxDocument {
    var id: Long = 0

    var enkelvoudiginformatieobjectUUID: UUID? = null

    var enkelvoudiginformatieobjectID: String? = null

    var informatieobjectTypeUUID: UUID? = null

    var creatiedatum: LocalDate? = null

    var titel: String? = null

    var bestandsnaam: String? = null
}
