/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import nl.info.zac.policy.output.DocumentRechten

data class RestDocumentRechten(
    val canLezen: Boolean,
    val canWijzigen: Boolean,
    val canVerwijderen: Boolean,
    val canVergrendelen: Boolean,
    val canOntgrendelen: Boolean,
    val canOndertekenen: Boolean,
    val canToevoegenNieuweVersie: Boolean,
    val canConverteren: Boolean
)

fun DocumentRechten.toRestDocumentRechten() = RestDocumentRechten(
    canLezen = this.canLezen,
    canWijzigen = this.canWijzigen,
    canOntgrendelen = this.canOntgrendelen,
    canVergrendelen = this.canVergrendelen,
    canVerwijderen = this.canVerwijderen,
    canOndertekenen = this.canOndertekenen,
    canToevoegenNieuweVersie = this.canToevoegenNieuweVersie,
    canConverteren = this.canConverteren
)
