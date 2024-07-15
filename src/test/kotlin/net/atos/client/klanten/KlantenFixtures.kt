package net.atos.client.klanten

import net.atos.client.klanten.model.Klant
import net.atos.client.klanten.model.Klant.SubjectTypeEnum
import java.net.URI
import java.util.UUID

@Suppress("LongParameterList")
fun createKlant(
    achternaam: String = "dummyAchternaam",
    bedrijfsnaam: String = "dummyBedrijfsnaam",
    bronorganisatie: String = "dummyBronorganisatie",
    emailadres: String = "dummy-email-adres@example.com",
    functie: String = "dummyFunctie",
    klantnummer: String = "dummyKlantnummer",
    subjectType: SubjectTypeEnum = SubjectTypeEnum.NATUURLIJK_PERSOON,
    telefoonnummer: String = "dummyTelefoonnummer",
    uri: URI = URI("http://example.com/klanten/${UUID.randomUUID()}"),
    voornaam: String = "dummyVoornaam",
    websiteUrl: URI = URI("http://example.com/dummyWebsiteUrl"),
) = Klant(uri).apply {
    this.achternaam = achternaam
    this.bedrijfsnaam = bedrijfsnaam
    this.bronorganisatie = bronorganisatie
    this.emailadres = emailadres
    this.functie = functie
    this.klantnummer = klantnummer
    this.subjectType = subjectType
    this.telefoonnummer = telefoonnummer
    this.voornaam = voornaam
    this.websiteUrl = websiteUrl
}
