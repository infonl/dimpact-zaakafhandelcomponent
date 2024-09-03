package net.atos.client.klant

import net.atos.client.klant.model.DigitaalAdres
import java.net.URI
import java.util.UUID

fun createDigitalAddresses(
    phone: String,
    email: String
) = listOf(
    createDigitalAddress(
        uri = URI("https://example.com/dummyUr"),
        address = phone,
        soortDigitaalAdres = "telefoon"
    ),
    createDigitalAddress(
        uri = URI("https://example.com/dummyUri2"),
        address = email,
        soortDigitaalAdres = "email"
    )
)

fun createDigitalAddress(
    uuid: UUID = UUID.randomUUID(),
    uri: URI = URI("https://example.com/dummyUri"),
    address: String = "dummyAddress",
    soortDigitaalAdres: String = "telefoon"
) = DigitaalAdres(uuid, uri).apply {
    this.soortDigitaalAdres = soortDigitaalAdres
    this.adres = address
}
