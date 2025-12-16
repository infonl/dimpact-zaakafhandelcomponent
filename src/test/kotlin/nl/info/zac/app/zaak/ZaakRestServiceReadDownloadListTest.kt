/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolNatuurlijkPersoonForReads
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoonForReads
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createZaakAfzender
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.model.createBetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaaktype
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import org.apache.http.HttpStatus
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList", "LargeClass")
class ZaakRestServiceReadDownloadListTest : BehaviorSpec({
    val decisionService = mockk<DecisionService>()
    val bpmnService = mockk<BpmnService>()
    val brcClientService = mockk<BrcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val healthCheckService = mockk<HealthCheckService>()
    val identityService = mockk<IdentityService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val objectsClientService = mockk<ObjectsClientService>()
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val opschortenZaakHelper = mockk<SuspensionZaakHelper>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val restDecisionConverter = mockk<RestDecisionConverter>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val zaakHistoryLineConverter = mockk<ZaakHistoryLineConverter>()
    val signaleringService = mockk<SignaleringService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakHistoryService = mockk<ZaakHistoryService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakRestService = ZaakRestService(
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        cmmnService = cmmnService,
        configuratieService = configuratieService,
        decisionService = decisionService,
        dispatcher = testDispatcher,
        drcClientService = drcClientService,
        eventingService = eventingService,
        flowableTaskService = flowableTaskService,
        healthCheckService = healthCheckService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        indexingService = indexingService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        opschortenZaakHelper = opschortenZaakHelper,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        restDecisionConverter = restDecisionConverter,
        restZaakConverter = restZaakConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        signaleringService = signaleringService,
        zaakHistoryLineConverter = zaakHistoryLineConverter,
        zaakHistoryService = zaakHistoryService,
        zaakService = zaakService,
        zaakVariabelenService = zaakVariabelenService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Listing zaak types that can be used for zaak creation") {
        Given(
            """
        Two existing CMMN zaaktypes in the configured catalogue for which the logged in user is authorised
        and which are valid on the current date
        """
        ) {
            val defaultCatalogueURI = URI("http://example.com/fakeCatalogue")
            val now = LocalDate.now()
            val zaaktypes = listOf(
                createZaakType(
                    omschrijving = "Zaaktype 1",
                    identification = "ZAAKTYPE1",
                    beginGeldigheid = now.minusDays(1)
                ),
                createZaakType(
                    omschrijving = "Zaaktype 2",
                    identification = "ZAAKTYPE2",
                    beginGeldigheid = now.minusDays(2),
                    eindeGeldigheid = now.plusDays(1)
                )
            )
            val restZaaktypes = listOf(createRestZaaktype(), createRestZaaktype())
            val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()
            every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes
            zaaktypes.forEach {
                every { healthCheckService.controleerZaaktype(it.url) } returns zaaktypeInrichtingscheck
                every { restZaaktypeConverter.convert(it) } returns restZaaktypes[zaaktypes.indexOf(it)]
                every { policyService.readOverigeRechten(it.omschrijving) } returns createOverigeRechten()
                every { policyService.isAuthorisedForZaaktype(it.omschrijving) } returns true
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(it.url.extractUuid())
                } returns createZaaktypeCmmnConfiguration()
            }
            every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogueURI

            When("the zaaktypes are requested") {
                val returnedRestZaaktypes = zaakRestService.listZaaktypesForZaakCreation()

                Then("only CMMN zaaktypes are returned for which the user is authorised") {
                    verify(exactly = 1) {
                        ztcClientService.listZaaktypen(defaultCatalogueURI)
                    }
                    returnedRestZaaktypes shouldHaveSize 2
                    returnedRestZaaktypes shouldBe restZaaktypes
                }
            }
        }

        Given("Two CMMN and one BPMN zaaktypes valid on the current date") {
            val defaultCatalogueURI = URI("http://example.com/fakeCatalogue")
            val now = LocalDate.now()
            val zaakType1UUID = UUID.randomUUID()
            val zaakType2UUID = UUID.randomUUID()
            val zaakType3UUID = UUID.randomUUID()
            val zaaktypes = listOf(
                createZaakType(
                    omschrijving = "Zaaktype 1",
                    identification = "ZAAKTYPE1",
                    uri = URI("https://example.com/zaaktypes/$zaakType1UUID"),
                    beginGeldigheid = now.minusDays(1)
                ),
                createZaakType(
                    omschrijving = "Zaaktype 2",
                    identification = "ZAAKTYPE2",
                    uri = URI("https://example.com/zaaktypes/$zaakType2UUID"),
                    beginGeldigheid = now.minusDays(1)
                ),
                createZaakType(
                    omschrijving = "Zaaktype 3",
                    identification = "ZAAKTYPE3",
                    uri = URI("https://example.com/zaaktypes/$zaakType3UUID"),
                    beginGeldigheid = now.minusDays(1)
                ),
            )
            val restZaaktype1 = createRestZaaktype()
            val restZaaktype2 = createRestZaaktype()
            val restZaaktype3 = createRestZaaktype()
            val restZaaktypes = listOf(restZaaktype1, restZaaktype2, restZaaktype3)
            val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()
            zaaktypes.slice(0..1).forEach {
                every { restZaaktypeConverter.convert(it) } returns restZaaktypes[zaaktypes.indexOf(it)]
                every { healthCheckService.controleerZaaktype(it.url) } returns zaaktypeInrichtingscheck
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(it.url.extractUuid())
                } returns createZaaktypeCmmnConfiguration()
            }
            zaaktypes.last().let {
                every { restZaaktypeConverter.convert(it) } returns restZaaktypes[zaaktypes.indexOf(it)]
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(it.url.extractUuid())
                } returns createZaaktypeBpmnConfiguration()
            }

            And("all zaaktypes are authorised") {
                zaaktypes.forEach {
                    every { policyService.readOverigeRechten(it.omschrijving) } returns createOverigeRechten()
                    every { policyService.isAuthorisedForZaaktype(it.omschrijving) } returns true
                }

                every { configuratieService.readDefaultCatalogusURI() } returns defaultCatalogueURI
                every { ztcClientService.listZaaktypen(defaultCatalogueURI) } returns zaaktypes

                When("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaakRestService.listZaaktypesForZaakCreation()

                    Then("all zaaktypes are returned") {
                        verify(exactly = 1) {
                            ztcClientService.listZaaktypen(defaultCatalogueURI)
                        }
                        verify(exactly = 3) {
                            zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                        }
                        returnedRestZaaktypes shouldHaveSize 3
                        returnedRestZaaktypes shouldBe restZaaktypes
                    }
                }
            }
            And("a user is not authorised for a CMMN zaaktype, because of missing startenZaak right") {
                clearMocks(ztcClientService, zaaktypeConfigurationService, answers = false)
                zaaktypes[1].let {
                    every {
                        policyService.readOverigeRechten(it.omschrijving)
                    } returns createOverigeRechten(startenZaak = false)
                }

                When("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaakRestService.listZaaktypesForZaakCreation()

                    Then("only the zaaktypes for which the user is authorised are returned") {
                        verify(exactly = 1) {
                            ztcClientService.listZaaktypen(defaultCatalogueURI)
                        }
                        verify(exactly = 2) {
                            zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                        }
                        returnedRestZaaktypes shouldHaveSize 2
                        returnedRestZaaktypes shouldBe listOf(restZaaktype1, restZaaktype3)
                    }
                }
            }
            And("user is not authorised for a CMMN zaaktype") {
                clearMocks(ztcClientService, zaaktypeConfigurationService, answers = false)
                zaaktypes[1].let {
                    every { policyService.readOverigeRechten(it.omschrijving) } returns createOverigeRechten()
                    every { policyService.isAuthorisedForZaaktype(it.omschrijving) } returns false
                }

                When("the zaaktypes are listed") {
                    val returnedRestZaaktypes = zaakRestService.listZaaktypesForZaakCreation()

                    Then("the authorised zaaktypes are returned") {
                        verify(exactly = 1) {
                            ztcClientService.listZaaktypen(defaultCatalogueURI)
                        }
                        verify(exactly = 2) {
                            zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                        }
                        returnedRestZaaktypes shouldHaveSize 2
                        returnedRestZaaktypes shouldBe listOf(restZaaktype1, restZaaktype3)
                    }
                }
            }
        }
    }

    Context("Reading a zaak") {
        Given("A zaak with an initiator of type BSN for which signaleringen exist") {
            val zaakUUID = UUID.randomUUID()
            val zaak = createZaak(uuid = zaakUUID)
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(lezen = true)
            val restZaak = createRestZaak(
                uuid = zaakUUID,
                rechten = zaakRechten.toRestZaakRechten(),
                initiatorBetrokkeneIdentificatie = createBetrokkeneIdentificatie(
                    type = IdentificatieType.BSN,
                    bsnNummer = "123456789"
                )
            )
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns restZaak
            every { signaleringService.deleteSignaleringenForZaak(zaak) } returns 1

            When("the zaak is read") {
                val returnedRestZaak = zaakRestService.readZaak(zaakUUID)

                Then("the zaak is returned and any zaak signaleringen are deleted") {
                    returnedRestZaak shouldBe restZaak
                    verify(exactly = 1) {
                        signaleringService.deleteSignaleringenForZaak(zaak)
                    }
                }
            }
        }
    }

    Context("Downloading a process diagram") {
        Given("An existing BPMN process diagram for a given zaak UUID") {
            val uuid = UUID.randomUUID()
            every { bpmnService.getProcessDiagram(uuid) } returns ByteArrayInputStream("fakeDiagram".toByteArray())

            When("the process diagram is requested") {
                val response = zaakRestService.downloadProcessDiagram(uuid)

                Then(
                    "a HTTP OK response is returned with a 'Content-Disposition' HTTP header and the diagram as input stream"
                ) {
                    with(response) {
                        status shouldBe HttpStatus.SC_OK
                        headers["Content-Disposition"]!![0] shouldBe """attachment; filename="procesdiagram.gif"""".trimIndent()
                        (entity as InputStream).bufferedReader().use { it.readText() } shouldBe "fakeDiagram"
                    }
                }
            }
        }
    }

    Context("Listing betrokkenen for a zaak") {
        Given(
            """
            A zaak with a betrokkene of type natuurlijk persoon, a betrokkene of type niet-natuurlijk persoon
            with a vestigingsnummer, a betrokkene of type niet-natuurlijk persoon with a RSIN (=INN NNP ID),
            and a betrokkene without a betrokkene identification.
            """
        ) {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val rolNatuurlijkPersoon = createRolNatuurlijkPersoonForReads()
            val rolNietNatuurlijkPersoonWithVestigingsnummer = createRolNietNatuurlijkPersoonForReads(
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                    vestigingsnummer = "fakeVestigingsNummer"
                )
            )
            val rolNietNatuurlijkPersoonWithRSIN = createRolNietNatuurlijkPersoonForReads(
                nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                    innNnpId = "fakeInnNnpId"
                )
            )
            val rolNatuurlijkPersoonWithoutIdentificatie = createRolNatuurlijkPersoonForReads(
                natuurlijkPersoonIdentificatie = null
            )
            val betrokkeneRoles = listOf(
                rolNatuurlijkPersoon,
                rolNietNatuurlijkPersoonWithVestigingsnummer,
                rolNietNatuurlijkPersoonWithRSIN,
                rolNatuurlijkPersoonWithoutIdentificatie
            )
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { zaakService.listBetrokkenenforZaak(zaak) } returns betrokkeneRoles

            When("the betrokkenen are retrieved") {
                val returnedBetrokkenen = zaakRestService.listBetrokkenenVoorZaak(zaak.uuid)

                Then("the betrokkenen are correctly returned except the betrokkene without identification") {
                    with(returnedBetrokkenen) {
                        size shouldBe 3
                        with(first()) {
                            rolid shouldBe rolNatuurlijkPersoon.uuid.toString()
                            roltype shouldBe rolNatuurlijkPersoon.omschrijving
                            roltoelichting shouldBe rolNatuurlijkPersoon.roltoelichting
                            type shouldBe "NATUURLIJK_PERSOON"
                            identificatie shouldBe rolNatuurlijkPersoon.identificatienummer
                            identificatieType shouldBe IdentificatieType.BSN
                        }
                        with(this[1]) {
                            rolid shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.uuid.toString()
                            roltype shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.omschrijving
                            roltoelichting shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.roltoelichting
                            type shouldBe "NIET_NATUURLIJK_PERSOON"
                            identificatie shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.identificatienummer
                            identificatieType shouldBe IdentificatieType.VN
                        }
                        with(last()) {
                            rolid shouldBe rolNietNatuurlijkPersoonWithRSIN.uuid.toString()
                            roltype shouldBe rolNietNatuurlijkPersoonWithRSIN.omschrijving
                            roltoelichting shouldBe rolNietNatuurlijkPersoonWithRSIN.roltoelichting
                            type shouldBe "NIET_NATUURLIJK_PERSOON"
                            identificatie shouldBe rolNietNatuurlijkPersoonWithRSIN.identificatienummer
                            identificatieType shouldBe IdentificatieType.RSIN
                        }
                    }
                }
            }
        }
    }

    Context("Listing afzenders for zaak and reading the default afzender for a zaak") {
        Given("ZaaktypeCmmnConfiguration object with zaakafzenders, one of which uses 'special mails'") {
            val zaakUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val zaak = createZaak(
                uuid = zaakUUID,
                zaaktypeUri = URI("https://example.com/zaaktypes/$zaakTypeUUID")
            )
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(zaaktypeUUID = zaakTypeUUID)
            val zaakAfzenders = zaaktypeCmmnConfiguration.getZaakAfzenders().plus(
                createZaakAfzender(
                    id = 2L,
                    zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration,
                    defaultMail = true,
                    mail = "GEMEENTE",
                    replyTo = "MEDEWERKER"
                )
            )
            zaaktypeCmmnConfiguration.setZaakAfzenders(zaakAfzenders)
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration
            every { configuratieService.readGemeenteMail() } returns "fake-gemeente@example.com"
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                email = "fake-medewerker@example.com"
            )

            When("the zaakafzenders are requested") {
                val returnedRestZaakAfzenders = zaakRestService.listAfzendersVoorZaak(zaakUUID)

                Then("the zaakafzenders are returned, including special mails for GEMEENTE and MEDEWERKER") {
                    with(returnedRestZaakAfzenders) {
                        // we defined two afzender emails but expect three because
                        // we did not define an 'MEDEWERKER' afzender email so this one
                        // should be added automatically
                        size shouldBe 3
                        first().apply {
                            id shouldBe null
                            defaultMail shouldBe true
                            mail shouldBe "fake-gemeente@example.com"
                            replyTo shouldBe "fake-medewerker@example.com"
                            speciaal shouldBe true
                        }
                        this[1].apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "fake-medewerker@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe true
                        }
                        last().apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "mail@example.com"
                            replyTo shouldBe "replyTo@example.com"
                            speciaal shouldBe false
                        }
                    }
                }
            }

            When("the default afzender is read") {
                val returnedDefaultRestZaakAfzender = zaakRestService.readDefaultAfzenderVoorZaak(zaakUUID)

                Then("the default afzender is returned with the email address of the special mail type") {
                    returnedDefaultRestZaakAfzender shouldNotBe null
                    with(returnedDefaultRestZaakAfzender!!) {
                        id shouldBe null
                        defaultMail shouldBe true
                        mail shouldBe "fake-gemeente@example.com"
                        replyTo shouldBe "fake-medewerker@example.com"
                        speciaal shouldBe true
                    }
                }
            }
        }

        Given("ZaaktypeCmmnConfiguration without any zaakafzenders") {
            val zaakUUID = UUID.randomUUID()
            val zaakTypeUUID = UUID.randomUUID()
            val zaak = createZaak(
                uuid = zaakUUID,
                zaaktypeUri = URI("https://example.com/zaaktypes/$zaakTypeUUID")
            )
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(zaaktypeUUID = zaakTypeUUID)
            zaaktypeCmmnConfiguration.setZaakAfzenders(emptyList())
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration
            every { configuratieService.readGemeenteMail() } returns "fake-gemeente@example.com"
            every { loggedInUserInstance.get() } returns createLoggedInUser(
                email = "fake-medewerker@example.com"
            )

            When("the zaakafzenders are requested") {
                val returnedRestZaakAfzenders = zaakRestService.listAfzendersVoorZaak(zaakUUID)

                Then("a list consisting of 'special mail' afzenders only should be returned") {
                    returnedRestZaakAfzenders shouldHaveSize 2
                    with(returnedRestZaakAfzenders) {
                        size shouldBe 2
                        first().apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "fake-gemeente@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe true
                        }
                        last().apply {
                            id shouldBe null
                            defaultMail shouldBe false
                            mail shouldBe "fake-medewerker@example.com"
                            replyTo shouldBe null
                            speciaal shouldBe true
                        }
                    }
                }
            }

            When("the default afzender is read") {
                val returnedDefaultRestZaakAfzender = zaakRestService.readDefaultAfzenderVoorZaak(zaakUUID)

                Then("no default afzender should be returned") {
                    returnedDefaultRestZaakAfzender shouldBe null
                }
            }
        }
    }
})
