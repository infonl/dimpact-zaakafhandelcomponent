/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

fun createRestTaakRechten() = RestTaakRechten(
    lezen = true,
    wijzigen = true,
    toekennen = true,
    toevoegenDocument = true
)
