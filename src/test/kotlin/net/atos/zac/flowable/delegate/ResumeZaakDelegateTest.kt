/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import io.kotest.core.spec.style.BehaviorSpec
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

class ResumeZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val zaak = createZaak()
    val reason = "test"

    Given("Suspended zaak") {
        mockkStatic(FlowableHelper::class)
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

        every { suspensionZaakHelper.resumeZaak(zaak, reason) } returns zaak

        val suspendZaakDelegate = ResumeZaakDelegate().apply {
            hervattenReden = hervattenRedenExpression
        }

        When("zaak is resumed from service task") {
            suspendZaakDelegate.execute(delegateExecution)

            Then("expression is resolved") {
                verify(exactly = 1) {
                    hervattenRedenExpression.getValue(delegateExecution)
                }
            }

            And("the zaak resume is called") {
                verify(exactly = 1) {
                    suspensionZaakHelper.resumeZaak(zaak, reason)
                }
            }
        }
    }
})
