/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.model

import jakarta.validation.constraints.NotEmpty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestBehandelaarGroupsRequest(
    @field:NotEmpty
    var zaaktypeDescriptions: List<String>
)
