/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.planitems

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
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter
import net.atos.zac.app.mail.model.createRESTMailGegevens
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.model.createResultaat
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Resultaat
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.BrondatumArchiefprocedure
import nl.info.zac.admin.model.createHumanTaskParameters
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.app.planitems.converter.RESTPlanItemConverter
import nl.info.zac.app.planitems.model.UserEventListenerActie
import nl.info.zac.app.planitems.model.createRESTHumanTaskData
import nl.info.zac.app.planitems.model.createRESTUserEventListenerData
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.createMailGegevens
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.zaak.ZaakService
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.net.URI
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class PlanItemsRestServiceTest : BehaviorSpec({
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val cmmnService = mockk<CMMNService>()
    val zrcClientService = mockk<ZrcClientService>()
    val brcClientService = mockk<BrcClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val planItemConverter = mockk<RESTPlanItemConverter>()
    val zgwApiService = mockk<ZGWApiService>()
    val indexingService = mockk<IndexingService>()
    val mailService = mockk<MailService>()
    val configuratieService = mockk<ConfiguratieService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val policyService = mockk<PolicyService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val restMailGegevensConverter = mockk<RESTMailGegevensConverter>()
    val zaakService = mockk<ZaakService>()

    val planItemsRESTService = PlanItemsRestService(
        zaakVariabelenService,
        cmmnService,
        zrcClientService,
        brcClientService,
        zaakafhandelParameterService,
        planItemConverter,
        zgwApiService,
        indexingService,
        mailService,
        configuratieService,
        mailTemplateService,
        policyService,
        suspensionZaakHelper,
        restMailGegevensConverter,
        zaakService
    )

    val planItemInstanceId = "fakePlanItemInstanceId"
    val planItemInstance = mockk<PlanItemInstance>()
    val zaakTypeUUID = UUID.randomUUID()
    val zaakafhandelParameters = createZaakafhandelParameters(
        zaaktypeUUID = zaakTypeUUID
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Valid REST human task data without a fatal date") {
        val restHumanTaskData = createRESTHumanTaskData(
            planItemInstanceId = planItemInstanceId,
            taakdata = mapOf("fakeKey" to "fakeValue"),
            fataledatum = null
        )
        val taskDataSlot = slot<Map<String, String>>()
        val zaak = createZaak(
            zaakTypeURI = URI("https://example.com/$zaakTypeUUID"),
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
        )
        every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
        every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
        every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
        every { indexingService.addOrUpdateZaak(zaak.uuid, false) } just runs
        every {
            cmmnService.startHumanTaskPlanItem(
                planItemInstanceId,
                restHumanTaskData.groep.id,
                null,
                any(),
                restHumanTaskData.toelichting,
                capture(taskDataSlot),
                zaak.uuid
            )
        } just runs

        When("A human task plan item is started from user that has access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(startenTaak = true)

            planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

            Then("A CMMN human task plan item is started and the zaak is re-indexed") {
                verify(exactly = 1) {
                    cmmnService.startHumanTaskPlanItem(any(), any(), any(), any(), any(), any(), any())
                    indexingService.addOrUpdateZaak(any(), any())
                }
            }
            with(taskDataSlot.captured) {
                get("fakeKey") shouldBe "fakeValue"
            }
        }

        When("the enkelvoudig informatieobject is updated by a user that has no access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> { planItemsRESTService.doHumanTaskplanItem(restHumanTaskData) }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }

    Given("Valid REST human task data with a fatal date and with zaak opschorten set to true") {
        val opgeschorteZaak = createZaak()
        val restHumanTaskData = createRESTHumanTaskData(
            planItemInstanceId = planItemInstanceId,
            taakdata = mapOf(
                "fakeKey" to "fakeValue",
                "zaakOpschorten" to "true"
            ),
            fataledatum = LocalDate.now().plusDays(1)
        )
        val zaak = createZaak(
            zaakTypeURI = URI("https://example.com/$zaakTypeUUID"),
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
        )
        every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
        every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(startenTaak = true)
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
        every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
        every { indexingService.addOrUpdateZaak(zaak.uuid, false) } just runs
        every {
            cmmnService.startHumanTaskPlanItem(
                planItemInstanceId,
                restHumanTaskData.groep.id,
                null,
                DateTimeConverterUtil.convertToDate(restHumanTaskData.fataledatum),
                restHumanTaskData.toelichting,
                any(),
                zaak.uuid
            )
        } just runs
        every {
            suspensionZaakHelper.suspendZaak(zaak, 1, "Aanvullende informatie opgevraagd")
        } returns opgeschorteZaak

        When("A human task plan item is started from user with access") {
            planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

            Then("A CMMN human task plan item is started and the zaak is opgeschort and re-indexed") {
                verify(exactly = 1) {
                    cmmnService.startHumanTaskPlanItem(any(), any(), any(), any(), any(), any(), any())
                    indexingService.addOrUpdateZaak(any(), any())
                    suspensionZaakHelper.suspendZaak(any(), any(), any())
                }
            }
        }
    }

    Given("REST human task data with a user-set fatal date that comes after the fatal date of the related zaak") {
        val restHumanTaskData = createRESTHumanTaskData(
            planItemInstanceId = planItemInstanceId,
            taakdata = mapOf(
                "fakeKey" to "fakeValue"
            ),
            fataledatum = LocalDate.now().plusDays(3)
        )
        val zaak = createZaak(
            zaakTypeURI = URI("http://example.com/$zaakTypeUUID"),
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
        )
        every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
        every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(startenTaak = true)
        every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
        every { planItemInstance.planItemDefinitionId } returns planItemInstanceId

        When("A human task plan item is started") {
            shouldThrow<InputValidationFailedException> { planItemsRESTService.doHumanTaskplanItem(restHumanTaskData) }
            Then("An exception is thrown and the human task item is not started and the zaak is not indexed") {
                verify(exactly = 0) {
                    cmmnService.startHumanTaskPlanItem(any(), any(), any(), any(), any(), any(), any())
                    indexingService.addOrUpdateZaak(any(), any())
                }
            }
        }
    }

    Given("REST human task data with a calculated fatal date after the fatal date of the related zaak") {
        val restHumanTaskData = createRESTHumanTaskData(
            planItemInstanceId = planItemInstanceId,
            taakdata = mapOf(
                "fakeKey" to "fakeValue"
            )
        )
        val zaak = createZaak(
            zaakTypeURI = URI("http://example.com/$zaakTypeUUID")
        )
        val zaakafhandelParametersMock = mockk<ZaakafhandelParameters>()

        every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
        every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(startenTaak = true)
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakafhandelParametersMock
        every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
        every {
            zaakafhandelParametersMock.findHumanTaskParameter(planItemInstanceId)
        } returns Optional.of(
            createHumanTaskParameters().apply {
                doorlooptijd = 10
            }
        )
        every {
            cmmnService.startHumanTaskPlanItem(
                planItemInstanceId,
                restHumanTaskData.groep.id,
                null,
                DateTimeConverterUtil.convertToDate(zaak.uiterlijkeEinddatumAfdoening),
                restHumanTaskData.toelichting,
                any(),
                zaak.uuid
            )
        } just runs
        every { indexingService.addOrUpdateZaak(zaak.uuid, false) } just runs

        When("A human task plan item is started") {
            planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

            Then("The task is created with the zaak fatal date") {
                verify(exactly = 1) {
                    cmmnService.startHumanTaskPlanItem(any(), any(), any(), any(), any(), any(), any())
                    indexingService.addOrUpdateZaak(any(), any())
                }
            }
        }
    }

    Given("Additional info human task with a fatal date after the fatal date of the related zaak") {
        val numberOfDays = 3L
        val additionalInfoPlanItemInstanceId = FormulierDefinitie.AANVULLENDE_INFORMATIE.toString()
        val restHumanTaskData = createRESTHumanTaskData(
            planItemInstanceId = additionalInfoPlanItemInstanceId,
            taakdata = mapOf(
                "fakeKey" to "fakeValue"
            ),
            fataledatum = LocalDate.now().plusDays(numberOfDays)
        )
        val zaak = createZaak(
            zaakTypeURI = URI("http://example.com/$zaakTypeUUID"),
            uiterlijkeEinddatumAfdoening = LocalDate.now()
        )
        val extendedZaak = createZaak(
            zaakTypeURI = URI("http://example.com/$zaakTypeUUID"),
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(numberOfDays)
        )
        val zaakafhandelParametersMock = mockk<ZaakafhandelParameters>()

        every { cmmnService.readOpenPlanItem(additionalInfoPlanItemInstanceId) } returns planItemInstance
        every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(startenTaak = true)
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID)
        } returns zaakafhandelParametersMock
        every { planItemInstance.planItemDefinitionId } returns additionalInfoPlanItemInstanceId
        every {
            zaakafhandelParametersMock.findHumanTaskParameter(additionalInfoPlanItemInstanceId)
        } returns Optional.of(
            createHumanTaskParameters().apply {
                doorlooptijd = 10
            }
        )
        every {
            suspensionZaakHelper.extendZaakFatalDate(zaak, numberOfDays, "Aanvullende informatie opgevraagd")
        } returns extendedZaak
        every {
            cmmnService.startHumanTaskPlanItem(
                additionalInfoPlanItemInstanceId,
                restHumanTaskData.groep.id,
                null,
                DateTimeConverterUtil.convertToDate(restHumanTaskData.fataledatum),
                restHumanTaskData.toelichting,
                any(),
                zaak.uuid
            )
        } just runs
        every { indexingService.addOrUpdateZaak(zaak.uuid, false) } just runs

        When("A human task plan item is started") {
            planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

            Then("The task is created with its own fatal date") {
                verify(exactly = 1) {
                    cmmnService.startHumanTaskPlanItem(any(), any(), any(), any(), any(), any(), any())
                    indexingService.addOrUpdateZaak(any(), any())
                }
            }

            And("zaak extend fatal date was performed") {
                verify(exactly = 1) {
                    suspensionZaakHelper.extendZaakFatalDate(any(), any(), any())
                }
            }
        }
    }

    Given("Zaak exists") {
        val zaak = createZaak(
            resultaat = URI("https://example.com/resultaat/${UUID.randomUUID()}"),
        )

        val mailGegevens = createMailGegevens()
        val resultaat = createResultaat()
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
            startenTaak = true,
            versturenEmail = true
        )
        every { zaakService.checkZaakAfsluitbaar(zaak) } just runs
        every { brcClientService.listBesluiten(zaak) } returns listOf(Besluit())
        every { zrcClientService.readResultaat(zaak.resultaat) } returns resultaat
        every { zrcClientService.updateResultaat(any<Resultaat>()) } returns resultaat
        every { mailService.sendMail(mailGegevens, any<Bronnen>()) } returns mailGegevens.body

        When("A user event to settle the zaak and send a corresponding email is planned") {
            val restMailGegevens = createRESTMailGegevens()
            val restUserEventListenerData = createRESTUserEventListenerData(
                zaakUuid = zaak.uuid,
                actie = UserEventListenerActie.ZAAK_AFHANDELEN,
                restMailGegevens = restMailGegevens
            )
            every { restMailGegevensConverter.convert(restMailGegevens) } returns mailGegevens

            planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

            Then("the zaak is settled and the email is sent") {
                verify(exactly = 1) {
                    mailService.sendMail(mailGegevens, any<Bronnen>())
                }
            }
        }
    }

    Given("Zaak exists with resultaattype that has afleidingswijze EIGENSCHAP") {
        val zaak = createZaak(
            resultaat = null,
        )
        val resultaattypeUuid = UUID.randomUUID()
        val datumkenmerk = "testDatumkenmerk"
        val brondatumEigenschap = "20231201" // YYYYMMDD format

        val brondatumArchiefprocedure = BrondatumArchiefprocedure().apply {
            afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP
        }
        brondatumArchiefprocedure.datumkenmerk = datumkenmerk

        val restUserEventListenerData = createRESTUserEventListenerData(
            zaakUuid = zaak.uuid,
            actie = UserEventListenerActie.ZAAK_AFHANDELEN,
            restMailGegevens = null,
            resultaattypeUuid = resultaattypeUuid,
            brondatumEigenschap = brondatumEigenschap
        )

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(startenTaak = true)
        every { zaakService.checkZaakAfsluitbaar(zaak) } just runs
        every { brcClientService.listBesluiten(zaak) } returns emptyList()

        every { zgwApiService.createResultaatForZaak(zaak, restUserEventListenerData.resultaattypeUuid!!, null) } just runs
        every { zaakService.processSpecialBrondatumProcedure(zaak, resultaattypeUuid, any()) } just runs

        When("the user event listener plan item is processed") {
            planItemsRESTService.doUserEventListenerPlanItem(restUserEventListenerData)

            Then("the processing of special brondatum procedure is requested") {
                verify(exactly = 1) {
                    zaakService.processSpecialBrondatumProcedure(
                        zaak,
                        resultaattypeUuid,
                        match {
                            it.datumkenmerk == brondatumEigenschap
                        }
                    )
                }
            }
        }
    }
})
