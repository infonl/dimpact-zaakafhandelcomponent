/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.NotitieRechten

data class RestNotitieRechten(
    val lezen: Boolean,
    val wijzigen: Boolean
)

fun NotitieRechten.toRestNotitieRechten() = RestNotitieRechten(
    lezen = this.lezen,
    wijzigen = this.wijzigen
)
