/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.model.createNaamPersoon
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.model.createAdres
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createVerblijfadresBinnenland
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createOrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheidForReads
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.IdentityService
import java.net.URI
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class MailTemplateHelperTest : BehaviorSpec({
    val brpClientService = mockk<BrpClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val identityService = mockk<IdentityService>()
    val kvkClientService = mockk<KvkClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val mailTemplateHelper = MailTemplateHelper(
        brpClientService,
        configuratieService,
        identityService,
        kvkClientService,
        zgwApiService,
        zrcClientService,
        ztcClientService
    )

    Context("The 'GEMEENTE' variable can be resolved from a string") {
        Given("A text containing the {GEMEENTE} placeholder") {
            val gemeenteNaam = "fakeGemeenteNaam"
            every { configuratieService.readGemeenteNaam() } returns gemeenteNaam

            When("resolveGemeenteVariable is called") {
                val resolvedText = mailTemplateHelper.resolveGemeenteVariable("Welcome to {GEMEENTE}!")

                Then("the {GEMEENTE} placeholder should be replaced with the gemeente name") {
                    resolvedText shouldBe "Welcome to $gemeenteNaam!"
                }
            }
        }

        Given("A text without the {GEMEENTE} placeholder") {
            every { configuratieService.readGemeenteNaam() } returns "fakeGemeenteNaam"

            When("resolveGemeenteVariable is called") {
                val resolvedText = mailTemplateHelper.resolveGemeenteVariable("fakeText")

                Then("the text should remain unchanged") {
                    resolvedText shouldBe "fakeText"
                }
            }
        }

        Given("An gemeente name with HTML special characters") {
            every { configuratieService.readGemeenteNaam() } returns "\"fake\" &amp; \"gemeente naam\""

            When("resolveGemeenteVariable is called") {
                val resolvedText = mailTemplateHelper.resolveGemeenteVariable("Welcome to {GEMEENTE}!")

                Then(
                    "the {GEMEENTE} placeholder should be replaced and the HTML special characters should be escaped"
                ) {
                    resolvedText shouldBe "Welcome to &quot;fake&quot; &amp;amp; &quot;gemeente naam&quot;!"
                }
            }
        }
    }

    Context("Mail template variables can be resolved for a zaak") {
        Given("A zaak without an initiator") {
            val zaakType = createZaakType()
            val zaak = createZaak(
                zaakTypeURI = zaakType.url,
                status = URI("https://example.com/fakeStatus"),
                startDate = LocalDate.of(2021, 10, 12)
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = "fakeStatusTypeDescription")
            val zaakTonenURL = URI("https://example.com/fakeURL")
            val groupName = "fakeGroupName"
            val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheidForReads(
                organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(
                    naam = groupName
                )
            )
            val medewerkerVoorletters = "fakeVoorletters"
            val medewerkerAchternaam = "fakeAchternaam"
            val rolMedewerker = createRolMedewerker(
                medewerkerIdentificatie = createMedewerkerIdentificatie(
                    voorletters = medewerkerVoorletters,
                    achternaam = medewerkerAchternaam
                )
            )
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
            every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zgwApiService.findGroepForZaak(zaak) } returns rolOrganisatorischeEenheid
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerker
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null

            When("the variables are resolved with a text containing placeholders") {
                val resolvedText = mailTemplateHelper.resolveZaakVariables(
                    "fakeText, {ZAAK_NUMMER}, {ZAAK_URL}, {ZAAK_TYPE}, {ZAAK_STATUS}, {ZAAK_STARTDATUM}, " +
                        "{ZAAK_BEHANDELAAR_GROEP}, {ZAAK_BEHANDELAAR_MEDEWERKER}, {ZAAK_INITIATOR}",
                    zaak
                )

                Then(
                    """
                        the variables in the provided text should be replaced by the correct values from the zaak, 
                        and the initiator variable should be replaced with 'Onbekend'
                        """
                ) {
                    resolvedText shouldBe "fakeText, ${zaak.identificatie}, $zaakTonenURL, ${zaakType.omschrijving}, " +
                        "${statusType.omschrijving}, 12-10-2021, $groupName, $medewerkerVoorletters $medewerkerAchternaam, " +
                        "Onbekend"
                }
            }
        }

        Given(
            """
            A zaak with an initiator of role natuurlijk persoon with a BSN and a persoon with a name and a verblijfplaats
            """
        ) {
            val zaakType = createZaakType()
            val zaakIdentificatie = "fakeZaakIdentificatie"
            val zaak = createZaak(
                identificatie = zaakIdentificatie,
                zaakTypeURI = zaakType.url,
                status = URI("https://example.com/fakeStatus"),
                startDate = LocalDate.of(2021, 10, 12)
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = "fakeStatusTypeDescription")
            val zaakTonenURL = URI("https://example.com/fakeURL")
            val bsn = "123456789"
            val rolNietNatuurlijkPersoon = createRolNatuurlijkPersoon(
                natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(
                    bsn = bsn
                )
            )
            val persoonFullName = "fakeFullName"
            val streetName = "fakeStreetName"
            val houseNumber = 123
            val zipcode = "fakeZipcode"
            val city = "fakeCity"
            val persoon = createPersoon(
                bsn = bsn,
                name = createNaamPersoon(
                    fullName = persoonFullName
                ),
                verblijfplaats = createAdres(
                    verblijfAdresBinnenland = createVerblijfadresBinnenland(
                        officieleStraatnaam = streetName,
                        huisnummer = houseNumber,
                        postcode = zipcode,
                        woonplaats = city
                    )
                )
            )
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
            every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
            every {
                brpClientService.retrievePersoon(bsn, "fakeZaakIdentificatie@E-mail verzenden")
            } returns persoon

            When(
                """
                the variables are resolved with a text containing a zaak initiator variable and a 
                zaak initiator address variable
                """
            ) {
                val resolvedText = mailTemplateHelper.resolveZaakVariables(
                    "fakeText, {ZAAK_INITIATOR}, {ZAAK_INITIATOR_ADRES}",
                    zaak
                )

                Then("the zaak initiator variable should be replaced with the person's full name") {
                    resolvedText shouldBe "fakeText, $persoonFullName, $streetName $houseNumber, $zipcode $city"
                }
            }
        }

        Given(
            """
            A zaak with an initiator of role natuurlijk persoon with a BSN and a persoon without a name and a verblijfplaats
            """
        ) {
            val zaakType = createZaakType()
            val zaakIdentificatie = "fakeZaakIdentificatie"
            val zaak = createZaak(
                identificatie = zaakIdentificatie,
                zaakTypeURI = zaakType.url,
                status = URI("https://example.com/fakeStatus"),
                startDate = LocalDate.of(2021, 10, 12)
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = "fakeStatusTypeDescription")
            val zaakTonenURL = URI("https://example.com/fakeURL")
            val bsn = "123456789"
            val rolNietNatuurlijkPersoon = createRolNatuurlijkPersoon(
                natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(
                    bsn = bsn
                )
            )
            val persoon = createPersoon(
                bsn = bsn,
                verblijfplaats = null
            )
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
            every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
            every {
                brpClientService.retrievePersoon(bsn, "fakeZaakIdentificatie@E-mail verzenden")
            } returns persoon

            When("the variables are resolved with a text containing a placeholder for the zaak initiator") {
                val resolvedText = mailTemplateHelper.resolveZaakVariables(
                    "fakeText, {ZAAK_INITIATOR}, {ZAAK_INITIATOR_ADRES}",
                    zaak
                )

                Then(
                    "the text 'Onbekend' should be used for the persoon's name and no verblijfplaats should be replaced"
                ) {
                    resolvedText shouldBe "fakeText, Onbekend, "
                }
            }
        }

        Given("A zaak with an initiator of role niet-natuurlijk persoon with a vestigingnummer") {
            val zaakType = createZaakType()
            val zaak = createZaak(
                zaakTypeURI = zaakType.url,
                status = URI("https://example.com/fakeStatus"),
                startDate = LocalDate.of(2021, 10, 12)
            )
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = "fakeStatusTypeDescription")
            val zaakTonenURL = URI("https://example.com/fakeURL")
            val vestigingsnummer = "123456789"
            val rolNietNatuurlijkPersoon = createRolNietNatuurlijkPersoon(
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                    vestigingsnummer = vestigingsnummer
                )
            )
            val resultaatItem = createResultaatItem()
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
            every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
            every { kvkClientService.findVestiging(vestigingsnummer) } returns Optional.of(resultaatItem)

            When("the variables are resolved with a text containing a placeholder for the zaak initiator") {
                val resolvedText = mailTemplateHelper.resolveZaakVariables(
                    "fakeText, {ZAAK_INITIATOR}",
                    zaak
                )

                Then("the variables in the provided text should be replaced by the correct values from the zaak") {
                    resolvedText shouldBe "fakeText, ${resultaatItem.naam}"
                }
            }
        }

        Given("A document with a title and URL") {
            val enkelvoudigInformatieobjectUUID = UUID.randomUUID()
            val documentTitle = "fakeTitle"
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(
                uuid = enkelvoudigInformatieobjectUUID,
                title = documentTitle
            )
            val documentUriString = "https://example.com/fakeUrl/$enkelvoudigInformatieobjectUUID"
            every {
                configuratieService.informatieobjectTonenUrl(enkelvoudigInformatieobjectUUID)
            } returns URI(documentUriString)

            When("resolveVariabelen is called with a text containing placeholders") {
                val resolvedText = mailTemplateHelper.resolveEnkelvoudigInformatieObjectVariables(
                    "Title: {DOCUMENT_TITEL}, URL: {DOCUMENT_URL}, Link: {DOCUMENT_LINK}",
                    enkelvoudigInformatieObject
                )

                Then("the placeholders should be replaced with the document's title, URL, and link") {
                    resolvedText shouldBe "Title: $documentTitle, " +
                        "URL: $documentUriString, " +
                        "Link: Klik om naar het document " +
                        "<a href=\"$documentUriString\" " +
                        "title=\"de zaakafhandelcomponent...\">$documentTitle</a> te gaan."
                }
            }
        }
    }
})
