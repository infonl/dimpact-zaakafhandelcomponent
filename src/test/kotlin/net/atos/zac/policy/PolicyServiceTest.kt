package net.atos.zac.policy

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.opa.model.RuleQuery
import net.atos.client.opa.model.RuleResponse
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createVerlenging
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.policy.input.UserInput
import net.atos.zac.policy.input.ZaakInput
import net.atos.zac.policy.output.createWerklijstRechten
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.createZaakZoekObject
import java.net.URI
import java.util.UUID

class PolicyServiceTest : BehaviorSpec() {
    private val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    private val evaluationClient = mockk<OPAEvaluationClient>()
    private val ztcClientService = mockk<ZTCClientService>()
    private val zrcClientService = mockk<ZRCClientService>()

    private val loggedInUser = createLoggedInUser()

    @InjectMockKs
    lateinit var policyService: PolicyService

    override suspend fun beforeContainer(testCase: TestCase) {
        super.beforeContainer(testCase)

        // Only run before Given
        if (testCase.parent != null) return

        MockKAnnotations.init(this)
        clearAllMocks()

        every { loggedInUserInstance.get() } returns loggedInUser
    }

    init {
        Given("zaak with no status") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val expectedZaakRechten = createZaakRechten()

            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { evaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        evaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.zaak) {
                        this.open shouldBe true
                        this.zaaktype shouldBe zaakType.omschrijving
                        this.opgeschort shouldBe zaak.isOpgeschort
                        this.verlengd shouldBe zaak.isVerlengd
                        this.besloten shouldBe false
                        this.intake shouldBe false
                        this.heropend shouldBe false
                    }
                }
            }
        }

        Given("zaak with status") {
            val zaak = createZaak(
                status = URI("https://example.com/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType()
            val expectedZaakRechten = createZaakRechten()

            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { evaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        evaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.zaak) {
                        this.open shouldBe true
                        this.zaaktype shouldBe zaakType.omschrijving
                        this.opgeschort shouldBe zaak.isOpgeschort
                        this.verlengd shouldBe zaak.isVerlengd
                        this.besloten shouldBe false
                        this.intake shouldBe false
                        this.heropend shouldBe false
                    }
                }
            }
        }

        Given("locked zaak with no status that is now in intake") {
            val zaak = createZaak(
                verlenging = createVerlenging()
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_INTAKE)
            val expectedZaakRechten = createZaakRechten()

            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { evaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        evaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.zaak) {
                        open shouldBe true
                        zaaktype shouldBe zaakType.omschrijving
                        opgeschort shouldBe zaak.isOpgeschort
                        verlengd shouldBe zaak.isVerlengd
                        besloten shouldBe false
                        intake shouldBe false
                        heropend shouldBe false
                    }
                }
            }
        }

        Given("zaak with status that was reopened") {
            val zaak = createZaak(
                verlenging = createVerlenging(),
                status = URI("https://example.com/${UUID.randomUUID()}")
            )
            val zaakType = createZaakType()
            val zaakStatus = createZaakStatus()
            val statusType = createStatusType(omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND)
            val expectedZaakRechten = createZaakRechten()

            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()

            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
            every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
            every { evaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaak)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        evaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.zaak) {
                        open shouldBe true
                        zaaktype shouldBe zaakType.omschrijving
                        opgeschort shouldBe zaak.isOpgeschort
                        verlengd shouldBe zaak.isVerlengd
                        besloten shouldBe false
                        intake shouldBe false
                        heropend shouldBe true
                    }
                }
            }
        }

        Given("ZaakZoekObject") {
            val zaakZoekObject = createZaakZoekObject().apply {
                this.setIndicatie(ZaakIndicatie.OPSCHORTING, true)
                this.setIndicatie(ZaakIndicatie.VERLENGD, true)
                this.setIndicatie(ZaakIndicatie.INTAKE, true)
                this.setIndicatie(ZaakIndicatie.BESLOTEN, true)
                this.setIndicatie(ZaakIndicatie.HEROPEND, true)
            }
            val expectedZaakRechten = createZaakRechten()

            val ruleQuerySlot = slot<RuleQuery<ZaakInput>>()
            every { evaluationClient.readZaakRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedZaakRechten)

            When("policy rights are requested") {
                val zaakRechten = policyService.readZaakRechten(zaakZoekObject)

                Then("correct ZaakData is sent to OPA") {
                    zaakRechten shouldBe expectedZaakRechten
                    verify(exactly = 1) {
                        evaluationClient.readZaakRechten(any<RuleQuery<ZaakInput>>())
                    }
                    with(ruleQuerySlot.captured.input.zaak) {
                        open shouldBe true
                        zaaktype shouldBe zaakZoekObject.zaaktypeOmschrijving
                        opgeschort shouldBe true
                        verlengd shouldBe true
                        besloten shouldBe true
                        intake shouldBe true
                        heropend shouldBe true
                    }
                }
            }
        }

        Given("an evaluation client") {
            val expectedWerklijstRechten = createWerklijstRechten()
            val ruleQuerySlot = slot<RuleQuery<UserInput>>()
            every { evaluationClient.readWerklijstRechten(capture(ruleQuerySlot)) } returns RuleResponse(expectedWerklijstRechten)

            When("the werklijst rechten are requested") {

                val werklijstRechten = policyService.readWerklijstRechten()

                Then("the evaluation client is called with the correct arguments") {
                    werklijstRechten shouldBe expectedWerklijstRechten
                    verify(exactly = 1) {
                        evaluationClient.readWerklijstRechten(any<RuleQuery<UserInput>>())
                    }
                    with(ruleQuerySlot.captured.input.user) {
                        this.id shouldBe loggedInUser.id
                        this.rollen shouldBe loggedInUser.roles
                        this.zaaktypen shouldBe loggedInUser.geautoriseerdeZaaktypen
                    }
                }
            }
        }
    }
}
