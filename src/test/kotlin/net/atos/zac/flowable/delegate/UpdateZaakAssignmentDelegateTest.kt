/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.zaak.ZaakService
import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.impl.el.JuelExpression

class UpdateZaakAssignmentDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakService = mockk<ZaakService>()
    val zaak = createZaak()
    val groupId = "fakeGroupId"
    val userId = "fakeUserId"
    val reason = "fakeReason"

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("zaak and assignment details as expressions") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zaakService } returns zaakService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        val groupExpression = mockk<JuelExpression>()
        every { groupExpression.getValue(delegateExecution) } returns groupId
        val userExpression = mockk<JuelExpression>()
        every { userExpression.getValue(delegateExecution) } returns userId
        val reasonExpression = mockk<JuelExpression>()
        every { reasonExpression.getValue(delegateExecution) } returns reason

        every { zaakService.assignZaak(zaak, groupId, userId, reason) } just runs

        val updateZaakAssignmentDelegate = UpdateZaakAssignmentDelegate().apply {
            groepId = groupExpression
            behandelaarGebruikersnaam = userExpression
            reden = reasonExpression
        }

        When("execute is called") {
            updateZaakAssignmentDelegate.execute(delegateExecution)

            Then("zaak is updated with correct assignment") {
                verify(exactly = 1) {
                    zaakService.assignZaak(zaak, groupId, userId, reason)
                }
            }
        }
    }

    Given("zaak, group and reason as fixed values, missing user assignment") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zaakService } returns zaakService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        val groupExpression = mockk<FixedValue>()
        every { groupExpression.getValue(delegateExecution) } returns groupId
        val reasonExpression = mockk<FixedValue>()
        every { reasonExpression.getValue(delegateExecution) } returns reason

        every { zaakService.assignZaak(zaak, groupId, null, reason) } just runs

        val updateZaakAssignmentDelegate = UpdateZaakAssignmentDelegate().apply {
            groepId = groupExpression
            reden = reasonExpression
        }

        When("execute is called") {
            updateZaakAssignmentDelegate.execute(delegateExecution)

            Then("zaak is updated with correct assignment") {
                verify(exactly = 1) {
                    zaakService.assignZaak(zaak, groupId, null, reason)
                }
            }
        }
    }
})
