/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createOpschorting
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.flowable.common.engine.impl.el.JuelExpression
import org.flowable.engine.delegate.DelegateExecution
import java.math.BigDecimal

class SuspendZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()
    val zaak = createZaak()
    val opschorting = createOpschorting()
    zaak.opschorting = opschorting

    val numberOfDays = 10L
    val reason = "test"

    afterEach {
        checkUnnecessaryStub()
    }

    Given("Service task with expressions for aantalDagen and opschortingReden") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.suspensionZaakHelper } returns suspensionZaakHelper
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "activity"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(opschorten = true)

        val opschortingRedenExpression = mockk<JuelExpression>()
        every { opschortingRedenExpression.getValue(delegateExecution) } returns reason

        every { suspensionZaakHelper.suspendZaak(zaak, numberOfDays, reason) } returns zaak

        When("string expressions are used") {
            val aantalDagenExpression = mockk<JuelExpression>()
            every { aantalDagenExpression.getValue(delegateExecution) } returns "$numberOfDays"

            val suspendZaakDelegate = SuspendZaakDelegate().apply {
                aantalDagen = aantalDagenExpression
                opschortingReden = opschortingRedenExpression
            }

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

        When("BigDecimal expression is used for aantalDagen") {
            clearMocks(opschortingRedenExpression, suspensionZaakHelper, answers = false)

            val aantalDagenExpression = mockk<JuelExpression>()
            every { aantalDagenExpression.getValue(delegateExecution) } returns BigDecimal(numberOfDays)

            val suspendZaakDelegate = SuspendZaakDelegate().apply {
                aantalDagen = aantalDagenExpression
                opschortingReden = opschortingRedenExpression
            }

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

    Given("Policy denies suspending zaak") {
        val zaak = createZaak()
        zaak.opschorting = createOpschorting()

        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

        val suspendZaakDelegate = SuspendZaakDelegate().apply {
            aantalDagen = mockk()
            opschortingReden = mockk()
        }

        When("suspend zaak is called") {
            val policyException = shouldThrow<PolicyException> {
                suspendZaakDelegate.execute(delegateExecution)
            }

            Then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the zaak is not suspended") {
                verify(exactly = 0) {
                    suspensionZaakHelper.suspendZaak(any(), any(), any())
                }
            }
        }
    }

    Given("Zaak is already suspended with an existing suspension reason") {
        val zaak = createZaak()
        zaak.opschorting = createOpschorting(reden = "fakeExistingSuspensionReason")

        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(opschorten = true)

        val suspendZaakDelegate = SuspendZaakDelegate().apply {
            aantalDagen = mockk()
            opschortingReden = mockk()
        }

        When("suspend zaak is called") {
            val policyException = shouldThrow<PolicyException> {
                suspendZaakDelegate.execute(delegateExecution)
            }

            Then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the zaak is not suspended") {
                verify(exactly = 0) {
                    suspensionZaakHelper.suspendZaak(any(), any(), any())
                }
            }
        }
    }
})
