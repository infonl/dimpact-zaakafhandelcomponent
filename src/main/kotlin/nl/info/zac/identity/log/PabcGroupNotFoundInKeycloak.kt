/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.log

import nl.info.zac.log.LogEvent

class PabcGroupNotFoundInKeycloak(
    val groupId: String
) : LogEvent("Group from the PABC could not be found in Keycloak")
