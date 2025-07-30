/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.impl.el.JuelExpression

class UpdateZaakJavaDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zaak = createZaak()
    val zaakStatusName = "fakeStatus"
    val zaakStatus = createZaakStatus()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("JUEL expression in a BPMN service task") {
        mockkStatic(FlowableHelper::class)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zgwApiService } returns zgwApiService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns "fakeUUID"

        every { zrcClientService.readZaakByID("fakeUUID") } returns zaak

        val juelExpression = mockk<JuelExpression>()
        every { juelExpression.getValue(delegateExecution) } returns zaakStatusName

        every {
            zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
        } returns zaakStatus

        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = juelExpression
        }

        When("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            Then("the expression was resolved") {
                verify(exactly = 1) {
                    juelExpression.getValue(delegateExecution)
                }
            }

            And("the status was updated in the ZGW API") {
                verify(exactly = 1) {
                    zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
                }
            }
        }
    }

    Given("Fixed value in a BPMN service task") {
        mockkStatic(FlowableHelper::class)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zgwApiService } returns zgwApiService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns "fakeUUID"

        every { zrcClientService.readZaakByID("fakeUUID") } returns zaak

        val fixedValueExpression = mockk<FixedValue>()
        every { fixedValueExpression.getValue(delegateExecution) } returns zaakStatusName

        every {
            zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
        } returns zaakStatus

        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = fixedValueExpression
        }

        When("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            Then("the value is obtained") {
                verify(exactly = 1) {
                    fixedValueExpression.getValue(delegateExecution)
                }
            }

            And("the status was updated in the ZGW API") {
                verify(exactly = 1) {
                    zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
                }
            }
        }
    }
})
