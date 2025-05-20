/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.model

data class RestDocumentRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val verwijderen: Boolean,
    val vergrendelen: Boolean,
    val ontgrendelen: Boolean,
    val ondertekenen: Boolean,
    val toevoegenNieuweVersie: Boolean
)
