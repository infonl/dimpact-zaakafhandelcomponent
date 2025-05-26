/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.DocumentRechten

data class RestDocumentRechten(
    val lezen: Boolean,
    val wijzigen: Boolean,
    val verwijderen: Boolean,
    val vergrendelen: Boolean,
    val ontgrendelen: Boolean,
    val ondertekenen: Boolean,
    val toevoegenNieuweVersie: Boolean
)

fun DocumentRechten.toRestDocumentRechten() = RestDocumentRechten(
    lezen = this.lezen,
    wijzigen = this.wijzigen,
    ontgrendelen = this.ontgrendelen,
    vergrendelen = this.vergrendelen,
    verwijderen = this.verwijderen,
    ondertekenen = this.ondertekenen,
    toevoegenNieuweVersie = this.toevoegenNieuweVersie
)
