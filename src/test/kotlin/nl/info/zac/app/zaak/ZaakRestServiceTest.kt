/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService.INADMISSIBLE_TERMINATION_ID
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.or.`object`.ObjectsClientService
import nl.info.client.or.`object`.model.createORObject
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoonForReads
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoonForReads
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.model.createZaakobjectOpenbareRuimte
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaakAfzender
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.exception.BetrokkeneNotAllowedException
import nl.info.zac.app.zaak.exception.CommunicationChannelNotFound
import nl.info.zac.app.zaak.exception.DueDateNotAllowed
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import nl.info.zac.app.zaak.model.RESTZaakAfsluitenGegevens
import nl.info.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.createBetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.createRESTGeometry
import nl.info.zac.app.zaak.model.createRESTZaakAanmaakGegevens
import nl.info.zac.app.zaak.model.createRestDetachDocumentData
import nl.info.zac.app.zaak.model.createRestGroup
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakCreateData
import nl.info.zac.app.zaak.model.createRestZaakDataUpdate
import nl.info.zac.app.zaak.model.createRestZaakInitiatorGegevens
import nl.info.zac.app.zaak.model.createRestZaakLocatieGegevens
import nl.info.zac.app.zaak.model.createRestZaaktype
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.identification.IdentificationService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createOverigeRechten
import nl.info.zac.policy.output.createOverigeRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.productaanvraag.InboxProductaanvraagService
import nl.info.zac.productaanvraag.ProductaanvraagDocumentService
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.exception.ZaakWithABesluitCannotBeTerminatedException
import org.apache.http.HttpStatus
import org.flowable.task.api.Task
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.util.*
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration as createAdminZaaktypeBpmnConfiguration

