/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters

fun createRestListBedrijvenParameters(
    kvkNummer: String = "123456789",
) = RestListBedrijvenParameters(
    kvkNummer = kvkNummer,
)
