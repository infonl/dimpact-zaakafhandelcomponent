/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.model

import nl.info.zac.util.NoArgConstructor

// No-arg constructor is needed because somewhere we
// create an empty instance of this class.
// We should probably fix this at some point.
@NoArgConstructor
data class RestTaakRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val toekennen: Boolean,
    val toevoegenDocument: Boolean
)
