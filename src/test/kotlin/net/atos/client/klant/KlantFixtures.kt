package net.atos.client.klant

import net.atos.client.klant.model.DigitaalAdres
import java.net.URI
import java.util.UUID

fun createDigitalAddresses(
    phone: String,
    email: String
) = listOf(
    DigitaalAdres(UUID.randomUUID(), URI("dummyUri1")).apply {
        soortDigitaalAdres = "telefoon"
        adres = phone
    },
    DigitaalAdres(UUID.randomUUID(), URI("dummyUri2")).apply {
        soortDigitaalAdres = "email"
        adres = email
    }
)
