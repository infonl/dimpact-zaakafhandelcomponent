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
    val telephoneNumber = this.find { it.soortDigitaalAdres == TELEFOONNUMMER }?.adres
    val emailAddress = this.find { it.soortDigitaalAdres == EMAIL }?.adres
    return ContactDetails(telephoneNumber, emailAddress)
}