@Suppress("LongParameterList", "LargeClass")
class ZaakRestServiceTest : BehaviorSpec({
    val bpmnService = mockk<BpmnService>()
    val configurationService = mockk<ConfigurationService>()
    val cmmnService = mockk<CMMNService>()
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val healthCheckService = mockk<HealthCheckService>()
    val identityService = mockk<IdentityService>()
    val inboxProductaanvraagService = mockk<InboxProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val objectsClientService = mockk<ObjectsClientService>()
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val productaanvraagDocumentService = mockk<ProductaanvraagDocumentService>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
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
    val zaakRestService = ZaakRestService(
        bpmnService = bpmnService,
        cmmnService = cmmnService,
        configurationService = configurationService,
        drcClientService = drcClientService,
        eventingService = eventingService,
        healthCheckService = healthCheckService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        indexingService = indexingService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        detachedDocumentService = detachedDocumentService,
        suspensionZaakHelper = suspensionZaakHelper,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        productaanvraagDocumentService = productaanvraagDocumentService,
        restZaakConverter = restZaakConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        restZaaktypeConverter = restZaaktypeConverter,
        signaleringService = signaleringService,
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

    afterEach {
        checkUnnecessaryStub()
    }

    Context("Closing a zaak") {
        Given("open zaak without locked informatieobjecten") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val reden = "Fake reden"
            val resultaattypeUuid = UUID.randomUUID()
            val restZaakAfsluitenGegevens = RESTZaakAfsluitenGegevens(reden, resultaattypeUuid)
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(behandelen = true)
            every { zgwApiService.closeZaak(zaak, resultaattypeUuid, reden) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

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

    Context("Creating a zaak") {
        Context("Creating a CMMN zaak") {
            Given("CMMN zaak input data is provided") {
                val group = createGroup()
                val bsn = "12345678"
                val formulierData = mapOf(Pair("fakeKey", "fakeValue"))
                val objectRegistratieObject = createORObject()
                val productaanvraagDimpact = createProductaanvraagDimpact()
                val restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now().minusDays(1))
                val restZaak = createRestZaak(einddatumGepland = LocalDate.now().minusDays(1))
                val zaakTypeUUID = UUID.randomUUID()
                val zaakType = createZaakType(
                    omschrijving = ZAAK_TYPE_1_OMSCHRIJVING,
                    servicenorm = "P10D",
                    uri = URI("https://example.com/zaaktypes/$zaakTypeUUID")
                )
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                    restZaakCreateData = createRestZaakCreateData(
                        restZaakType = RestZaaktype(
                            uuid = zaakTypeUUID
                        ),
                        restGroup = createRestGroup(
                            id = group.name
                        ),

                    ),
                    zaakTypeUUID = zaakTypeUUID
                )
                val rolMedewerker = createRolMedewerker()
                val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
                val user = createLoggedInUser()
                val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
                val zaakObjectPand = createZaakobjectPand()
                val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
                val zaak = createZaak(zaaktypeUri = zaakType.url)
                val bronOrganisatie = "fakeBronOrganisatie"
                val verantwoordelijkeOrganisatie = "fakeVerantwoordelijkeOrganisatie"
                val zaakCreatedSlot = slot<Zaak>()
                val updatedRolesSlot = mutableListOf<Rol<*>>()
                val loggedInUser = createLoggedInUser()

                every { configurationService.readBronOrganisatie() } returns bronOrganisatie
                every { configurationService.readVerantwoordelijkeOrganisatie() } returns verantwoordelijkeOrganisatie
                every { cmmnService.startCase(zaak, zaakType, zaaktypeCmmnConfiguration, null) } just runs
                every {
                    inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag!!.id)
                } just runs
                every {
                    objectsClientService
                        .readObject(restZaakAanmaakGegevens.inboxProductaanvraag!!.productaanvraagObjectUUID)
                } returns objectRegistratieObject
                every { productaanvraagService.getAanvraaggegevens(objectRegistratieObject) } returns formulierData
                every {
                    productaanvraagService.getProductaanvraag(objectRegistratieObject)
                } returns productaanvraagDimpact
                every { productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
                every {
                    productaanvraagDocumentService.pairBijlagenWithZaak(productaanvraagDimpact.bijlagen, zaak.url)
                } just runs
                every {
                    productaanvraagDocumentService.pairProductaanvraagWithZaak(
                        objectRegistratieObject,
                        zaak.url
                    )
                } just runs
                every { restZaakConverter.toRestZaak(zaak, zaakType, any(), loggedInUser) } returns restZaak
                every { zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID) } returns zaaktypeCmmnConfiguration
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
                } returns zaaktypeCmmnConfiguration
                every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
                every { zgwApiService.createZaak(capture(zaakCreatedSlot)) } returns zaak
                every {
                    zrcClientService.updateRol(
                        any(),
                        capture(updatedRolesSlot),
                        any()
                    )
                } just runs
                every { zrcClientService.createZaakobject(any<ZaakobjectPand>()) } returns zaakObjectPand
                every { zrcClientService.createZaakobject(any<ZaakobjectOpenbareRuimte>()) } returns zaakObjectOpenbareRuimte
                every { zaakService.bepaalRolGroep(group, zaak) } returns rolOrganisatorischeEenheid
                every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker
                every { zaakService.readZaakTypeByUUID(zaakTypeUUID) } returns zaakType
                every {
                    zaakService.addInitiatorToZaak(
                        identificationType = restZaakCreateData.initiatorIdentificatie!!.type,
                        identification = bsn,
                        zaak = zaak,
                        explanation = "Aanmaken zaak"
                    )
                } just runs
                every { loggedInUserInstance.get() } returns loggedInUser

                When(
                    """
                a zaak is created for a zaaktype for which the user has permissions and no BPMN process definition is found
                    """.trimMargin()
                ) {
                    every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep!!.id) } returns group
                    every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar!!.id) } returns user
                    every {
                        policyService.readOverigeRechten(zaakType.omschrijving)
                    } returns createOverigeRechtenAllDeny(startenZaak = true)
                    every {
                        policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                    } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)
                    every {
                        identificationService.replaceKeyWithBsn(restZaakAanmaakGegevens.zaak.initiatorIdentificatie!!.temporaryPersonId!!)
                    } returns bsn

                    val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

                    Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                        restZaakReturned shouldBe restZaak
                        verify(exactly = 1) {
                            zgwApiService.createZaak(any())
                            cmmnService.startCase(zaak, zaakType, zaaktypeCmmnConfiguration, null)
                        }
                        verify(exactly = 2) {
                            zrcClientService.createZaakobject(any())
                            zrcClientService.updateRol(
                                zaak,
                                any<Rol<*>>(),
                                "Aanmaken zaak"
                            )
                        }
                        updatedRolesSlot shouldHaveSize 2
                        with(updatedRolesSlot[0]) {
                            betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                            omschrijving shouldBe rolOrganisatorischeEenheid.omschrijving
                            omschrijvingGeneriek shouldBe rolOrganisatorischeEenheid.omschrijvingGeneriek
                            roltoelichting shouldBe rolOrganisatorischeEenheid.roltoelichting
                        }
                        with(updatedRolesSlot[1]) {
                            betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                            omschrijving shouldBe rolMedewerker.omschrijving
                            omschrijvingGeneriek shouldBe rolMedewerker.omschrijvingGeneriek
                            roltoelichting shouldBe rolMedewerker.roltoelichting
                        }
                    }
                }
            }
        }

        Context("Creating a BPMN zaak") {
            Given("BPMN zaak input data is provided") {
                val group = createGroup()
                val bsn = "12345678"
                val formulierData = mapOf(Pair("fakeKey", "fakeValue"))
                val objectRegistratieObject = createORObject()
                val productaanvraagDimpact = createProductaanvraagDimpact()
                val restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now().minusDays(1))
                val restZaak = createRestZaak(einddatumGepland = LocalDate.now().minusDays(1))
                val zaakTypeUUID = UUID.randomUUID()
                val zaakType = createZaakType(
                    omschrijving = ZAAK_TYPE_1_OMSCHRIJVING,
                    servicenorm = "P10D",
                    uri = URI("https://example.com/zaaktypes/$zaakTypeUUID")
                )
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                    restZaakCreateData = createRestZaakCreateData(
                        restZaakType = RestZaaktype(
                            uuid = zaakTypeUUID
                        ),
                        restGroup = createRestGroup(
                            id = group.name
                        ),

                    ),
                    zaakTypeUUID = zaakTypeUUID
                )
                val rolMedewerker = createRolMedewerker()
                val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
                val user = createLoggedInUser()
                val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
                val zaakData = mapOf(
                    "zaakGroep" to restZaak.groep!!.id,
                    "zaakBehandelaar" to restZaak.behandelaar!!.id,
                    "zaakCommunicatiekanaal" to restZaak.communicatiekanaal!!
                )
                val zaakObjectPand = createZaakobjectPand()
                val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
                val zaak = createZaak(zaaktypeUri = zaakType.url)
                val bronOrganisatie = "fakeBronOrganisatie"
                val verantwoordelijkeOrganisatie = "fakeVerantwoordelijkeOrganisatie"
                val zaakCreatedSlot = slot<Zaak>()
                val updatedRolesSlot = mutableListOf<Rol<*>>()
                val loggedInUser = createLoggedInUser()

                every { configurationService.readBronOrganisatie() } returns bronOrganisatie
                every { configurationService.readVerantwoordelijkeOrganisatie() } returns verantwoordelijkeOrganisatie
                every {
                    bpmnService.startProcess(zaak, zaakType, zaaktypeBpmnConfiguration.bpmnProcessDefinitionKey, zaakData)
                } just runs
                every {
                    inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag!!.id)
                } just runs
                every {
                    objectsClientService
                        .readObject(restZaakAanmaakGegevens.inboxProductaanvraag!!.productaanvraagObjectUUID)
                } returns objectRegistratieObject
                every { productaanvraagService.getAanvraaggegevens(objectRegistratieObject) } returns formulierData
                every {
                    productaanvraagService.getProductaanvraag(objectRegistratieObject)
                } returns productaanvraagDimpact
                every { productaanvraagDocumentService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
                every {
                    productaanvraagDocumentService.pairBijlagenWithZaak(productaanvraagDimpact.bijlagen, zaak.url)
                } just runs
                every {
                    productaanvraagDocumentService.pairProductaanvraagWithZaak(
                        objectRegistratieObject,
                        zaak.url
                    )
                } just runs
                every { restZaakConverter.toRestZaak(zaak, zaakType, any(), loggedInUser) } returns restZaak
                every { zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID) } returns zaaktypeBpmnConfiguration
                every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
                every { zgwApiService.createZaak(capture(zaakCreatedSlot)) } returns zaak
                every {
                    zrcClientService.updateRol(
                        any(),
                        capture(updatedRolesSlot),
                        any()
                    )
                } just runs
                every { zrcClientService.createZaakobject(any<ZaakobjectPand>()) } returns zaakObjectPand
                every { zrcClientService.createZaakobject(any<ZaakobjectOpenbareRuimte>()) } returns zaakObjectOpenbareRuimte
                every { zaakService.bepaalRolGroep(group, zaak) } returns rolOrganisatorischeEenheid
                every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker
                every { zaakService.readZaakTypeByUUID(zaakTypeUUID) } returns zaakType
                every {
                    zaakService.addInitiatorToZaak(
                        identificationType = restZaakCreateData.initiatorIdentificatie!!.type,
                        identification = bsn,
                        zaak = zaak,
                        explanation = "Aanmaken zaak"
                    )
                } just runs
                every { bpmnService.findProcessDefinitionForZaaktype(zaakTypeUUID) } returns zaaktypeBpmnConfiguration
                every { loggedInUserInstance.get() } returns loggedInUser

                When(
                    """
                a zaak is created for a zaaktype for which the user has permissions and no BPMN process definition is found
                    """.trimMargin()
                ) {
                    every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep!!.id) } returns group
                    every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar!!.id) } returns user
                    every {
                        policyService.readOverigeRechten(zaakType.omschrijving)
                    } returns createOverigeRechtenAllDeny(startenZaak = true)
                    every {
                        policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                    } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)
                    every {
                        identificationService.replaceKeyWithBsn(restZaakAanmaakGegevens.zaak.initiatorIdentificatie!!.temporaryPersonId!!)
                    } returns bsn

                    val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

                    Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                        restZaakReturned shouldBe restZaak
                        verify(exactly = 1) {
                            zgwApiService.createZaak(any())
                            bpmnService.startProcess(
                                zaak,
                                zaakType,
                                zaaktypeBpmnConfiguration.bpmnProcessDefinitionKey,
                                zaakData
                            )
                        }
                        verify(exactly = 2) {
                            zrcClientService.createZaakobject(any())
                            zrcClientService.updateRol(
                                zaak,
                                any<Rol<*>>(),
                                "Aanmaken zaak"
                            )
                        }
                        updatedRolesSlot shouldHaveSize 2
                        with(updatedRolesSlot[0]) {
                            betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                            omschrijving shouldBe rolOrganisatorischeEenheid.omschrijving
                            omschrijvingGeneriek shouldBe rolOrganisatorischeEenheid.omschrijvingGeneriek
                            roltoelichting shouldBe rolOrganisatorischeEenheid.roltoelichting
                        }
                        with(updatedRolesSlot[1]) {
                            betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                            omschrijving shouldBe rolMedewerker.omschrijving
                            omschrijvingGeneriek shouldBe rolMedewerker.omschrijvingGeneriek
                            roltoelichting shouldBe rolMedewerker.roltoelichting
                        }
                    }
                }
            }
        }

        Context("Creating a zaak with no communication channel") {
            Given("zaak input data has no communication channel") {
                val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                    restZaakCreateData = createRestZaakCreateData(communicatiekanaal = null)
                )
                every { zaakService.readZaakTypeByUUID(any()) } returns zaakType
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                } returns createZaaktypeBpmnConfiguration()
                every { policyService.readOverigeRechten(zaakType.omschrijving) } returns createOverigeRechten()
                every { loggedInUserInstance.get() } returns createLoggedInUser()

                When("zaak creation is attempted") {
                    val exception = shouldThrow<CommunicationChannelNotFound> {
                        zaakRestService.createZaak(restZaakAanmaakGegevens)
                    }

                    Then("an exception is thrown") {
                        exception.errorCode shouldNotBe null
                    }
                }
            }
        }

        Context("Creating a zaak with blank communication channel") {
            Given("zaak input data has blank communication channel") {
                val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                    restZaakCreateData = createRestZaakCreateData(communicatiekanaal = "      ")
                )
                every { zaakService.readZaakTypeByUUID(any<UUID>()) } returns zaakType
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                } returns createZaaktypeBpmnConfiguration()
                every { policyService.readOverigeRechten(zaakType.omschrijving) } returns createOverigeRechten()
                every { loggedInUserInstance.get() } returns createLoggedInUser()

                When("zaak creation is attempted") {
                    val exception = shouldThrow<CommunicationChannelNotFound> {
                        zaakRestService.createZaak(restZaakAanmaakGegevens)
                    }

                    Then("an exception is thrown") {
                        exception.errorCode shouldNotBe null
                    }
                }
            }
        }

        Context("Creating a zaak with due date when servicenorm is not specified") {
            Given("zaak input data has due date when servicenorm is not specified in OpenZaak") {
                val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
                val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
                    restZaakCreateData = createRestZaakCreateData(einddatumGepland = LocalDate.now())
                )
                every { zaakService.readZaakTypeByUUID(any<UUID>()) } returns zaakType
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                } returns createZaaktypeBpmnConfiguration()
                every { policyService.readOverigeRechten(zaakType.omschrijving) } returns createOverigeRechten()
                every { loggedInUserInstance.get() } returns createLoggedInUser()

                When("zaak creation is attempted") {
                    val exception = shouldThrow<DueDateNotAllowed> {
                        zaakRestService.createZaak(restZaakAanmaakGegevens)
                    }

                    Then("an exception is thrown") {
                        exception.errorCode shouldNotBe null
                    }
                }
            }
        }

        Context("Creating a zaak with betrokkene not allowed") {
            Given("A initiator is posted on a zaak create with an initiator") {
                When("This is not allowed in the zaak afhandel parameters") {
                    val betrokkeneKoppelingen = createBetrokkeneKoppelingen(
                        brpKoppelen = false,
                        zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
                    )
                    val zaaktypeCmmnConfiguration =
                        createZaaktypeCmmnConfiguration(zaaktypeBetrokkeneParameters = betrokkeneKoppelingen)
                    val zaakType = createZaakType()
                    val restZaakCreateData = createRestZaakCreateData()
                    val zaakAanmaakGegevens = createRESTZaakAanmaakGegevens(restZaakCreateData = restZaakCreateData)

                    every { zaakService.readZaakTypeByUUID(restZaakCreateData.zaaktype.uuid) } returns zaakType
                    every {
                        zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                    } returns zaaktypeCmmnConfiguration
                    every { loggedInUserInstance.get() } returns createLoggedInUser()

                    val exception = shouldThrow<BetrokkeneNotAllowedException> {
                        zaakRestService.createZaak(zaakAanmaakGegevens)
                    }

                    Then("An error should be thrown") {
                        exception.errorCode shouldBe ErrorCode.ERROR_CODE_CASE_BETROKKENE_NOT_ALLOWED
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
            val loggedInUser = createLoggedInUser()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { zrcClientService.deleteRol(any(), any()) } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser) } returns restZaak
            every { loggedInUserInstance.get() } returns loggedInUser

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

    Context("Detaching an informatieobject from a zaak") {
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
            val restOntkoppelGegevens = createRestDetachDocumentData(
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
                detachedDocumentService.create(enkelvoudiginformatieobject, zaak, "veryFakeReason")
            } just Runs

            When("a request is done to unlink the zaakinformatieobject from the zaak") {
                zaakRestService.detachZaakinformatieobject(restOntkoppelGegevens)

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
                        detachedDocumentService.create(enkelvoudiginformatieobject, zaak, "veryFakeReason")
                    }
                }
            }
        }

        Given(
            "A zaak with a zaakinformatieobject where the corresponding informatieobject is also linked to another zaak"
        ) {
            val zaakUUID = UUID.randomUUID()
            val informatieobjectUUID = UUID.randomUUID()
            val zaak = createZaak(uuid = zaakUUID)
            val enkelvoudiginformatieobject = createEnkelvoudigInformatieObject(uuid = informatieobjectUUID)
            val zaakinformatiebject = createZaakInformatieobjectForReads(
                uuid = informatieobjectUUID
            )
            val zaakInformatieobject2 = createZaakInformatieobjectForReads()
            val restOntkoppelGegevens = createRestDetachDocumentData(
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
            every { zrcClientService.listZaakinformatieobjecten(enkelvoudiginformatieobject) } returns listOf(zaakInformatieobject2)
            every {
                zrcClientService.deleteZaakInformatieobject(zaakinformatiebject.uuid, "veryFakeReason", "Ontkoppeld")
            } just Runs

            When("a request is done to unlink the zaakinformatieobject from the zaak") {
                zaakRestService.detachZaakinformatieobject(restOntkoppelGegevens)

                Then(
                    "the zaakinformatieobject is unlinked from the zaak"
                ) {
                    verify(exactly = 1) {
                        zrcClientService.deleteZaakInformatieobject(
                            zaakinformatiebject.uuid,
                            "veryFakeReason",
                            "Ontkoppeld"
                        )
                    }
                }
                And("the informatieobject is not removed from the search index and is not added as an inboxdocument") {
                    verify(exactly = 0) {
                        indexingService.removeInformatieobject(informatieobjectUUID)
                        detachedDocumentService.create(
                            any<EnkelvoudigInformatieObject>(),
                            any<Zaak>(),
                            any()
                        )
                    }
                }
            }
        }
    }

    Context("Downloading a process diagram") {
        Given("An existing BPMN process diagram for a given zaak UUID") {
            val uuid = UUID.randomUUID()
            val zaak = createZaak(uuid = uuid)
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(lezen = true)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { bpmnService.getProcessDiagram(uuid) } returns ByteArrayInputStream("fakeDiagram".toByteArray())

            When("the process diagram is requested") {
                val response = zaakRestService.downloadProcessDiagram(uuid)

                Then(
                    "a HTTP OK response is returned with a 'Content-Disposition' HTTP header and the diagram as input stream"
                ) {
                    with(response) {
                        status shouldBe HttpStatus.SC_OK
                        headers["Content-Disposition"]!![0] shouldBe """inline; filename="process-diagram.png"""".trimIndent()
                        (entity as InputStream).bufferedReader().use { it.readText() } shouldBe "fakeDiagram"
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
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser(email = "fake-medewerker@example.com")
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
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(lezen = true)
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration
            every { configurationService.readGemeenteMail() } returns "fake-gemeente@example.com"
            every { loggedInUserInstance.get() } returns loggedInUser

            When("the zaakafzenders are requested") {
                val returnedRestZaakAfzenders = zaakRestService.listAfzendersVoorZaak(zaakUUID)

                Then("the zaakafzenders are returned, including special mails for GEMEENTE and MEDEWERKER") {
                    with(returnedRestZaakAfzenders) {
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
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser(email = "fake-medewerker@example.com")
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(zaaktypeUUID = zaakTypeUUID)
            zaaktypeCmmnConfiguration.setZaakAfzenders(emptyList())
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(lezen = true)
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaakTypeUUID)
            } returns zaaktypeCmmnConfiguration
            every { configurationService.readGemeenteMail() } returns "fake-gemeente@example.com"
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val expectedPersonId = UUID.randomUUID()
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
            val loggedInUser = createLoggedInUser()
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { zaakService.listBetrokkenenforZaak(zaak) } returns betrokkeneRoles
            every { identificationService.replaceBsnWithKey(rolNatuurlijkPersoon.identificatienummer!!) } returns expectedPersonId
            every { loggedInUserInstance.get() } returns loggedInUser

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
                            temporaryPersonId shouldBe expectedPersonId
                            identificatieType shouldBe IdentificatieType.BSN
                        }
                        with(this[1]) {
                            rolid shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.uuid.toString()
                            roltype shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.omschrijving
                            roltoelichting shouldBe rolNietNatuurlijkPersoonWithVestigingsnummer.roltoelichting
                            type shouldBe "NIET_NATUURLIJK_PERSOON"
                            identificatieType shouldBe IdentificatieType.VN
                        }
                        with(last()) {
                            rolid shouldBe rolNietNatuurlijkPersoonWithRSIN.uuid.toString()
                            roltype shouldBe rolNietNatuurlijkPersoonWithRSIN.omschrijving
                            roltoelichting shouldBe rolNietNatuurlijkPersoonWithRSIN.roltoelichting
                            type shouldBe "NIET_NATUURLIJK_PERSOON"
                            identificatieType shouldBe IdentificatieType.RSIN
                        }
                    }
                }
            }
        }
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
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(it.url.extractUuid())
                } returns createZaaktypeCmmnConfiguration()
            }
            every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI

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
                }

                every { configurationService.readDefaultCatalogusURI() } returns defaultCatalogueURI
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
                    type = IdentificatieType.BSN
                )
            )
            val loggedInUser = createLoggedInUser()
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser) } returns restZaak
            every { signaleringService.deleteSignaleringenForZaak(zaak) } returns 1
            every { loggedInUserInstance.get() } returns loggedInUser

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

    Context("Terminating a zaak") {
        Given("A zaak and no managed zaakbeeindigreden") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaakTypeUUID = zaakType.url.extractUuid()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val zaaktypeConfiguration = createZaaktypeCmmnConfiguration()
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(afbreken = true)
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID)
            } returns zaaktypeConfiguration
            every {
                zgwApiService.closeZaak(zaak, zaaktypeConfiguration.nietOntvankelijkResultaattype!!, "Zaak is niet ontvankelijk")
            } just runs
            every { cmmnService.terminateCase(zaak.uuid) } returns Unit
            every { loggedInUserInstance.get() } returns loggedInUser

            When("aborted with the hardcoded 'niet ontvankelijk' zaakbeeindigreden") {
                zaakRestService.terminateZaak(
                    zaak.uuid,
                    RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
                )

                Then("it is ended with result") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(
                            zaak,
                            zaaktypeConfiguration.nietOntvankelijkResultaattype!!,
                            "Zaak is niet ontvankelijk"
                        )
                        cmmnService.terminateCase(zaak.uuid)
                    }
                }
            }
        }

        Given("A zaak with a besluit cannot be terminated. A bad request is returned") {
            val zaakUuid = UUID.randomUUID()
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaak = createZaak(
                uuid = zaakUuid,
                zaaktypeUri = zaakType.url,
                resultaat = URI("https://example.com/${UUID.randomUUID()}")
            )
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(afbreken = true)
            every { loggedInUserInstance.get() } returns loggedInUser

            When("trying to terminate the zaak") {
                shouldThrow<ZaakWithABesluitCannotBeTerminatedException> {
                    zaakRestService.terminateZaak(
                        zaakUuid,
                        RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
                    )
                }

                Then(
                    "it throws ZaakWithADecisionCannotBeTerminatedException and no close or terminate calls are made"
                ) {
                    verify(exactly = 0) {
                        zgwApiService.closeZaak(any<Zaak>(), any<UUID>(), any())
                        cmmnService.terminateCase(any())
                    }
                }
            }
        }

        Given("A zaak and managed zaakbeeindigreden") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaakTypeUUID = zaakType.url.extractUuid()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val resultTypeUUID = UUID.randomUUID()
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCompletionParameters = setOf(
                    ZaaktypeCompletionParameters().apply {
                        id = 123
                        resultaattype = resultTypeUUID
                        zaakbeeindigReden = ZaakbeeindigReden().apply {
                            id = -2
                            naam = "-2 name"
                        }
                    }
                )
            )
            val loggedInUser = createLoggedInUser()

            When("aborted with managed zaakbeeindigreden") {
                every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(afbreken = true)
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID)
                } returns zaaktypeCmmnConfiguration
                every { zgwApiService.closeZaak(zaak, resultTypeUUID, "-2 name") } just runs
                every { cmmnService.terminateCase(zaak.uuid) } returns Unit
                every { loggedInUserInstance.get() } returns loggedInUser
                zaakRestService.terminateZaak(zaak.uuid, RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = "-2"))

                Then("it is ended with result") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(zaak, resultTypeUUID, "-2 name")
                        cmmnService.terminateCase(zaak.uuid)
                    }
                }
            }

            When("aborted with invalid zaakbeeindigreden id") {
                every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(afbreken = true)
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID)
                } returns zaaktypeCmmnConfiguration
                every { loggedInUserInstance.get() } returns loggedInUser
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

        Given("A BPMN zaak and no managed zaakbeeindigreden") {
            val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
            val zaakTypeUUID = zaakType.url.extractUuid()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val zaaktypeConfiguration = createAdminZaaktypeBpmnConfiguration()
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(afbreken = true)
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(zaakTypeUUID)
            } returns zaaktypeConfiguration
            every {
                zgwApiService.closeZaak(zaak, zaaktypeConfiguration.nietOntvankelijkResultaattype!!, "Zaak is niet ontvankelijk")
            } just runs
            every { bpmnService.terminateCase(zaak.uuid) } returns Unit
            every { loggedInUserInstance.get() } returns loggedInUser

            When("aborted with the hardcoded 'niet ontvankelijk' zaakbeeindigreden") {
                zaakRestService.terminateZaak(
                    zaak.uuid,
                    RESTZaakAfbrekenGegevens(zaakbeeindigRedenId = INADMISSIBLE_TERMINATION_ID)
                )

                Then("it is ended with result") {
                    verify(exactly = 1) {
                        zgwApiService.closeZaak(
                            zaak,
                            zaaktypeConfiguration.nietOntvankelijkResultaattype!!,
                            "Zaak is niet ontvankelijk"
                        )
                        bpmnService.terminateCase(zaak.uuid)
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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolMedewerker
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { zrcClientService.deleteRol(any(), any()) } just runs
            every { zaakService.addInitiatorToZaak(any(), any(), any(), any()) } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser) } returns restZaak
            every {
                identificationService.replaceKeyWithBsn(restZaakInitiatorGegevens.betrokkeneIdentificatie.temporaryPersonId!!)
            } returns bsn
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                zaakService.addInitiatorToZaak(
                    IdentificatieType.VN,
                    "$kvkNummer|$vestigingsnummer",
                    zaak,
                    any()
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser) } returns createRestZaak()
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                zaakService.addInitiatorToZaak(
                    IdentificatieType.RSIN,
                    kvkNummer,
                    zaak,
                    any()
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten, loggedInUser) } returns createRestZaak()
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
            } returns Pair(zaak, zaakType)
            every { zgwApiService.findInitiatorRoleForZaak(any()) } returns null
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { zrcClientService.patchZaak(zaak.uuid, any(), changeDescription) } returns patchedZaak
            every { task.id } returns "id"
            every { eventingService.send(any<ScreenEvent>()) } just runs
            every { restZaakConverter.toRestZaak(patchedZaak, zaakType, zaakRechten, loggedInUser) } returns patchedRestZaak
            every {
                identityService.validateIfUserIsInGroup(restZaakCreateData.behandelaar!!.id, restZaakCreateData.groep!!.id)
            } just runs
            every {
                zaakVariabelenService.setCommunicationChannel(
                    zaak.uuid,
                    restZaakEditMetRedenGegevens.zaak.communicatiekanaal!!
                )
            } just runs
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns zaaktypeBpmnConfiguration
            every { loggedInUserInstance.get() } returns loggedInUser

            When("zaak final date is set to a later date") {
                every {
                    suspensionZaakHelper.adjustFinalDateForOpenTasks(zaak.uuid, newZaakFinalDate)
                } returns listOf(task, task)

                val updatedRestZaak = zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)

                Then("zaak is updated with the new data") {
                    updatedRestZaak shouldBe patchedRestZaak
                }

                And("the communication channel is exposed to zaak data") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setCommunicationChannel(
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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { identityService.validateIfUserIsInGroup(any(), any()) } throws InputValidationFailedException()
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns createZaaktypeCmmnConfiguration()
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { loggedInUserInstance.get() } returns loggedInUser

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
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
                every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
                every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten, loggedInUser) } returns restZaak
                every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                } returns createZaaktypeCmmnConfiguration()

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { loggedInUserInstance.get() } returns loggedInUser

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
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
                every { identityService.validateIfUserIsInGroup(any(), any()) } just runs
                every { restZaakConverter.toRestZaak(any(), zaakType, zaakRechten, loggedInUser) } returns restZaak
                every { zrcClientService.patchZaak(zaak.uuid, any(), any()) } returns zaak
                every {
                    suspensionZaakHelper.adjustFinalDateForOpenTasks(zaak.uuid, newZaakFinalDate)
                } returns emptyList()
                every { eventingService.send(any<ScreenEvent>()) } just runs
                every {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
                } returns createZaaktypeCmmnConfiguration()

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
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(any<UUID>())
            } returns createZaaktypeCmmnConfiguration()
            every { loggedInUserInstance.get() } returns loggedInUser

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

    Context("Updating zaak data") {
        Given("Rest zaak data") {
            val restZaakDataUpdate = createRestZaakDataUpdate()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakdataMap = slot<Map<String, Any>>()
            val loggedInUser = createLoggedInUser()

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakDataUpdate.uuid)
            } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { zaakVariabelenService.setZaakdata(restZaakDataUpdate.uuid, capture(zaakdataMap)) } just runs
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), reason) } returns updatedZaak
            every { restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten, loggedInUser) } returns updatedRestZaak
            every { loggedInUserInstance.get() } returns loggedInUser

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
            val loggedInUser = createLoggedInUser()

            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid)
            } returns Pair(zaak, zaakType)
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), reason) } returns updatedZaak
            every { restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten, loggedInUser) } returns updatedRestZaak
            every { loggedInUserInstance.get() } returns loggedInUser

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
})
