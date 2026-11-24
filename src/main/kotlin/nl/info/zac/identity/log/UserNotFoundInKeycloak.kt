/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.log

import nl.info.zac.identity.IdentityLookupContext
import nl.info.zac.identity.IdentityLookupSource
import nl.info.zac.log.LogEvent
import java.util.UUID

// Without the employee: Keycloak does not know the user, so its Keycloak user ID cannot be looked up either
@Suppress("LongParameterList")
class UserNotFoundInKeycloak(
    val source: IdentityLookupSource?,
    val zaakIdentificatie: String?,
    val zaakUuid: UUID?,
    val taakId: String?,
    val informatieobjectIdentificatie: String?,
    val signaleringId: Long?,
    val noteId: Long?
) : LogEvent("User could not be found in Keycloak; returning the username only") {
    constructor(context: IdentityLookupContext?) : this(
        source = context?.source,
        zaakIdentificatie = context?.zaakIdentificatie,
        zaakUuid = context?.zaakUuid,
        taakId = context?.taakId,
        informatieobjectIdentificatie = context?.informatieobjectIdentificatie,
        signaleringId = context?.signaleringId,
        noteId = context?.noteId
    )
}
