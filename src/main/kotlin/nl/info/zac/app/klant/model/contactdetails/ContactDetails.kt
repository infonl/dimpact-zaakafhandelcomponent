/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.contactdetails

import nl.info.client.klanten.model.generated.DigitaalAdres
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum.EMAIL
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum.TELEFOONNUMMER

data class ContactDetails(
    val telephoneNumber: String?,
    val emailAddress: String?
)

fun List<DigitaalAdres>.toContactDetails(): ContactDetails {
    val telephoneNumber = this.filter { it.soortDigitaalAdres == TELEFOONNUMMER }
        .getStandaardDigitaalAdres()?.adres
    val emailAddress = this.filter { it.soortDigitaalAdres == EMAIL }
        .getStandaardDigitaalAdres()?.adres
    return ContactDetails(telephoneNumber, emailAddress)
}

/**
 * Returns the digital address marked as 'standaard adres' (default address) if available,
 * otherwise falls back to the first address in the list.
 */
fun List<DigitaalAdres>.getStandaardDigitaalAdres(): DigitaalAdres? =
    firstOrNull { it.isStandaardAdres == true } ?: firstOrNull()
