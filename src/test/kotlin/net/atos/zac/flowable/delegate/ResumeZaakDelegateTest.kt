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
import java.time.ZonedDateTime

class ResumeZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()
    val zaak = createZaak()
    val opschorting = createOpschorting(indicatie = true)
    zaak.opschorting = opschorting
    val reason = "test"
    val date = ZonedDateTime.now()

    afterEach {
        checkUnnecessaryStub()
    }

    given("Suspended zaak") {
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
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(hervatten = true)

        val hervattenRedenExpression = mockk<JuelExpression>()
        every { hervattenRedenExpression.getValue(delegateExecution) } returns reason

        every { suspensionZaakHelper.resumeZaak(zaak, reason, any()) } returns zaak

        `when`("zaak is resumed from service task with no 'hervattenDatum'") {
            val suspendZaakDelegate = ResumeZaakDelegate().apply {
                hervattenReden = hervattenRedenExpression
            }
            suspendZaakDelegate.execute(delegateExecution)

            then("expressions resolution is attempted") {
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

        `when`("zaak is resumed from service task with 'hervattenDatum'") {
            clearMocks(hervattenRedenExpression, answers = false)

            val hervattenDatumExpression = mockk<JuelExpression>()
            every { hervattenDatumExpression.getValue(delegateExecution) } returns date.toString()

            val suspendZaakDelegate = ResumeZaakDelegate().apply {
                hervattenReden = hervattenRedenExpression
                hervattenDatum = hervattenDatumExpression
            }
            suspendZaakDelegate.execute(delegateExecution)

            then("expressions resolution is attempted") {
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

    given("User is not authorized to resume zaak") {
        val zaak = createZaak()
        zaak.opschorting = createOpschorting(indicatie = true)

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

        val resumeZaakDelegate = ResumeZaakDelegate().apply {
            hervattenReden = mockk()
        }

        `when`("resume zaak is called") {
            val policyException = shouldThrow<PolicyException> {
                resumeZaakDelegate.execute(delegateExecution)
            }

            then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the zaak is not resumed") {
                verify(exactly = 0) {
                    suspensionZaakHelper.resumeZaak(any(), any(), any())
                }
            }
        }
    }

    given("Zaak is not suspended") {
        val zaak = createZaak()
        zaak.opschorting = createOpschorting(indicatie = false)

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
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(hervatten = true)

        val resumeZaakDelegate = ResumeZaakDelegate().apply {
            hervattenReden = mockk()
        }

        `when`("resume zaak is called") {
            val policyException = shouldThrow<PolicyException> {
                resumeZaakDelegate.execute(delegateExecution)
            }

            then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the zaak is not resumed") {
                verify(exactly = 0) {
                    suspensionZaakHelper.resumeZaak(any(), any(), any())
                }
            }
        }
    }
})
