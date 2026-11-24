/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.log

import nl.info.zac.identity.IdentityLookupContext
import nl.info.zac.identity.IdentityLookupSource
import nl.info.zac.log.LogEvent
import java.util.UUID

@Suppress("LongParameterList")
class GroupNotFoundInKeycloak(
    val groupId: String,
    val source: IdentityLookupSource?,
    val zaakIdentificatie: String?,
    val zaakUuid: UUID?,
    val taakId: String?,
    val informatieobjectIdentificatie: String?,
    val signaleringId: Long?,
    val noteId: Long?
) : LogEvent("Group could not be found in Keycloak; returning a placeholder") {
    constructor(groupId: String, context: IdentityLookupContext?) : this(
        groupId = groupId,
        source = context?.source,
        zaakIdentificatie = context?.zaakIdentificatie,
        zaakUuid = context?.zaakUuid,
        taakId = context?.taakId,
        informatieobjectIdentificatie = context?.informatieobjectIdentificatie,
        signaleringId = context?.signaleringId,
        noteId = context?.noteId
    )
}
