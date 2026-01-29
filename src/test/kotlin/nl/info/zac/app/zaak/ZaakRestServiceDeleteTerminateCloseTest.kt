/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService.INADMISSIBLE_TERMINATION_ID
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.documenten.model.OntkoppeldDocument
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCmmnCompletionParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import nl.info.zac.app.zaak.model.RESTZaakAfsluitenGegevens
import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.createRestDocumentOntkoppelGegevens
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.identification.IdentificationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.exception.ZaakWithADecisionCannotBeTerminatedException
import java.net.URI
import java.util.UUID

@Suppress("LongParameterList")
class ZaakRestServiceDeleteTerminateCloseTest : BehaviorSpec({
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

    Context("Terminating a zaak") {
        Given("A zaak and no managed zaakbeeindigreden") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaakTypeUUID = zaakType.url.extractUuid()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(afbreken = true)
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration
            every {
                zgwApiService.closeZaak(zaak, zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype!!, "Zaak is niet ontvankelijk")
            } just runs
            every { cmmnService.terminateCase(zaak.uuid) } returns Unit

            When("aborted with the hardcoded 'niet ontvankelijk' zaakbeeindigreden") {
                zaakRestService.terminateZaak(
                    zaak.uuid,
                    RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
                )

                Then("it is ended with result") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(
                            zaak,
                            zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype!!,
                            "Zaak is niet ontvankelijk"
                        )
                        cmmnService.terminateCase(zaak.uuid)
                    }
                }
            }
        }

        Given("A zaak with a decision cannot be terminated. A bad request is returned") {
            clearAllMocks()
            val zaakUuid = UUID.randomUUID()
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaak = createZaak(
                uuid = zaakUuid,
                zaaktypeUri = zaakType.url,
                resultaat = URI("https://example.com/${UUID.randomUUID()}")
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(afbreken = true)

            shouldThrow<ZaakWithADecisionCannotBeTerminatedException> {
                zaakRestService.terminateZaak(
                    zaakUuid,
                    RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
                )
            }

            verify(exactly = 0) {
                zgwApiService.closeZaak(any<Zaak>(), any<UUID>(), any())
                cmmnService.terminateCase(any())
            }
        }

        Given("A zaak and managed zaakbeeindigreden") {
            clearAllMocks()
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaakTypeUUID = zaakType.url.extractUuid()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val resultTypeUUID = UUID.randomUUID()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCmmnCompletionParameters = setOf(
                    ZaaktypeCmmnCompletionParameters().apply {
                        id = 123
                        resultaattype = resultTypeUUID
                        zaakbeeindigReden = ZaakbeeindigReden().apply {
                            id = -2
                            naam = "-2 name"
                        }
                    }
                )
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(afbreken = true)
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration
            every { zgwApiService.closeZaak(zaak, resultTypeUUID, "-2 name") } just runs
            every { cmmnService.terminateCase(zaak.uuid) } returns Unit

            When("aborted with managed zaakbeeindigreden") {
                zaakRestService.terminateZaak(zaak.uuid, RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = "-2"))

                Then("it is ended with result") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(zaak, resultTypeUUID, "-2 name")
                        cmmnService.terminateCase(zaak.uuid)
                    }
                }
            }

            When("aborted with invalid zaakbeeindigreden id") {
                clearMocks(zgwApiService, cmmnService)
                val exception = shouldThrow<IllegalArgumentException> {
                    zaakRestService.terminateZaak(
                        zaak.uuid,
                        RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = "not a number")
                    )
                }

                Then("it throws an error") {
                    exception.message shouldBe "For input string: \"not a number\""
                }
            }
        }
    }

    Context("Uncoupling an informatieobject from a zaak") {
        Given(
            "A zaak with a zaakinformatieobject where the corresponding informatieobject is only linked to this zaak"
        ) {
            val zaakUUID = UUID.randomUUID()
            val informatieobjectUUID = UUID.randomUUID()
            val zaak = createZaak(uuid = zaakUUID)
            val enkelvoudiginformatieobject = createEnkelvoudigInformatieObject(uuid = informatieobjectUUID)
            val zaakinformatiebject = createZaakInformatieobjectForReads(
                uuid = informatieobjectUUID
            )
            val restOntkoppelGegevens = createRestDocumentOntkoppelGegevens(
                zaakUUID = zaakUUID,
                documentUUID = informatieobjectUUID,
                reden = "veryFakeReason"
            )
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every { drcClientService.readEnkelvoudigInformatieobject(informatieobjectUUID) } returns enkelvoudiginformatieobject
            every { policyService.readDocumentRechten(enkelvoudiginformatieobject, zaak).ontkoppelen } returns true
            every {
                zrcClientService.listZaakinformatieobjecten(any<ZaakInformatieobjectListParameters>())
            } returns listOf(zaakinformatiebject)
            every { zrcClientService.listZaakinformatieobjecten(enkelvoudiginformatieobject) } returns emptyList()
            every {
                zrcClientService.deleteZaakInformatieobject(zaakinformatiebject.uuid, "veryFakeReason", "Ontkoppeld")
            } just Runs
            every { indexingService.removeInformatieobject(informatieobjectUUID) } just Runs
            every {
                ontkoppeldeDocumentenService.create(enkelvoudiginformatieobject, zaak, "veryFakeReason")
            } returns mockk<OntkoppeldDocument>()

            When("a request is done to unlink the zaakinformatieobject from the zaak") {
                zaakRestService.ontkoppelInformatieObject(restOntkoppelGegevens)

                Then(
                    """
                    the zaakinformatieobject is unlinked from the zaak and the related informatieobject is removed from the search index 
                    and is added as an inboxdocument
                    """.trimIndent()
                ) {
                    verify(exactly = 1) {
                        zrcClientService.deleteZaakInformatieobject(
                            zaakinformatiebject.uuid,
                            "veryFakeReason",
                            "Ontkoppeld"
                        )
                        indexingService.removeInformatieobject(informatieobjectUUID)
                        ontkoppeldeDocumentenService.create(enkelvoudiginformatieobject, zaak, "veryFakeReason")
                    }
                }
            }
        }
    }

    Context("Closing a zaak") {
        Given("open zaak without locked informatieobjecten") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val reden = "Fake reden"
            val resultaattypeUuid = UUID.randomUUID()
            val restZaakAfsluitenGegevens = RESTZaakAfsluitenGegevens(reden, resultaattypeUuid)

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType) } returns createZaakRechten(behandelen = true)
            every { zgwApiService.closeZaak(zaak, resultaattypeUuid, reden) } just runs

            When("zaak is closed") {
                zaakRestService.closeZaak(zaak.uuid, restZaakAfsluitenGegevens)

                Then("result and status are correctly set") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(zaak, resultaattypeUuid, reden)
                    }
                }
            }
        }
    }
})
