package net.atos.zac.app.planitems

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.brc.BRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.zac.app.planitems.converter.RESTPlanItemConverter
import net.atos.zac.app.planitems.model.createRESTHumanTaskData
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.mail.MailService
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import net.atos.zac.zoeken.IndexeerService
import org.eclipse.microprofile.config.ConfigProvider
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@MockKExtension.CheckUnnecessaryStub
class PlanItemsRESTServiceTest : BehaviorSpec() {
    init {
        // add static mocking for config provider or else the MailService class cannot be mocked
        // since it references the config provider statically
        mockkStatic(ConfigProvider::class)
        every { ConfigProvider.getConfig().getValue("mailjet.api.key", String::class.java) } returns "dummyApiKey"
        every { ConfigProvider.getConfig().getValue("mailjet.api.secret.key", String::class.java) } returns "dummySecretKey"
    }

    val taakVariabelenService = mockk<TaakVariabelenService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val cmmnService = mockk<CMMNService>()
    val zrcClientService = mockk<ZRCClientService>()
    val brcClientService = mockk<BRCClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val planItemConverter = mockk<RESTPlanItemConverter>()
    val zgwApiService = mockk<ZGWApiService>()
    val indexeerService = mockk<IndexeerService>()
    val mailService = mockk<MailService>()
    val configuratieService = mockk<ConfiguratieService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val policyService = mockk<PolicyService>()
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()

    val planItemsRESTService = PlanItemsRESTService(
        taakVariabelenService,
        zaakVariabelenService,
        cmmnService,
        zrcClientService,
        brcClientService,
        zaakafhandelParameterService,
        planItemConverter,
        zgwApiService,
        indexeerService,
        mailService,
        configuratieService,
        mailTemplateService,
        policyService,
        opschortenZaakHelper
    )

    init {
        Given("Valid REST human task data without a fatal date") {
            val planItemInstanceId = "dummyPlanItemInstanceId"
            val restHumanTaskData = createRESTHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf("dummyKey" to "dummyValue"),
                fataledatum = null
            )
            val taskDataSlot = slot<Map<String, String>>()
            val planItemInstance = mockk<PlanItemInstance>()
            val zaakTypeUUID = UUID.randomUUID()
            val zaak = createZaak(
                zaaktypeURI = URI("http://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val zaakRechtenAllAllowed = createZaakRechten()
            val zaakafhandelParameters = createZaakafhandelParameters(
                zaakTypeUUID = zaakTypeUUID
            )
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak) } returns zaakRechtenAllAllowed
            every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every { indexeerService.addOrUpdateZaak(zaak.uuid, false) } just runs
            every {
                cmmnService.startHumanTaskPlanItem(
                    planItemInstanceId,
                    restHumanTaskData.groep.id,
                    null,
                    null,
                    restHumanTaskData.toelichting,
                    capture(taskDataSlot),
                    zaak.uuid
                )
            } just runs

            When("A human task plan item is started") {
                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                Then("A CMMN human task plan item is started and the zaak is re-indexed") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId,
                            restHumanTaskData.groep.id,
                            null,
                            any(),
                            restHumanTaskData.toelichting,
                            any(),
                            zaak.uuid
                        )
                        indexeerService.addOrUpdateZaak(zaak.uuid, false)
                    }
                }
                with(taskDataSlot.captured) {
                    get("dummyKey") shouldBe "dummyValue"
                }
            }
        }
        Given("Valid REST human task data with a fatal date and with zaak opschorten set to true") {
            val opgeschorteZaak = createZaak()
            val planItemInstanceId = "dummyPlanItemInstanceId"
            val restHumanTaskData = createRESTHumanTaskData(
                planItemInstanceId = planItemInstanceId,
                taakdata = mapOf(
                    "dummyKey" to "dummyValue"
                ),
                fataledatum = LocalDate.now().plusDays(1)
            )
            val planItemInstance = mockk<PlanItemInstance>()
            val zaakTypeUUID = UUID.randomUUID()
            val zaak = createZaak(
                zaaktypeURI = URI("http://example.com/$zaakTypeUUID"),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val zaakRechtenAllAllowed = createZaakRechten()
            val zaakafhandelParameters = createZaakafhandelParameters(
                zaakTypeUUID = zaakTypeUUID
            )
            every { cmmnService.readOpenPlanItem(planItemInstanceId) } returns planItemInstance
            every { zaakVariabelenService.readZaakUUID(planItemInstance) } returns zaak.uuid
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readZaakRechten(zaak) } returns zaakRechtenAllAllowed
            every { zaakafhandelParameterService.readZaakafhandelParameters(zaakTypeUUID) } returns zaakafhandelParameters
            every { planItemInstance.planItemDefinitionId } returns planItemInstanceId
            every { indexeerService.addOrUpdateZaak(zaak.uuid, false) } just runs
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
            every { taakVariabelenService.isZaakOpschorten(any()) } returns true
            every {
                opschortenZaakHelper.opschortenZaak(zaak, 1, "Aanvullende informatie opgevraagd")
            } returns opgeschorteZaak

            When("A human task plan item is started") {
                planItemsRESTService.doHumanTaskplanItem(restHumanTaskData)

                Then("A CMMN human task plan item is started and the zaak is opgeschort and re-indexed") {
                    verify(exactly = 1) {
                        cmmnService.startHumanTaskPlanItem(
                            planItemInstanceId,
                            restHumanTaskData.groep.id,
                            null,
                            any(),
                            restHumanTaskData.toelichting,
                            any(),
                            zaak.uuid
                        )
                        indexeerService.addOrUpdateZaak(zaak.uuid, false)
                        opschortenZaakHelper.opschortenZaak(zaak, 1, "Aanvullende informatie opgevraagd")
                    }
                }
            }
        }
    }
}
