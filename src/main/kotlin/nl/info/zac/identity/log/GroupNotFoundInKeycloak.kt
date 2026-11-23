/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.log

import nl.info.zac.log.LogEvent

class GroupNotFoundInKeycloak(
    val groupId: String,
    val context: String?
) : LogEvent("Group could not be found in Keycloak; returning a placeholder")
