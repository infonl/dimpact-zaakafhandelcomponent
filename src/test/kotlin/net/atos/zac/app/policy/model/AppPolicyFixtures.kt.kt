/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.policy.model

fun createRestDocumentRechten() = RestDocumentRechten(
    lezen = true,
    wijzigen = true,
    verwijderen = true,
    vergrendelen = true,
    ontgrendelen = true,
    ondertekenen = true,
    toevoegenNieuweVersie = true
)
