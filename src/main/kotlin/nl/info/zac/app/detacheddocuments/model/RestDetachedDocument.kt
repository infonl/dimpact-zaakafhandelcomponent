/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestDetachedDocument(
    var id: Long = 0,
    var documentUUID: UUID? = null,
    var documentID: String? = null,
    var informatieobjectTypeUUID: UUID? = null,
    var zaakID: String? = null,
    var creatiedatum: LocalDate? = null,
    var titel: String? = null,
    var bestandsnaam: String? = null,
    var ontkoppeldDoor: RestUser? = null,
    var ontkoppeldOp: ZonedDateTime? = null,
    var reden: String? = null,
    var isVergrendeld: Boolean = false
)
