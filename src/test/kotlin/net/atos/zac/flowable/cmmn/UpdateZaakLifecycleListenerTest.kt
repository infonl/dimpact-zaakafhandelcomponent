/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance
import org.flowable.common.engine.api.delegate.Expression

class UpdateZaakLifecycleListenerTest : BehaviorSpec({
    mockkStatic(FlowableHelper::class)
    val flowableHelper = mockk<FlowableHelper>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val delegatePlanItemInstance = mockk<DelegatePlanItemInstance>()
    val expression = mockk<Expression>()
    val zrcClientService = mockk<ZrcClientService>()
    val zgwApiService = mockk<ZGWApiService>()

    val updateZaakLifecycleListener = UpdateZaakLifecycleListener()

    Given(
        """
        A plan item instance with a zaak UUID and 
        a status set in the updateZaakLifecycleListener
        """
    ) {
        val zaak = createZaak()
        val zaakStatus = createZaakStatus()
        every { expression.getValue(delegatePlanItemInstance) } returns "dummyStatus"
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakVariabelenService } returns zaakVariabelenService
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { zaakVariabelenService.readZaakUUID(any()) } returns zaak.uuid
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { zgwApiService.createStatusForZaak(zaak, any(), any()) } returns zaakStatus

        updateZaakLifecycleListener.setStatus(expression)

        When("the state is changed") {
            updateZaakLifecycleListener.stateChanged(
                delegatePlanItemInstance,
                "oldState",
                "newState"
            )

            Then("the state for the zaak is updated in the ZGW API") {
                verify(exactly = 1) {
                    zaakVariabelenService.readZaakUUID(any())
                    zrcClientService.readZaak(zaak.uuid)
                    zgwApiService.createStatusForZaak(zaak, any(), "Status gewijzigd")
                }
            }
        }
    }
})
