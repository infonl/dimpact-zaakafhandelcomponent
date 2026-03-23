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
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.spi.CDI
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createOndertekening
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.UUID

class SignDocumentDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val eventingService = mockk<EventingService>()
    val zaakUuid = UUID.randomUUID()
    val documentUuid = UUID.randomUUID()
    val documentenKeyPrefix = "ZAAK_Documents_To_Sign_Select"

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a BPMN service task for signing documents") {
        mockkObject(FlowableHelper)
        mockkStatic(CDI::class)

        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakVariabelenService } returns zaakVariabelenService
        every { flowableHelper.eventingService } returns eventingService

        val cdiInstance = mockk<CDI<Any>>()
        every { CDI.current() } returns cdiInstance

        val drcServiceInstance = mockk<Instance<DrcClientService>>()
        every { cdiInstance.select(DrcClientService::class.java) } returns drcServiceInstance
        every { drcServiceInstance.get() } returns drcClientService

        val updateServiceInstance = mockk<Instance<EnkelvoudigInformatieObjectUpdateService>>()
        every { cdiInstance.select(EnkelvoudigInformatieObjectUpdateService::class.java) } returns updateServiceInstance
        every { updateServiceInstance.get() } returns enkelvoudigInformatieObjectUpdateService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "Sign Documents"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_UUID) } returns zaakUuid

        val documentenKeyExpression = mockk<Expression>()
        every { documentenKeyExpression.toString() } returns documentenKeyPrefix

        When("a single unsigned document is found") {
            val document = createEnkelvoudigInformatieObject(uuid = documentUuid, ondertekening = null)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document
            every {
                enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
            } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("the document is signed") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
                }
            }

            And("a screen event is sent") {
                verify(exactly = 1) { eventingService.send(any<ScreenEvent>()) }
            }
        }

        When("the document is already signed") {
            val signedDocument = createEnkelvoudigInformatieObject(
                uuid = documentUuid,
                ondertekening = createOndertekening()
            )
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns signedDocument

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("the document is not signed again") {
                verify(exactly = 0) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
                }
            }

            And("no screen event is sent") {
                verify(exactly = 0) { eventingService.send(any<ScreenEvent>()) }
            }
        }

        When("no documents match the key prefix") {
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                "ZAAK_SomeOtherField" to listOf("some-value")
            )

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("no documents are signed") {
                verify(exactly = 0) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
                }
            }
        }

        When("multiple documents are found across numbered keys") {
            val documentUuid2 = UUID.randomUUID()
            val document1 = createEnkelvoudigInformatieObject(uuid = documentUuid, ondertekening = null)
            val document2 = createEnkelvoudigInformatieObject(uuid = documentUuid2, ondertekening = null)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString()),
                "$documentenKeyPrefix (1)" to listOf(documentUuid2.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document1
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid2) } returns document2
            every {
                enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
            } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("all documents are signed") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
                }
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid2)
                }
            }

            And("a screen event is sent for each document") {
                verify(exactly = 2) { eventingService.send(any<ScreenEvent>()) }
            }
        }
    }
})
