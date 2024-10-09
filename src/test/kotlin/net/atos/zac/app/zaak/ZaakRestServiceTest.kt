/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.runTest
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createRolMedewerker
import net.atos.client.zgw.zrc.model.createRolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakobjectOpenbareRuimte
import net.atos.client.zgw.zrc.model.createZaakobjectPand
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.app.audit.converter.RESTHistorieRegelConverter
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.besluit.BesluitService
import net.atos.zac.app.zaak.ZaakRestService.Companion.AANVULLENDE_INFORMATIE_TASK_NAME
import net.atos.zac.app.zaak.converter.RestBesluitConverter
import net.atos.zac.app.zaak.converter.RestGeometryConverter
import net.atos.zac.app.zaak.converter.RestZaakConverter
import net.atos.zac.app.zaak.converter.RestZaakOverzichtConverter
import net.atos.zac.app.zaak.converter.RestZaaktypeConverter
import net.atos.zac.app.zaak.converter.historie.RESTZaakHistorieRegelConverter
import net.atos.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.app.zaak.model.RestZaaktype
import net.atos.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import net.atos.zac.app.zaak.model.createRESTZaakAanmaakGegevens
import net.atos.zac.app.zaak.model.createRESTZaakKoppelGegevens
import net.atos.zac.app.zaak.model.createRESTZaakToekennenGegevens
import net.atos.zac.app.zaak.model.createRESTZakenVerdeelGegevens
import net.atos.zac.app.zaak.model.createRestGroup
import net.atos.zac.app.zaak.model.createRestZaak
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.bpmn.BPMNService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.healthcheck.HealthCheckService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.createGroup
import net.atos.zac.identity.model.createUser
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createOverigeRechtenAllDeny
import net.atos.zac.policy.output.createWerklijstRechten
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.policy.output.createZaakRechtenAllDeny
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.productaanvraag.createProductaanvraagDimpact
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.zaak.ZaakService
import net.atos.zac.zoeken.IndexingService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.flowable.task.api.Task
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Optional
import java.util.UUID

