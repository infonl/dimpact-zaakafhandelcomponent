/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.klant

abstract class RestKlant {
    abstract var emailadres: String?
    abstract var naam: String?
    abstract var telefoonnummer: String?
    abstract fun getIdentificatieType(): IdentificatieType?
    abstract fun getIdentificatie(): String?
}
