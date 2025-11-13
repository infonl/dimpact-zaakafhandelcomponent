/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.flowable.common.engine.impl.el.JuelExpression
import org.flowable.engine.delegate.DelegateExecution
import java.time.ZonedDateTime

class ResumeZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val zaak = createZaak()
    val reason = "test"
    val date = ZonedDateTime.now()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Suspended zaak") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.suspensionZaakHelper } returns suspensionZaakHelper

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "activity"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        val hervattenRedenExpression = mockk<JuelExpression>()
        every { hervattenRedenExpression.getValue(delegateExecution) } returns reason

        every { suspensionZaakHelper.resumeZaak(zaak, reason, any()) } returns zaak

        When("zaak is resumed from service task with no 'hervattenDatum'") {
            val suspendZaakDelegate = ResumeZaakDelegate().apply {
                hervattenReden = hervattenRedenExpression
            }
            suspendZaakDelegate.execute(delegateExecution)

            Then("expressions resolution is attempted") {
                verify(exactly = 1) {
                    hervattenRedenExpression.getValue(delegateExecution)
                }
            }

            And("the zaak resume is called with correct parameters") {
                verify(exactly = 1) {
                    suspensionZaakHelper.resumeZaak(zaak, reason, any())
                }
            }
        }

        When("zaak is resumed from service task with 'hervattenDatum'") {
            clearMocks(hervattenRedenExpression, answers = false)

            val hervattenDatumExpression = mockk<JuelExpression>()
            every { hervattenDatumExpression.getValue(delegateExecution) } returns date.toString()

            val suspendZaakDelegate = ResumeZaakDelegate().apply {
                hervattenReden = hervattenRedenExpression
                hervattenDatum = hervattenDatumExpression
            }
            suspendZaakDelegate.execute(delegateExecution)

            Then("expressions resolution is attempted") {
                verify(exactly = 1) {
                    hervattenRedenExpression.getValue(delegateExecution)
                    hervattenDatumExpression.getValue(delegateExecution)
                }
            }

            And("the zaak resume is called with correct parameters") {
                verify(exactly = 1) {
                    suspensionZaakHelper.resumeZaak(zaak, reason, date)
                }
            }
        }
    }
})
