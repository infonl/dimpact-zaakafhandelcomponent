/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.zaak.ZaakService
import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.impl.el.JuelExpression

class UpdateZaakAssignmentDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zaakService = mockk<ZaakService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()
    val zaak = createZaak()
    val zaaktype = createZaakType(uri = zaak.zaaktype)
    val groupId = "fakeGroupId"
    val userId = "fakeUserId"
    val reason = "fakeReason"

    afterEach {
        checkUnnecessaryStub()
    }

    Given("zaak and assignment details as expressions") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaaktype, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)

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
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaaktype, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)

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

    Given("Policy denies assigning zaak") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaaktype, loggedInUser) } returns createZaakRechtenAllDeny()

        val updateZaakAssignmentDelegate = UpdateZaakAssignmentDelegate().apply {
            groepId = mockk()
        }

        When("execute is called") {
            val policyException = shouldThrow<PolicyException> {
                updateZaakAssignmentDelegate.execute(delegateExecution)
            }

            Then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the zaak is not assigned") {
                verify(exactly = 0) {
                    zaakService.assignZaak(any(), any(), any(), any())
                }
            }
        }
    }
})
