/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.documentcreation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.model.createAdres
import nl.info.client.brp.model.createAdressering
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createVerblijfadresBinnenland
import nl.info.client.brp.model.generated.Adres
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.exception.KvkClientException
import nl.info.client.kvk.model.createBasisprofiel
import nl.info.client.kvk.model.createBasisprofielAdres
import nl.info.client.kvk.model.createEigenaar
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.kvk.model.createVestiging
import nl.info.client.kvk.model.createVestigingsAdres
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.or.`object`.model.createModelObject
import nl.info.client.smartdocuments.model.document.AanvragerData
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createOpschorting
import nl.info.client.zgw.model.createResultaat
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolVestiging
import nl.info.client.zgw.model.createStatus
import nl.info.client.zgw.model.createVerlenging
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakobjectProductaanvraag
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.flowable.task.FlowableTaskService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.identity.model.getFullName
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact
import org.flowable.task.api.TaskInfo
import java.net.URI
import java.util.UUID

@Suppress("LargeClass")
class DocumentCreationDataServiceTest : BehaviorSpec({
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val brpClientService = mockk<BrpClientService>()
    val kvkClientService = mockk<KvkClientService>()
    val objectsClientService = mockk<ObjectsClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val identityService = mockk<IdentityService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val documentCreationDataService = DocumentCreationDataService(
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        brpClientService = brpClientService,
        kvkClientService = kvkClientService,
        objectsClientService = objectsClientService,
        flowableTaskService = flowableTaskService,
        identityService = identityService,
        productaanvraagService = productaanvraagService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    fun stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak: Zaak, zaakType: ZaakType) {
        every { zrcClientService.listZaakobjecten(any()) } returns Results(emptyList(), 0)
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
    }

    given("A logged-in user and a zaak with a behandelaar and an initiator of type natuurlijk persoon") {
        val loggedInUser = createLoggedInUser()
        val rolNatuurlijkPersoon =
            createRolNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val persoon = createPersoon(
            address = createAdressering(),
            verblijfplaats = createAdres(
                verblijfAdresBinnenland = createVerblijfadresBinnenland(huisnummer = 12).apply {
                    huisletter = "A"
                    huisnummertoevoeging = "bis"
                }
            )
        )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val rolMedewerker = createRolMedewerker()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNatuurlijkPersoon
        every {
            brpClientService.retrievePersoon(rolNatuurlijkPersoon.identificatienummer!!, any(), any())
        } returns persoon
        every { zrcClientService.listZaakobjecten(any()) } returns Results(emptyList(), 0)
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns rolMedewerker
        every { zgwApiService.findGroepForZaak(zaak) } returns rolOrganisatorischeEenheid
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the data is created correctly") {
                with(data) {
                    with(aanvragerData!!) {
                        naam shouldBe persoon.naam
                        straat shouldBe (persoon.verblijfplaats as Adres).verblijfadres.officieleStraatnaam
                        huisnummer shouldBe "12Abis"
                        postcode shouldBe (persoon.verblijfplaats as Adres).verblijfadres.postcode
                        woonplaats shouldBe (persoon.verblijfplaats as Adres).verblijfadres.woonplaats
                    }
                    with(gebruikerData) {
                        id shouldBe loggedInUser.username
                        naam shouldBe loggedInUser.getFullName()
                    }
                    with(zaakData) {
                        zaaktype shouldBe zaakType.omschrijving
                        behandelaar shouldBe "${rolMedewerker.betrokkeneIdentificatie!!.voorletters} " +
                            "${rolMedewerker.betrokkeneIdentificatie!!.achternaam}"
                        groep shouldBe rolOrganisatorischeEenheid.naam
                        io.kotest.assertions.withClue("SmartDocuments templates expect vertrouwelijkheidaanduiding in lowercase") {
                            vertrouwelijkheidaanduiding shouldBe "openbaar"
                        }
                    }
                    startformulierData shouldBe null
                    taskData shouldBe null
                }
            }
        }
    }

    given("A logged-in user and a zaak without a behandelaar and an initiator of type vestiging") {
        val loggedInUser = createLoggedInUser()
        val rolVestiging =
            createRolVestiging(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer")
        val vestigingsprofiel = createVestiging(
            adressen = listOf(
                createVestigingsAdres(
                    type = "bezoekadres",
                    straatnaam = "fakeBezoekStraatnaam",
                    huisnummer = 11,
                    postcode = "fakeBezoekPostcode",
                    plaats = "fakeBezoekPlaats"
                ),
                createVestigingsAdres(
                    type = "correspondentieadres",
                    straatnaam = "fakeCorrespondentieStraatnaam",
                    huisnummer = 7,
                    huisletter = "A",
                    huisnummerToevoeging = "2",
                    postcode = "fakeCorrespondentiePostcode",
                    plaats = "fakeCorrespondentiePlaats"
                )
            )
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolVestiging
        every {
            kvkClientService.findVestiging(rolVestiging.identificatienummer!!)
        } returns resultaatItem
        every { kvkClientService.findVestigingsprofiel("fakeVestigingsnummer") } returns vestigingsprofiel
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then(
                "the data is created correctly, with the correspondentieadres of the vestigingsprofiel as aanvrager address"
            ) {
                with(data) {
                    aanvragerData shouldBe AanvragerData(
                        naam = resultaatItem.naam,
                        straat = "fakeCorrespondentieStraatnaam",
                        huisnummer = "7A2",
                        postcode = "fakeCorrespondentiePostcode",
                        woonplaats = "fakeCorrespondentiePlaats"
                    )
                    with(gebruikerData) {
                        id shouldBe loggedInUser.username
                        naam shouldBe loggedInUser.getFullName()
                    }
                    with(zaakData) {
                        zaaktype shouldBe zaakType.omschrijving
                        behandelaar shouldBe null
                        groep shouldBe null
                        vertrouwelijkheidaanduiding shouldBe "openbaar"
                    }
                    startformulierData shouldBe null
                    taskData shouldBe null
                }
            }
        }
    }

    given(
        """
        A logged-in user and a zaak without a behandelaar and an initiator of type niet-natuurlijk persoon
        with a vestigingsnummer
        """
    ) {
        val loggedInUser = createLoggedInUser()
        val vestigingsNummer = "fakeVestigingsNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                    vestigingsnummer = vestigingsNummer
                )
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(vestingsnummer = vestigingsNummer)
        val vestigingsprofiel = createVestiging(
            adressen = listOf(
                createVestigingsAdres(
                    type = "bezoekadres",
                    straatnaam = "fakeBezoekStraatnaam",
                    huisnummer = 11,
                    postcode = "fakeBezoekPostcode",
                    plaats = "fakeBezoekPlaats"
                )
            )
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every {
            kvkClientService.findVestiging(vestigingsNummer)
        } returns resultaatItem
        every { kvkClientService.findVestigingsprofiel(vestigingsNummer) } returns vestigingsprofiel
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then(
                "the data is created correctly, with the bezoekadres as aanvrager address because there is no correspondentieadres"
            ) {
                with(data) {
                    aanvragerData shouldBe AanvragerData(
                        naam = resultaatItem.naam,
                        straat = "fakeBezoekStraatnaam",
                        huisnummer = "11",
                        postcode = "fakeBezoekPostcode",
                        woonplaats = "fakeBezoekPlaats"
                    )
                    with(gebruikerData) {
                        id shouldBe loggedInUser.username
                        naam shouldBe loggedInUser.getFullName()
                    }
                    with(zaakData) {
                        zaaktype shouldBe zaakType.omschrijving
                        behandelaar shouldBe null
                        groep shouldBe null
                        vertrouwelijkheidaanduiding shouldBe "openbaar"
                    }
                    startformulierData shouldBe null
                    taskData shouldBe null
                }
            }
        }
    }

    given("A zaak without an initiator role") {
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvragerData is null") {
                data.aanvragerData shouldBe null
            }
        }
    }

    given("A zaak with an initiator of type natuurlijk persoon that cannot be found in BRP") {
        val loggedInUser = createLoggedInUser()
        val rolNatuurlijkPersoon =
            createRolNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNatuurlijkPersoon
        every {
            brpClientService.retrievePersoon(rolNatuurlijkPersoon.identificatienummer!!, any(), any())
        } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvragerData is null") {
                data.aanvragerData shouldBe null
            }
        }
    }

    given("A zaak with an initiator of type vestiging that cannot be found in KVK") {
        val loggedInUser = createLoggedInUser()
        val rolVestiging =
            createRolVestiging(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolVestiging
        every { kvkClientService.findVestiging(rolVestiging.identificatienummer!!) } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvragerData is null") {
                data.aanvragerData shouldBe null
            }
        }
    }

    given("A zaak with an initiator of type vestiging with a foreign correspondentieadres") {
        val loggedInUser = createLoggedInUser()
        val rolVestiging =
            createRolVestiging(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer")
        val vestigingsprofiel = createVestiging(
            adressen = listOf(
                createVestigingsAdres(type = "correspondentieadres").apply {
                    straatHuisnummer = "fakeStraatHuisnummer"
                    toevoegingAdres = "fakeToevoegingAdres"
                    postcodeWoonplaats = "fakePostcodeWoonplaats"
                    land = "fakeLand"
                }
            )
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolVestiging
        every { kvkClientService.findVestiging(rolVestiging.identificatienummer!!) } returns resultaatItem
        every { kvkClientService.findVestigingsprofiel("fakeVestigingsnummer") } returns vestigingsprofiel
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager address has the foreign street line as straat and the postcode, woonplaats and land as woonplaats") {
                data.aanvragerData shouldBe AanvragerData(
                    naam = resultaatItem.naam,
                    straat = "fakeStraatHuisnummer fakeToevoegingAdres",
                    woonplaats = "fakePostcodeWoonplaats, fakeLand"
                )
            }
        }
    }

    given("A zaak with an initiator of type vestiging whose vestigingsprofiel is not found at the KVK") {
        val loggedInUser = createLoggedInUser()
        val rolVestiging =
            createRolVestiging(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer")

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolVestiging
        every { kvkClientService.findVestiging(rolVestiging.identificatienummer!!) } returns resultaatItem
        every {
            kvkClientService.findVestigingsprofiel("fakeVestigingsnummer")
        } throws NotFoundException("fakeNotFound")
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data has the name of the vestiging and no address, so that a document can still be created") {
                data.aanvragerData shouldBe AanvragerData(naam = resultaatItem.naam)
            }
        }
    }

    given("A zaak with an initiator of type rechtspersoon and a KVK basisprofiel call that fails with a server error") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(kvkNummer = kvkNummer, vestingsnummer = null)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem
        every { kvkClientService.findBasisprofiel(kvkNummer) } throws KvkClientException("fakeServerError")
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data has the name of the rechtspersoon and no address, so that a document can still be created") {
                data.aanvragerData shouldBe AanvragerData(naam = resultaatItem.naam)
            }
        }
    }

    given("A zaak with an initiator of type rechtspersoon and a KVK basisprofiel call that fails with a client error") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(kvkNummer = kvkNummer, vestingsnummer = null)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem
        every { kvkClientService.findBasisprofiel(kvkNummer) } throws WebApplicationException("fakeForbidden", Response.Status.FORBIDDEN)
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data has the name of the rechtspersoon and no address, so that a document can still be created") {
                data.aanvragerData shouldBe AanvragerData(naam = resultaatItem.naam)
            }
        }
    }

    given("A zaak with an initiator of type rechtspersoon and a KVK basisprofiel call that cannot reach the KVK") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(kvkNummer = kvkNummer, vestingsnummer = null)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem
        every { kvkClientService.findBasisprofiel(kvkNummer) } throws ProcessingException("fakeTimeout")
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data has the name of the rechtspersoon and no address, so that a document can still be created") {
                data.aanvragerData shouldBe AanvragerData(naam = resultaatItem.naam)
            }
        }
    }

    given("A zaak with an initiator of type rechtspersoon and a KVK basisprofiel call that fails unexpectedly") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(kvkNummer = kvkNummer, vestingsnummer = null)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem
        every { kvkClientService.findBasisprofiel(kvkNummer) } throws IllegalStateException("fakeUnexpectedError")

        `when`("SmartDocuments data is created") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                documentCreationDataService.createData(
                    loggedInUser = loggedInUser,
                    zaak = zaak
                )
            }

            then("the error is not suppressed, so that an unexpected failure is not hidden") {
                illegalStateException.message shouldBe "fakeUnexpectedError"
            }
        }
    }

    given("A zaak with an initiator of type vestiging whose vestigingsprofiel has no address") {
        val loggedInUser = createLoggedInUser()
        val rolVestiging =
            createRolVestiging(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(vestingsnummer = "fakeVestigingsnummer")

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolVestiging
        every { kvkClientService.findVestiging(rolVestiging.identificatienummer!!) } returns resultaatItem
        every {
            kvkClientService.findVestigingsprofiel("fakeVestigingsnummer")
        } returns createVestiging(adressen = emptyList())
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data has the name of the vestiging and no address") {
                data.aanvragerData shouldBe AanvragerData(naam = resultaatItem.naam)
            }
        }
    }

    given(
        "A zaak with an initiator of type niet-natuurlijk persoon with an RSIN (INN NNP ID) instead of a vestigingsnummer"
    ) {
        val loggedInUser = createLoggedInUser()
        val rsin = "fakeRsin"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(innNnpId = rsin)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(kvkNummer = "fakeKvkNummer", vestingsnummer = null)
        val basisprofiel = createBasisprofiel(
            eigenaar = createEigenaar(
                adressen = listOf(
                    createBasisprofielAdres(type = "bezoekadres", straatnaam = "fakeBezoekStraatnaam"),
                    createBasisprofielAdres(
                        type = "correspondentieadres",
                        postbusnummer = 1234,
                        postcode = "fakePostbusPostcode",
                        plaats = "fakePostbusPlaats"
                    )
                )
            )
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByRsin(rsin) } returns resultaatItem
        every { kvkClientService.findBasisprofiel("fakeKvkNummer") } returns basisprofiel
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then(
                "the aanvrager data has the name found by the RSIN lookup and the postbus correspondentieadres of the basisprofiel"
            ) {
                data.aanvragerData shouldBe AanvragerData(
                    naam = resultaatItem.naam,
                    straat = "Postbus",
                    huisnummer = "1234",
                    postcode = "fakePostbusPostcode",
                    woonplaats = "fakePostbusPlaats"
                )
            }
        }
    }

    given("A zaak with an initiator of type niet-natuurlijk persoon with only a KVK number") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(kvkNummer = kvkNummer, vestingsnummer = null)
        val basisprofiel = createBasisprofiel(
            eigenaar = createEigenaar(
                adressen = listOf(
                    createBasisprofielAdres(
                        type = "bezoekadres",
                        straatnaam = "fakeBezoekStraatnaam",
                        huisnummer = 11,
                        postcode = "fakeBezoekPostcode",
                        plaats = "fakeBezoekPlaats"
                    )
                )
            )
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem
        every { kvkClientService.findBasisprofiel(kvkNummer) } returns basisprofiel
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data has the name and the bezoekadres of the rechtspersoon found by KVK number") {
                data.aanvragerData shouldBe AanvragerData(
                    naam = resultaatItem.naam,
                    straat = "fakeBezoekStraatnaam",
                    huisnummer = "11",
                    postcode = "fakeBezoekPostcode",
                    woonplaats = "fakeBezoekPlaats"
                )
            }
        }
    }

    given("A zaak with an initiator of type niet-natuurlijk persoon with only a KVK number and no address in the basisprofiel") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val resultaatItem = createResultaatItem(
            adres = null,
            type = "rechtspersoon",
            kvkNummer = kvkNummer,
            vestingsnummer = null
        )

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns resultaatItem
        every { kvkClientService.findBasisprofiel(kvkNummer) } returns createBasisprofiel(
            eigenaar = createEigenaar(adressen = emptyList())
        )
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the aanvrager data contains the name of the rechtspersoon and no address") {
                data.aanvragerData shouldBe AanvragerData(naam = resultaatItem.naam)
            }
        }
    }

    given(
        "A zaak with an initiator of type niet-natuurlijk persoon with neither an RSIN, a KVK number nor a vestigingsnummer"
    ) {
        val loggedInUser = createLoggedInUser()
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                    innNnpId = null,
                    kvkNummer = null,
                    vestigingsnummer = null
                )
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon

        `when`("SmartDocuments data is created") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                documentCreationDataService.createData(
                    loggedInUser = loggedInUser,
                    zaak = zaak
                )
            }

            then("an exception is thrown") {
                illegalStateException.message shouldBe
                    "Niet-natuurlijke persoon initiator role '$rolNietNatuurlijkPersoon' with neither " +
                    "INN NNP ID (RSIN), KVK number nor vestigingsnummer is not supported"
            }
        }
    }

    given("A zaak with an initiator of type niet-natuurlijk persoon without a betrokkene identificatie") {
        val loggedInUser = createLoggedInUser()
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = null
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon

        `when`("SmartDocuments data is created") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                documentCreationDataService.createData(
                    loggedInUser = loggedInUser,
                    zaak = zaak
                )
            }

            then("an exception is thrown") {
                illegalStateException.message shouldBe
                    "Niet-natuurlijke persoon initiator role '$rolNietNatuurlijkPersoon' with neither " +
                    "INN NNP ID (RSIN), KVK number nor vestigingsnummer is not supported"
            }
        }
    }

    given("A zaak with an initiator of type niet-natuurlijk persoon with a KVK number that KVK does not find") {
        val loggedInUser = createLoggedInUser()
        val kvkNummer = "fakeKvkNummer"
        val rolNietNatuurlijkPersoon =
            createRolNietNatuurlijkPersoon(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR),
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(kvkNummer = kvkNummer)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        every { kvkClientService.findRechtspersoonByKvkNummer(kvkNummer) } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("no aanvrager data is created") {
                data.aanvragerData shouldBe null
            }
        }
    }

    given("A zaak with an initiator role of an unsupported betrokkene type") {
        val loggedInUser = createLoggedInUser()
        val rolOrganisatorischeEenheid =
            createRolOrganisatorischeEenheid(
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolOrganisatorischeEenheid

        `when`("SmartDocuments data is created") {
            val illegalStateException = shouldThrow<IllegalStateException> {
                documentCreationDataService.createData(
                    loggedInUser = loggedInUser,
                    zaak = zaak
                )
            }

            then("an exception is thrown") {
                illegalStateException.message shouldBe
                    "Initiator of type '${rolOrganisatorischeEenheid.betrokkeneType}' is not supported"
            }
        }
    }

    given("A zaak that is opgeschort, is verlengd, and has a resultaat and a status") {
        val loggedInUser = createLoggedInUser()
        val resultaatUri = URI("https://example.com/resultaat/${UUID.randomUUID()}")
        val statusUri = URI("https://example.com/status/${UUID.randomUUID()}")
        val opschorting = createOpschorting(reden = "fakeOpschortingReden", indicatie = true)
        val verlenging = createVerlenging(reden = "fakeVerlengingReden")
        val zaakType = createZaakType()
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            resultaat = resultaatUri,
            status = statusUri,
            opschorting = opschorting,
            verlenging = verlenging
        )
        val resultaat = createResultaat()
        val resultaatType = createResultaatType(omschrijving = "fakeResultaatOmschrijving")
        val status = createStatus()
        val statusType = createStatusType(omschrijving = "fakeStatusOmschrijving")

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)
        every { zrcClientService.readResultaat(resultaatUri) } returns resultaat
        every { ztcClientService.readResultaattype(resultaat.resultaattype) } returns resultaatType
        every { zrcClientService.readStatus(statusUri) } returns status
        every { ztcClientService.readStatustype(status.statustype) } returns statusType

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the opschorting reden, verlenging reden, resultaat, and status are included") {
                data.zaakData.opschortingReden shouldBe opschorting.reden
                data.zaakData.verlengingReden shouldBe verlenging.reden
                data.zaakData.resultaat shouldBe resultaatType.omschrijving
                data.zaakData.status shouldBe statusType.omschrijving
            }
        }
    }

    given("A zaak with a startformulier productaanvraag object") {
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val zaakobjectProductaanvraag = createZaakobjectProductaanvraag(zaakURI = zaak.url)
        val modelObject = createModelObject()
        val productaanvraagDimpact = createProductaanvraagDimpact(type = "fakeProductaanvraagType")
        val aanvraaggegevens = mapOf("fakeKey" to "fakeValue")

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null
        every { zrcClientService.listZaakobjecten(any()) } returns Results(listOf(zaakobjectProductaanvraag), 1)
        every {
            objectsClientService.readObject(zaakobjectProductaanvraag.`object`!!.extractUuid())
        } returns modelObject
        every { productaanvraagService.getProductaanvraag(modelObject) } returns productaanvraagDimpact
        every { productaanvraagService.getAanvraaggegevens(modelObject) } returns aanvraaggegevens
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
        every { zgwApiService.findGroepForZaak(zaak) } returns null
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak
            )

            then("the startformulier data is included") {
                with(data.startformulierData!!) {
                    productAanvraagtype shouldBe productaanvraagDimpact.type
                    this.data shouldBe aanvraaggegevens
                }
            }
        }
    }

    given("A task ID for which a task with an assigned behandelaar exists") {
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val taskId = "fakeTaskId"
        val assigneeId = "fakeAssigneeId"
        val user = createUser(username = assigneeId)
        val taskInfo = mockk<TaskInfo>()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)
        every { flowableTaskService.readTask(taskId) } returns taskInfo
        every { taskInfo.name } returns "fakeTaskName"
        every { taskInfo.assignee } returns assigneeId
        every { identityService.readUser(assigneeId, any()) } returns user

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak,
                taskId = taskId
            )

            then("the task data is included with the behandelaar's full name") {
                with(data.taskData!!) {
                    naam shouldBe taskInfo.name
                    behandelaar shouldBe user.getFullName()
                }
            }
        }
    }

    given("A task ID for which a task without an assignee exists") {
        val loggedInUser = createLoggedInUser()
        val zaakType = createZaakType()
        val zaak = createZaak(zaaktypeUri = zaakType.url)
        val taskId = "fakeTaskId"
        val taskInfo = mockk<TaskInfo>()

        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns null
        stubZaakWithoutZaakobjectenBehandelaarAndGroep(zaak, zaakType)
        every { flowableTaskService.readTask(taskId) } returns taskInfo
        every { taskInfo.name } returns "fakeTaskName"
        every { taskInfo.assignee } returns null

        `when`("SmartDocuments data is created") {
            val data = documentCreationDataService.createData(
                loggedInUser = loggedInUser,
                zaak = zaak,
                taskId = taskId
            )

            then("the task data is included without a behandelaar") {
                with(data.taskData!!) {
                    naam shouldBe taskInfo.name
                    behandelaar shouldBe null
                }
            }
        }
    }
})
