/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import java.util.UUID

/**
 * Where a user or group is looked up, and the ids of the objects that refer to it, for the log record that is written
 * when Keycloak does not know that user or group.
 */
data class IdentityLookupContext(
    val source: IdentityLookupSource,
    val zaakIdentificatie: String? = null,
    val zaakUuid: UUID? = null,
    val taakId: String? = null,
    val informatieobjectIdentificatie: String? = null,
    val signaleringId: Long? = null,
    val noteId: Long? = null
)
