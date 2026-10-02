/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.policy.model

import nl.info.zac.app.policy.model.RestDocumentRechten

fun createRestDocumentRechten() = RestDocumentRechten(
    canLezen = true,
    canWijzigen = true,
    canVerwijderen = true,
    canVergrendelen = true,
    canOntgrendelen = true,
    canOndertekenen = true,
    canToevoegenNieuweVersie = true,
    canConverteren = true
)
