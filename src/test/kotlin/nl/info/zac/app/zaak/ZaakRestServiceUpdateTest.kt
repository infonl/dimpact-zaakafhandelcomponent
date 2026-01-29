/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.exception.DueDateNotAllowed
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import nl.info.zac.app.zaak.model.createRESTGeometry
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakCreateData
import nl.info.zac.app.zaak.model.createRestZaakDataUpdate
import nl.info.zac.app.zaak.model.createRestZaakInitiatorGegevens
import nl.info.zac.app.zaak.model.createRestZaakLocatieGegevens
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identification.IdentificationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import org.flowable.task.api.Task
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
class ZaakRestServiceUpdateTest : BehaviorSpec({
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
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakHistoryService = mockk<ZaakHistoryService>()
    val identificationService = mockk<IdentificationService>()
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
        ztcClientService = ztcClientService,
        identificationService = identificationService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Updating a zaak") {

        Given("a BPMN zaak with tasks exists and zaak and tasks have final date and communication channel set") {
            val changeDescription = "change description"
            val zaak = createZaak()
            val zaakType = createZaakType(servicenorm = "P10D")
            val zaakRechten = createZaakRechten()
            val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
            val restZaakCreateData = createRestZaakCreateData(uiterlijkeEinddatumAfdoening = newZaakFinalDate).apply {
                einddatumGepland = startdatum
            }
            val restZaakEditMetRedenGegevens =
                RESTZaakEditMetRedenGegevens(zaak = restZaakCreateData, reden = changeDescription)
            val patchedZaak = createZaak()
            val patchedRestZaak = createRestZaak()
            val task = mockk<Task>()
            val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zrcClientService.patchZaak(zaak.uuid, any(), changeDescription) } returns patchedZaak
            every { task.id } returns "id"
            every { eventingService.send(any<ScreenEvent>()) } just runs
            every { restZaakConverter.toRestZaak(patchedZaak, zaakType, zaakRechten) } returns patchedRestZaak
            every {
                identityService.validateIfUserIsInGroup(restZaakCreateData.behandelaar!!.id, restZaakCreateData.groep!!.id)
            } just runs
            every {
                zaakVariabelenService.setCommunicatiekanaal(
                    zaak.uuid,
                    restZaakEditMetRedenGegevens.zaak.communicatiekanaal!!
                )
            } just runs
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns zaaktypeBpmnConfiguration

            When("zaak final date is set to a later date") {
                every {
                    opschortenZaakHelper.adjustFinalDateForOpenTasks(zaak.uuid, newZaakFinalDate)
                } returns listOf(task, task)

                val updatedRestZaak = zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)

                Then("zaak is updated with the new data") {
                    updatedRestZaak shouldBe patchedRestZaak
                }

                And("the communication channel is exposed to zaak data") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setCommunicatiekanaal(
                            zaak.uuid,
                            restZaakEditMetRedenGegevens.zaak.communicatiekanaal!!
                        )
                    }
                }

                And("screen event signals are sent") {
                    verify(exactly = 3) {
                        eventingService.send(any<ScreenEvent>())
                    }
                }
            }
        }

        Given("a zaak and user not part of any group") {
            val changeDescription = "change description"
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaakCreateData = createRestZaakCreateData()
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, changeDescription)

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { identityService.validateIfUserIsInGroup(any(), any()) } throws InputValidationFailedException()
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns createZaaktypeCmmnConfiguration()

            When("zaak update is requested") {
                shouldThrow<InputValidationFailedException> {
                    zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
                }

                Then("exception is thrown") {}
            }
        }

        Given("no verlengenDoorlooptijd policy") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(verlengenDoorlooptijd = false)
            val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
            val restZaakCreateData =
                createRestZaakCreateData(uiterlijkeEinddatumAfdoening = newZaakFinalDate, einddatumGepland = null)
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, "change description")

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns createZaaktypeCmmnConfiguration()

            When("zaak update is requested with a new final date") {
                val exception = shouldThrow<PolicyException> {
                    zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
                }

                Then("it fails") {
                    exception.message shouldBe null
                }
            }

            When("zaak update is requested without a new final date") {
                val restZaakCreateData = restZaakCreateData.copy(
                    uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening
                )
                val restZaak = createRestZaak()
                val zaakRechten = createZaakRechten()
                every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
                every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
                every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten) } returns restZaak
                every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak

                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens.copy(zaak = restZaakCreateData))

                Then("it succeeds") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(zaak.uuid, any(), any())
                    }
                }
            }
        }

        Given("no wijzigenDoorlooptijd policy") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(wijzigenDoorlooptijd = false)
            val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
            val restZaakCreateData =
                createRestZaakCreateData(uiterlijkeEinddatumAfdoening = newZaakFinalDate, einddatumGepland = null)
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, "change description")

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns createZaaktypeCmmnConfiguration()

            When("zaak update is requested with a new final date") {
                val exception = shouldThrow<PolicyException> {
                    zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
                }

                Then("it fails") {
                    exception.message shouldBe null
                }
            }

            When("zaak update is requested without a new final date") {
                val restZaak = createRestZaak(
                    uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening
                )
                val zaakRechten = createZaakRechten()
                every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
                every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
                every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten) } returns restZaak
                every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak
                every {
                    opschortenZaakHelper.adjustFinalDateForOpenTasks(zaak.uuid, newZaakFinalDate)
                } returns emptyList()
                every { eventingService.send(any<ScreenEvent>()) } just runs

                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens.copy(zaak = restZaakCreateData))

                Then("it succeeds") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(zaak.uuid, any(), any())
                    }
                }
            }
        }

        Given("due date change when servicenorm is not specified in OpenZaak") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()
            val restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now())
            val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaakCreateData, "change description")

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns createZaaktypeCmmnConfiguration()

            When("zaak update is requested with a new final date") {
                val exception = shouldThrow<DueDateNotAllowed> {
                    zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
                }

                Then("it fails") {
                    exception.message shouldBe null
                }
            }
        }
    }

    Context("Updating a zaak location") {
        Given("An existing zaak") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()
            val restGeometry = createRESTGeometry()
            val reason = "fakeReason"
            val restZaakLocatieGegevens = createRestZaakLocatieGegevens(
                restGeometry = restGeometry,
                reason = reason
            )
            val updatedZaak = createZaak()
            val updatedRestZaak = createRestZaak()
            val patchZaakSlot = slot<Zaak>()
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), reason) } returns updatedZaak
            every { restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten) } returns updatedRestZaak

            When("a zaak location is added to the zaak") {
                val restZaak = zaakRestService.updateZaakLocatie(zaak.uuid, restZaakLocatieGegevens)

                Then("the zaak is updated correctly") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(zaak.uuid, any(), reason)
                    }
                    restZaak shouldBe updatedRestZaak
                    with(patchZaakSlot.captured) {
                        zaakgeometrie.shouldBeInstanceOf<GeoJSONGeometry>()
                        with(zaakgeometrie as GeoJSONGeometry) {
                            coordinates[0].toDouble() shouldBe restGeometry.point!!.longitude
                            coordinates[1].toDouble() shouldBe restGeometry.point!!.latitude
                        }
                    }
                }
            }
        }

        Given("An existing zaak with a zaak location") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()
            val reason = "fakeReasonForDeletion"
            val restZaakLocatieGegevens = createRestZaakLocatieGegevens(
                restGeometry = null,
                reason = reason
            )
            val updatedZaak = createZaak()
            val updatedRestZaak = createRestZaak()
            val patchZaakSlot = slot<Zaak>()
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), reason) } returns updatedZaak
            every { restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten) } returns updatedRestZaak

            When("the zaak location is deleted") {
                val restZaak = zaakRestService.updateZaakLocatie(zaak.uuid, restZaakLocatieGegevens)

                Then("the zaak is updated correctly") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(zaak.uuid, any(), reason)
                    }
                    restZaak shouldBe updatedRestZaak
                    with(patchZaakSlot.captured) {
                        zaakgeometrie.shouldBeInstanceOf<DeleteGeoJSONGeometry>()
                    }
                }
            }
        }
    }

    Context("Updating an initiator") {
        Given("A zaak with an initiator and rest zaak betrokkene gegevens") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(toevoegenInitiatorPersoon = true)
            val bsn = "123456677"
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens()
            val rolMedewerker = createRolMedewerker()
            val restZaak = createRestZaak()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zrcClientService.deleteRol(any(), any()) } just runs
            every { zaakService.addInitiatorToZaak(any(), any(), any(), any()) } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns restZaak
            every {
                identificationService.replaceKeyWithBsn(restZaakInitiatorGegevens.betrokkeneIdentificatie.temporaryPersonId!!)
            } returns bsn

            When("an initiator is updated") {
                val updatedRestZaak = zaakRestService.updateInitiator(restZaakInitiatorGegevens)

                Then("the old initiator should be removed and the new one should be added to the zaak") {
                    updatedRestZaak shouldBe restZaak
                    verify(exactly = 1) {
                        zrcClientService.deleteRol(
                            rolMedewerker,
                            "Verwijderd door de medewerker tijdens het behandelen van de zaak"
                        )
                        zaakService.addInitiatorToZaak(
                            IdentificatieType.BSN,
                            bsn,
                            zaak,
                            restZaakInitiatorGegevens.toelichting!!
                        )
                    }
                }
            }
        }

        Given("A zaak without an initiator and an initiator of type vestiging") {
            val kvkNummer = "1234567"
            val vestigingsnummer = "00012352546"
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens(
                betrokkeneIdentificatie = BetrokkeneIdentificatie(
                    type = IdentificatieType.VN,
                    kvkNummer = kvkNummer,
                    vestigingsnummer = vestigingsnummer
                )
            )
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaakService.addInitiatorToZaak(
                    IdentificatieType.VN,
                    "$kvkNummer|$vestigingsnummer",
                    zaak,
                    any()
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns createRestZaak()

            When("the initiator is updated") {
                zaakRestService.updateInitiator(
                    restZaakInitiatorGegevens.apply {
                        toelichting = "test reden"
                    }
                )

                Then("the initiator should be added to the zaak") {
                    verify(exactly = 1) {
                        zaakService.addInitiatorToZaak(
                            IdentificatieType.VN,
                            "$kvkNummer|$vestigingsnummer",
                            any(),
                            "test reden"
                        )
                    }
                }
            }

            When("the initiator is updated without an explanation") {
                zaakRestService.updateInitiator(
                    restZaakInitiatorGegevens = restZaakInitiatorGegevens.apply {
                        toelichting = null
                    }
                )

                Then("the reason should be set to the default") {
                    verify(exactly = 1) {
                        zaakService.addInitiatorToZaak(
                            IdentificatieType.VN,
                            "$kvkNummer|$vestigingsnummer",
                            any(),
                            "Toegekend door de medewerker tijdens het behandelen van de zaak"
                        )
                    }
                }
            }
        }

        Given("A zaak without an initiator and an initiator of type rechtspersoon") {
            val kvkNummer = "1234567"
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens(
                betrokkeneIdentificatie = BetrokkeneIdentificatie(
                    type = IdentificatieType.RSIN,
                    kvkNummer = kvkNummer,
                )
            )
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every {
                zaakService.addInitiatorToZaak(
                    IdentificatieType.RSIN,
                    kvkNummer,
                    zaak,
                    any()
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns createRestZaak()

            When("the initiator is updated") {
                zaakRestService.updateInitiator(
                    restZaakInitiatorGegevens.apply {
                        toelichting = "test reden"
                    }
                )

                Then("the initiator should be added to the zaak") {
                    verify(exactly = 1) {
                        zaakService.addInitiatorToZaak(
                            IdentificatieType.RSIN,
                            kvkNummer,
                            any(),
                            "test reden"
                        )
                    }
                }
            }
        }

        Given("A zaak without an initiator and an initiator of type rechtspersoon without a KVK nummer") {
            val restZaakInitiatorGegevens = createRestZaakInitiatorGegevens(
                betrokkeneIdentificatie = BetrokkeneIdentificatie(
                    type = IdentificatieType.RSIN,
                    kvkNummer = null,
                )
            )
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten

            When("the initiator is updated without a required KVK nummer") {
                val exception = shouldThrow<IllegalArgumentException> {
                    zaakRestService.updateInitiator(restZaakInitiatorGegevens)
                }

                Then("and exception should be thrown and the initiator should not be added to the zaak") {
                    exception.message shouldBe "KVK nummer is required for type RSIN"
                    verify(exactly = 0) {
                        zaakService.addInitiatorToZaak(any(), any(), any(), any())
                    }
                }
            }
        }
    }

    Context("Deleting an initiator") {
        Given("A zaak with an initiator") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakRechten = createZaakRechten(toevoegenInitiatorPersoon = true)
            val rolMedewerker = createRolMedewerker()
            val restZaak = createRestZaak()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
            every { policyService.readZaakRechten(zaak, zaakType) } returns zaakRechten
            every { zrcClientService.deleteRol(any(), any()) } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten) } returns restZaak

            When("the initiator is deleted") {
                val updatedRestZaak = zaakRestService.deleteInitiator(zaak.uuid, RESTReden("fake reason"))

                Then("the initiator should be removed from the zaak") {
                    updatedRestZaak shouldBe restZaak
                    verify(exactly = 1) {
                        zrcClientService.deleteRol(rolMedewerker, "fake reason")
                    }
                }
            }
        }
    }

    Context("Updating zaak data") {
        Given("Rest zaak data") {
            val restZaakDataUpdate = createRestZaakDataUpdate()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakdataMap = slot<Map<String, Any>>()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakDataUpdate.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten()
            every { zaakVariabelenService.setZaakdata(restZaakDataUpdate.uuid, capture(zaakdataMap)) } just runs

            When("the zaakdata is requested to be updated") {
                zaakRestService.updateZaakdata(restZaakDataUpdate)

                Then("the zaakdata is correctly updated") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setZaakdata(restZaakDataUpdate.uuid, any())
                    }
                    zaakdataMap.captured shouldBe restZaakDataUpdate.zaakdata
                }
            }
        }
    }
})
