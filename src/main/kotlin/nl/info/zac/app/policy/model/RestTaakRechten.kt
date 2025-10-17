/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.TaakRechten
import nl.info.zac.util.NoArgConstructor

// No-arg constructor is needed because instances of this class are currently
// deserialized because they are part of incoming ZAC API requests in [TaskRestService.kt].
// This is because currently the same classes are used for all CRUD operations in the ZAC API.
// We should refactor this at some point.
@NoArgConstructor
data class RestTaakRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val toekennen: Boolean,
    val toevoegenDocument: Boolean
)

fun TaakRechten.toRestTaakRechten() = RestTaakRechten(
    lezen = this.lezen,
    wijzigen = this.wijzigen,
    toekennen = this.toekennen,
    toevoegenDocument = this.toevoegenDocument
)
