/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.client.pabc.ROLE_NAME_BRP_ZOEKEN
import nl.info.zac.authentication.LoggedInUser

data class RestBrpRechten(
    val zoeken: Boolean
)

fun LoggedInUser.toRestBrpRechten() = RestBrpRechten(
    zoeken = this.overallRoles.contains(ROLE_NAME_BRP_ZOEKEN) || this.brpGemeenten.any(),
)