@Suppress("LongParameterList")
class ZaakRestServiceTest : BehaviorSpec({
    val besluitService = mockk<BesluitService>()
    val bpmnService = mockk<BPMNService>()
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
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val policyService = mockk<PolicyService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val restBAGConverter = mockk<RESTBAGConverter>()
    val restBesluitConverter = mockk<RestBesluitConverter>()
    val restGeometryConverter = mockk<RestGeometryConverter>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val restZaaktypeConverter = mockk<RestZaaktypeConverter>()
    val restHistorieRegelConverter = mockk<RESTHistorieRegelConverter>()
    val signaleringService = mockk<SignaleringService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val restZaakHistorieRegelConverter = mockk<RESTZaakHistorieRegelConverter>()

    val zaakRestService = ZaakRestService(
        besluitService = besluitService,
        cmmnService = cmmnService,
        identityService = identityService,
        inboxProductaanvraagService = inboxProductaanvraagService,
        loggedInUserInstance = loggedInUserInstance,
        objectsClientService = objectsClientService,
        policyService = policyService,
        productaanvraagService = productaanvraagService,
        restBAGConverter = restBAGConverter,
        restZaakConverter = restZaakConverter,
        zaakafhandelParameterService = zaakafhandelParameterService,
        zaakVariabelenService = zaakVariabelenService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zaakService = zaakService,
        indexingService = indexingService,
        restHistorieRegelConverter = restHistorieRegelConverter,
        restBesluitConverter = restBesluitConverter,
        bpmnService = bpmnService,
        brcClientService = brcClientService,
        configuratieService = configuratieService,
        drcClientService = drcClientService,
        eventingService = eventingService,
        healthCheckService = healthCheckService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        opschortenZaakHelper = opschortenZaakHelper,
        restGeometryConverter = restGeometryConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        signaleringService = signaleringService,
        flowableTaskService = flowableTaskService,
        restZaaktypeConverter = restZaaktypeConverter,
        restZaakHistorieRegelConverter = restZaakHistorieRegelConverter,
        zgwApiService = zgwApiService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("zaak input data is provided") {
        val group = createGroup()
        val formulierData = mapOf(Pair("dummyKey", "dummyValue"))
        val objectRegistratieObject = createORObject()
        val productaanvraagDimpact = createProductaanvraagDimpact()
        val restZaak = createRestZaak()
        val zaakType = createZaakType(omschrijving = ZAAK_TYPE_1_OMSCHRIJVING)
        val zaakTypeUUID = URIUtil.parseUUIDFromResourceURI(zaakType.url)
        val restZaakAanmaakGegevens = createRESTZaakAanmaakGegevens(
            zaak = createRestZaak(
                restZaakType = RestZaaktype(
                    uuid = zaakTypeUUID
                ),
                restGroup = createRestGroup(
                    id = group.id
                ),

            ),
            zaakTypeUUID = zaakTypeUUID
        )
        val rolMedewerker = createRolMedewerker()
        val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid()
        val rolTypeInitiator = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
        val user = createLoggedInUser()
        val zaakAfhandelParameters = createZaakafhandelParameters()
        val zaakObjectPand = createZaakobjectPand()
        val zaakObjectOpenbareRuimte = createZaakobjectOpenbareRuimte()
        val zaak = createZaak(zaakType.url)

        every { cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null) } just runs
        every { identityService.readGroup(restZaakAanmaakGegevens.zaak.groep!!.id) } returns group
        every { identityService.readUser(restZaakAanmaakGegevens.zaak.behandelaar!!.id) } returns user
        every {
            inboxProductaanvraagService.delete(restZaakAanmaakGegevens.inboxProductaanvraag?.id)
        } just runs
        every { loggedInUserInstance.get() } returns createLoggedInUser()
        every {
            objectsClientService
                .readObject(restZaakAanmaakGegevens.inboxProductaanvraag?.productaanvraagObjectUUID)
        } returns objectRegistratieObject
        every { productaanvraagService.getAanvraaggegevens(objectRegistratieObject) } returns formulierData
        every {
            productaanvraagService.getProductaanvraag(objectRegistratieObject)
        } returns productaanvraagDimpact
        every { productaanvraagService.pairAanvraagPDFWithZaak(productaanvraagDimpact, zaak.url) } just runs
        every {
            productaanvraagService.pairBijlagenWithZaak(productaanvraagDimpact.bijlagen, zaak.url)
        } just runs
        every {
            productaanvraagService.pairProductaanvraagWithZaak(
                objectRegistratieObject,
                zaak.url
            )
        } just runs
        every {
            restBAGConverter.convertToZaakobject(restZaakAanmaakGegevens.bagObjecten?.get(0), zaak)
        } returns zaakObjectPand
        every {
            restBAGConverter.convertToZaakobject(
                restZaakAanmaakGegevens.bagObjecten?.get(1),
                zaak
            )
        } returns zaakObjectOpenbareRuimte
        every { restZaakConverter.toRestZaak(zaak) } returns restZaak
        every { restZaakConverter.toZaak(restZaakAanmaakGegevens.zaak, zaakType) } returns zaak
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakAfhandelParameters
        every { zaakVariabelenService.setZaakdata(zaak.uuid, formulierData) } just runs
        every { zgwApiService.createZaak(zaak) } returns zaak
        every { zrcClientService.updateRol(zaak, any(), any()) } just runs
        every { zrcClientService.createZaakobject(zaakObjectPand) } returns zaakObjectPand
        every { zrcClientService.createZaakobject(zaakObjectOpenbareRuimte) } returns zaakObjectOpenbareRuimte
        every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType
        every {
            ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
        } returns rolTypeInitiator
        every { zaakService.bepaalRolGroep(group, zaak) } returns rolOrganisatorischeEenheid
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker
        every {
            zaakService.addBetrokkenNatuurlijkPersoon(
                roltype = rolTypeInitiator,
                bsn = restZaak.initiatorIdentificatie!!,
                zaak = zaak,
                toelichting = "Toegekend door de medewerker tijdens het behandelen van de zaak"
            )
        } just runs

        When("createZaak is called for a zaaktype for which the logged in user has permissions") {
            every { policyService.readOverigeRechten() } returns createOverigeRechtenAllDeny(startenZaak = true)
            every {
                policyService.readZaakRechten(zaak)
            } returns createZaakRechtenAllDeny(toevoegenInitiatorPersoon = true)

            val restZaakReturned = zaakRestService.createZaak(restZaakAanmaakGegevens)

            Then("a zaak is created using the ZGW API and a zaak is started in the ZAC CMMN service") {
                restZaakReturned shouldBe restZaak
                val zaakCreatedSlot = slot<Zaak>()
                val rolGroupSlotOrganisatorischeEenheidSlot = slot<RolOrganisatorischeEenheid>()
                verify(exactly = 1) {
                    ztcClientService.readZaaktype(restZaakAanmaakGegevens.zaak.zaaktype.uuid)
                    zgwApiService.createZaak(capture(zaakCreatedSlot))
                    zrcClientService.updateRol(
                        zaak,
                        capture(rolGroupSlotOrganisatorischeEenheidSlot),
                        "Aanmaken zaak"
                    )
                    cmmnService.startCase(zaak, zaakType, zaakAfhandelParameters, null)
                    zrcClientService.createZaakobject(zaakObjectPand)
                    zrcClientService.createZaakobject(zaakObjectOpenbareRuimte)
                }
                zaakCreatedSlot.captured shouldBe zaak
                with(rolGroupSlotOrganisatorischeEenheidSlot.captured) {
                    this.zaak shouldBe rolOrganisatorischeEenheid.zaak
                    betrokkeneType shouldBe BetrokkeneType.ORGANISATORISCHE_EENHEID
                }
            }
        }
    }

    Given("a zaak exists, no one is assigned and zaak toekennen gegevens are provided") {
        val restZaakToekennenGegevens = createRESTZaakToekennenGegevens()
        val zaak = createZaak()
        val user = createLoggedInUser()
        val rolSlot = slot<Rol<*>>()
        val restZaak = createRestZaak()
        val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
        val rolMedewerker = createRolMedewerker()

        every { zrcClientService.readZaak(restZaakToekennenGegevens.zaakUUID) } returns zaak
        every { zrcClientService.updateRol(zaak, capture(rolSlot), restZaakToekennenGegevens.reason) } just runs
        every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns Optional.empty()
        every { identityService.readUser(restZaakToekennenGegevens.assigneeUserName!!) } returns user
        every { zgwApiService.findGroepForZaak(zaak) } returns Optional.empty()
        every { restZaakConverter.toRestZaak(zaak) } returns restZaak
        every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
        every { zaakService.bepaalRolMedewerker(user, zaak) } returns rolMedewerker

        When("toekennen is called from user with access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(toekennen = true)

            val returnedRestZaak = zaakRestService.assign(restZaakToekennenGegevens)

            Then("the zaak is updated, and the zaken search index is updated") {
                returnedRestZaak shouldBe restZaak
                verify(exactly = 1) {
                    zrcClientService.updateRol(zaak, any(), restZaakToekennenGegevens.reason)
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                }
                with(rolSlot.captured) {
                    betrokkeneType shouldBe BetrokkeneType.MEDEWERKER
                    with(betrokkeneIdentificatie as Medewerker) {
                        identificatie shouldBe rolMedewerker.betrokkeneIdentificatie.identificatie
                    }
                    this.zaak shouldBe rolMedewerker.zaak
                    omschrijving shouldBe rolType.omschrijving
                }
            }
        }
    }

    Given("REST zaken verdeel gegevens with a group and a user") {
        val zaakUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
        val group = createGroup()
        val user = createUser()
        val restZakenVerdeelGegevens = createRESTZakenVerdeelGegevens(
            uuids = zaakUUIDs,
            groepId = group.id,
            behandelaarGebruikersnaam = user.id,
            reden = "dummyReason"
        )
        every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
        every { zaakService.assignZaken(any(), any(), any(), any(), any()) } just runs
        every { identityService.readGroup(group.id) } returns group
        every { identityService.readUser(restZakenVerdeelGegevens.behandelaarGebruikersnaam!!) } returns user

        When("the assign zaken from a list function is called") {
            runTest {
                zaakRestService.assignFromList(restZakenVerdeelGegevens)
                testScheduler.advanceUntilIdle()
            }

            Then("the zaken are assigned to the group and user") {
                verify(exactly = 1) {
                    zaakService.assignZaken(
                        zaakUUIDs,
                        group,
                        user,
                        restZakenVerdeelGegevens.reden,
                        restZakenVerdeelGegevens.screenEventResourceId
                    )
                }
            }
        }
    }

    Given("Two open zaken") {
        val zaak = createZaak()
        val teKoppelenZaak = createZaak()
        val restZakenVerdeelGegevens = createRESTZaakKoppelGegevens(
            zaakUuid = zaak.uuid,
            teKoppelenZaakUuid = teKoppelenZaak.uuid,
            relatieType = RelatieType.BIJDRAGE,
            reverseRelatieType = RelatieType.BIJDRAGE
        )

        every { zrcClientService.readZaak(restZakenVerdeelGegevens.zaakUuid) } returns zaak
        every { zrcClientService.readZaak(restZakenVerdeelGegevens.teKoppelenZaakUuid) } returns teKoppelenZaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { policyService.readZaakRechten(teKoppelenZaak) } returns createZaakRechten()
        every { zrcClientService.patchZaak(any(), any()) } returns zaak

        When("the zaken are linked ") {
            zaakRestService.koppelZaak(restZakenVerdeelGegevens)

            Then("the two zaken are succesfully linked") {
                verify(exactly = 2) {
                    zrcClientService.patchZaak(any(), any())
                }
            }
        }
    }

    Given("An open zaak and a closed zaak") {
        val zaak = createZaak()
        val teKoppelenZaak = createZaak(archiefnominatie = Archiefnominatie.BLIJVEND_BEWAREN)
        val restZakenVerdeelGegevens = createRESTZaakKoppelGegevens(
            zaakUuid = zaak.uuid,
            teKoppelenZaakUuid = teKoppelenZaak.uuid,
            relatieType = RelatieType.BIJDRAGE,
            reverseRelatieType = RelatieType.BIJDRAGE
        )

        every { zrcClientService.readZaak(restZakenVerdeelGegevens.zaakUuid) } returns zaak
        every { zrcClientService.readZaak(restZakenVerdeelGegevens.teKoppelenZaakUuid) } returns teKoppelenZaak

        When("the zaken are linked") {
            val exception = shouldThrow<PolicyException> {
                zaakRestService.koppelZaak(restZakenVerdeelGegevens)
            }

            Then("a policy exception should be thrown") {
                exception.message shouldBe null
            }
        }
    }

    fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())

    Given("a zaak with tasks exists and zaak and tasks have final date set") {
        val changeDescription = "change description"
        val zaak = createZaak()
        val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
        val restZaak = createRestZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate)
        val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaak, changeDescription)
        val patchedZaak = createZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate)
        val task = mockk<Task>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { restZaakConverter.convertToPatch(restZaak) } returns patchedZaak
        every { zrcClientService.patchZaak(zaak.uuid, patchedZaak, changeDescription) } returns patchedZaak
        every { flowableTaskService.listOpenTasksForZaak(zaak.uuid) } returns listOf(task, task, task)
        every { task.dueDate } returns zaak.uiterlijkeEinddatumAfdoening.toDate()
        every { task.name } returnsMany listOf("dummyTask", AANVULLENDE_INFORMATIE_TASK_NAME, "another task")
        every { task.dueDate = newZaakFinalDate.toDate() } just runs
        every { flowableTaskService.updateTask(task) } returns task
        every { task.id } returns "id"
        every { eventingService.send(any<ScreenEvent>()) } just runs
        every { restZaakConverter.toRestZaak(patchedZaak) } returns restZaak

        When("zaak final date is set to a later date") {
            val updatedRestZaak = zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)

            Then("zaak is updated with the new data") {
                with(updatedRestZaak) {
                    uiterlijkeEinddatumAfdoening shouldBe newZaakFinalDate
                }
            }

            And("tasks final date are shifted accordingly") {
                verify(exactly = 2) {
                    task.dueDate = newZaakFinalDate.toDate()
                }
            }

            And("screen event signals are sent") {
                verify(exactly = 3) {
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    Given("no verlengenDoorlooptijd policy") {
        val zaak = createZaak()
        val newZaakFinalDate = zaak.uiterlijkeEinddatumAfdoening.minusDays(10)
        val restZaak = createRestZaak(uiterlijkeEinddatumAfdoening = newZaakFinalDate)
        val restZaakEditMetRedenGegevens = RESTZaakEditMetRedenGegevens(restZaak, "change description")

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten(verlengenDoorlooptijd = false)

        When("zaak update is requested") {
            val exception = shouldThrow<PolicyException> {
                zaakRestService.updateZaak(zaak.uuid, restZaakEditMetRedenGegevens)
            }

            Then("it fails") {
                exception.message shouldBe null
            }
        }
    }
})
