package net.atos.zac.flowable.delegate

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.flowable.common.engine.impl.el.JuelExpression
import org.flowable.engine.delegate.DelegateExecution

class SuspendZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val zaak = createZaak()
    val numberOfDays = 10L
    val reason = "test"

    Given("Service task with expressions for aantalDagen and opschortingReden") {
        mockkStatic(FlowableHelper::class)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.suspensionZaakHelper } returns suspensionZaakHelper

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "activity"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        val aantalDagenExpression = mockk<JuelExpression>()
        every { aantalDagenExpression.getValue(delegateExecution) } returns "$numberOfDays"
        val opschortingRedenExpression = mockk<JuelExpression>()
        every { opschortingRedenExpression.getValue(delegateExecution) } returns reason

        every { suspensionZaakHelper.suspendZaak(zaak, numberOfDays, reason) } returns zaak

        val suspendZaakDelegate = SuspendZaakDelegate().apply {
            aantalDagen = aantalDagenExpression
            opschortingReden = opschortingRedenExpression
        }

        When("execute is called") {
            suspendZaakDelegate.execute(delegateExecution)

            Then("expressions are resolved") {
                verify(exactly = 1) {
                    aantalDagenExpression.getValue(delegateExecution)
                    opschortingRedenExpression.getValue(delegateExecution)
                }
            }

            And("the zaak suspend is called") {
                verify(exactly = 1) {
                    suspensionZaakHelper.suspendZaak(zaak, numberOfDays, reason)
                }
            }
        }
    }
})
