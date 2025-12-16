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
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.flowable.common.engine.impl.el.JuelExpression
import org.flowable.engine.delegate.DelegateExecution
import java.time.LocalDate

class ExtendZaakDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val eventingService = mockk<EventingService>()
    val zaak = createZaak()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Process using ExtendZaakDelegate with a JUEL expressions") {
        mockkObject(FlowableHelper)
        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.suspensionZaakHelper } returns suspensionZaakHelper

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "activity"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        val aantalDagenExpression = mockk<JuelExpression>()
        every { aantalDagenExpression.getValue(delegateExecution) } returns 2

        val einddatumGeplandExpression = mockk<JuelExpression>()
        every { einddatumGeplandExpression.getValue(delegateExecution) } returns LocalDate.now().toString()

        val uiterlijkeEinddatumAfdoeningExpression = mockk<JuelExpression>()
        every {
            uiterlijkeEinddatumAfdoeningExpression.getValue(delegateExecution)
        } returns LocalDate.now().plusDays(1).toString()

        val verlengingRedenExpression = mockk<JuelExpression>()
        every { verlengingRedenExpression.getValue(delegateExecution) } returns "fakeReason"

        val takenVerlengenExpression = mockk<JuelExpression>()
        every { takenVerlengenExpression.getValue(delegateExecution) } returns true

        every {
            suspensionZaakHelper.extendZaak(
                zaak,
                any<LocalDate>(),
                any<LocalDate>(),
                any<String>(),
                2
            )
        } returns zaak

        every { suspensionZaakHelper.extendTasks(zaak, 2) } returns listOf(createTestTask())

        every { flowableHelper.eventingService } returns eventingService
        every { eventingService.send(any<ScreenEvent>()) } just runs

        val extendZaakDelegate = ExtendZaakDelegate().apply {
            aantalDagen = aantalDagenExpression
            einddatumGepland = einddatumGeplandExpression
            uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoeningExpression
            verlengingReden = verlengingRedenExpression
            takenVerlengen = takenVerlengenExpression
        }

        When("Extending the zaak") {
            extendZaakDelegate.execute(delegateExecution)

            Then("expressions resolution is attempted") {
                verify(exactly = 1) {
                    aantalDagenExpression.getValue(delegateExecution)
                    einddatumGeplandExpression.getValue(delegateExecution)
                    uiterlijkeEinddatumAfdoeningExpression.getValue(delegateExecution)
                    verlengingRedenExpression.getValue(delegateExecution)
                }
            }

            And("the zaak and tasks extend is called with correct parameters") {
                verify(exactly = 1) {
                    suspensionZaakHelper.extendZaak(
                        zaak,
                        any<LocalDate>(),
                        any<LocalDate>(),
                        any<String>(),
                        2
                    )
                    suspensionZaakHelper.extendTasks(zaak, 2)
                }
            }

            And("events are sent") {
                // 1 task event + 1 zaak event
                verify(exactly = 2) {
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }
})
