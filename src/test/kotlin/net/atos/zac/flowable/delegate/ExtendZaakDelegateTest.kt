/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.delegate.exception.InvalidExtensionPeriodException
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.exception.ErrorCode
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.zaak.ZaakService
import org.flowable.common.engine.impl.el.JuelExpression
import org.flowable.engine.delegate.DelegateExecution
import java.time.LocalDate

class ExtendZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val eventingService = mockk<EventingService>()
    val zaakService = mockk<ZaakService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()

    afterEach {
        checkUnnecessaryStub()
    }

    Given("Process using ExtendZaakDelegate with a JUEL expressions") {
        val zaak = createZaak(einddatumGepland = LocalDate.now())
        val zaaktype = createZaakType(uri = zaak.zaaktype)
        val extendDays = 2

        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.suspensionZaakHelper } returns suspensionZaakHelper
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "activity"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaaktype, loggedInUser) } returns createZaakRechtenAllDeny(verlengen = true)

        val aantalDagenExpression = mockk<JuelExpression>()
        every { aantalDagenExpression.getValue(delegateExecution) } returns extendDays

        val verlengingRedenExpression = mockk<JuelExpression>()
        every { verlengingRedenExpression.getValue(delegateExecution) } returns "fakeReason"

        val takenVerlengenExpression = mockk<JuelExpression>()
        every { takenVerlengenExpression.getValue(delegateExecution) } returns true

        val dueDateSlot = slot<LocalDate>()
        val fatalDateSlot = slot<LocalDate>()
        every {
            suspensionZaakHelper.extendZaak(
                zaak,
                capture(dueDateSlot),
                capture(fatalDateSlot),
                any<String>(),
                extendDays
            )
        } returns zaak

        every { suspensionZaakHelper.extendTasks(zaak, extendDays) } returns listOf(createTestTask())

        every { flowableHelper.eventingService } returns eventingService
        every { eventingService.send(any<ScreenEvent>()) } just runs

        val extendZaakDelegate = ExtendZaakDelegate().apply {
            aantalDagen = aantalDagenExpression
            verlengingReden = verlengingRedenExpression
            takenVerlengen = takenVerlengenExpression
        }

        When("Extending the zaak") {
            extendZaakDelegate.execute(delegateExecution)

            Then("correct expressions resolution is attempted") {
                verify(exactly = 1) {
                    aantalDagenExpression.getValue(delegateExecution)
                    verlengingRedenExpression.getValue(delegateExecution)
                    takenVerlengenExpression.getValue(delegateExecution)
                }
            }

            And("the zaak and tasks extend is called with correct parameters") {
                verify(exactly = 1) {
                    suspensionZaakHelper.extendZaak(
                        zaak,
                        any<LocalDate>(),
                        any<LocalDate>(),
                        any<String>(),
                        extendDays
                    )
                    suspensionZaakHelper.extendTasks(zaak, extendDays)
                }
            }

            And("due date and fatal date are extended") {
                dueDateSlot.captured shouldBe zaak.einddatumGepland.plusDays(extendDays.toLong())
                fatalDateSlot.captured shouldBe zaak.uiterlijkeEinddatumAfdoening.plusDays(extendDays.toLong())
            }

            And("events are sent") {
                // 1 task event + 1 zaak event
                verify(exactly = 2) {
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    Given("Process using ExtendZaakDelegate with a extend days bigger than allowed extension term") {
        val zaak = createZaak(einddatumGepland = LocalDate.now())
        val zaaktype = createZaakType(uri = zaak.zaaktype, verlengingMogelijk = true, verlengingstermijn = "P10D")
        val extendDays = 11

        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "activity"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)

        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaaktype, loggedInUser) } returns createZaakRechtenAllDeny(verlengen = true)

        val aantalDagenExpression = mockk<JuelExpression>()
        every { aantalDagenExpression.getValue(delegateExecution) } returns extendDays

        val verlengingRedenExpression = mockk<JuelExpression>()
        val takenVerlengenExpression = mockk<JuelExpression>()

        val extendZaakDelegate = ExtendZaakDelegate().apply {
            aantalDagen = aantalDagenExpression
            verlengingReden = verlengingRedenExpression
            takenVerlengen = takenVerlengenExpression
        }

        When("Extending the zaak") {
            val exception = shouldThrow<InvalidExtensionPeriodException> {
                extendZaakDelegate.execute(delegateExecution)
            }

            Then("exception is thrown") {
                exception.errorCode shouldBe ErrorCode.ERROR_CODE_EXTENSION_PERIOD_INVALID
            }

            And("correct expressions resolution is attempted") {
                verify(exactly = 1) {
                    aantalDagenExpression.getValue(delegateExecution)
                }
                verify(exactly = 0) {
                    verlengingRedenExpression.getValue(delegateExecution)
                    takenVerlengenExpression.getValue(delegateExecution)
                }
            }

            And("no zaak or tasks are extended") {
                verify(exactly = 0) {
                    suspensionZaakHelper.extendZaak(
                        zaak,
                        any<LocalDate>(),
                        any<LocalDate>(),
                        any<String>(),
                        extendDays
                    )
                    suspensionZaakHelper.extendTasks(zaak, extendDays)
                }
            }

            And("no events are sent") {
                // 1 task event + 1 zaak event
                verify(exactly = 0) {
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    Given("Process using ExtendZaakDelegate when user is not authorized to extend zaak") {
        val zaak = createZaak(einddatumGepland = LocalDate.now())
        val zaaktype = createZaakType(uri = zaak.zaaktype)

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

        val extendZaakDelegate = ExtendZaakDelegate().apply {
            aantalDagen = mockk()
            verlengingReden = mockk()
        }

        When("Extending the zaak") {
            val policyException = shouldThrow<PolicyException> {
                extendZaakDelegate.execute(delegateExecution)
            }

            Then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the zaak is not extended") {
                verify(exactly = 0) {
                    suspensionZaakHelper.extendZaak(any(), any(), any(), any(), any())
                }
            }
        }
    }
})
