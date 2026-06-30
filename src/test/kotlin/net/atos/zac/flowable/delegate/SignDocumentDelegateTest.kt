/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
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
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.spi.CDI
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createOndertekening
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createDocumentRechtenAllDeny
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.UUID

class SignDocumentDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>(relaxed = true)
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val eventingService = mockk<EventingService>(relaxed = true)
    val zrcClientService = mockk<ZrcClientService>()
    val policyService = mockk<PolicyService>()
    val zaak = createZaak()
    val zaakUuid = zaak.uuid
    val documentUuid = UUID.randomUUID()
    val documentenKeyPrefix = "ZAAK_Documents_To_Sign_Select"

    afterSpec {
        unmockkStatic(CDI::class)
    }

    afterEach {
        checkUnnecessaryStub()
    }

    Given("a BPMN service task for signing documents") {
        mockkObject(FlowableHelper)
        mockkStatic(CDI::class)

        val flowableHelper = mockk<FlowableHelper>()
        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakVariabelenService } returns zaakVariabelenService
        every { flowableHelper.eventingService } returns eventingService
        every { flowableHelper.drcClientService } returns drcClientService
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.policyService } returns policyService

        val cdiInstance = mockk<CDI<Any>>()
        every { CDI.current() } returns cdiInstance

        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak

        val updateServiceInstance = mockk<Instance<EnkelvoudigInformatieObjectUpdateService>>()
        every { cdiInstance.select(EnkelvoudigInformatieObjectUpdateService::class.java) } returns updateServiceInstance
        every { updateServiceInstance.get() } returns enkelvoudigInformatieObjectUpdateService

        every { delegateExecution.parent } returns parentDelegateExecution
        every { delegateExecution.currentActivityName } returns "Sign Documents"
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_UUID) } returns zaakUuid
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie

        val documentenKeyExpression = mockk<Expression>()
        every { documentenKeyExpression.getValue(delegateExecution) } returns documentenKeyPrefix

        When("a single unsigned document is found") {
            clearMocks(enkelvoudigInformatieObjectUpdateService, answers = false)

            val document = createEnkelvoudigInformatieObject(uuid = documentUuid, ondertekening = null)
            every { policyService.readDocumentRechten(document, zaak) } returns createDocumentRechtenAllDeny(ondertekenen = true)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("the document is signed") {
                verify(exactly = 1) { documentenKeyExpression.getValue(delegateExecution) }
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
                }
            }
        }

        When("the document is already signed") {
            clearMocks(enkelvoudigInformatieObjectUpdateService, documentenKeyExpression, answers = false)

            val signedDocument = createEnkelvoudigInformatieObject(
                uuid = documentUuid,
                ondertekening = createOndertekening()
            )
            every {
                policyService.readDocumentRechten(signedDocument, zaak)
            } returns createDocumentRechtenAllDeny(ondertekenen = true)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns signedDocument

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("the document is not signed again") {
                verify(exactly = 1) { documentenKeyExpression.getValue(delegateExecution) }
                verify(exactly = 0) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
                }
            }
        }

        When("no documents match the key prefix") {
            clearMocks(enkelvoudigInformatieObjectUpdateService, documentenKeyExpression, answers = false)

            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                "ZAAK_SomeOtherField" to listOf("some-value")
            )

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("no documents are signed") {
                verify(exactly = 1) { documentenKeyExpression.getValue(delegateExecution) }
                verify(exactly = 0) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
                }
            }
        }

        When("no documentenKey is configured") {
            clearMocks(enkelvoudigInformatieObjectUpdateService, answers = false)

            val defaultKey = "ZAAK_Documenten_Ondertekenen_Selectie"
            val document = createEnkelvoudigInformatieObject(uuid = documentUuid, ondertekening = null)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                defaultKey to listOf(documentUuid.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document
            every { policyService.readDocumentRechten(document, zaak) } returns createDocumentRechtenAllDeny(ondertekenen = true)

            SignDocumentDelegate().execute(delegateExecution)

            Then("the default key is used and the document is signed") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
                }
            }
        }

        When("multiple documents are found across numbered keys") {
            clearMocks(enkelvoudigInformatieObjectUpdateService, documentenKeyExpression, answers = false)

            val documentUuid2 = UUID.randomUUID()
            val document1 = createEnkelvoudigInformatieObject(uuid = documentUuid, ondertekening = null)
            val document2 = createEnkelvoudigInformatieObject(uuid = documentUuid2, ondertekening = null)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString()),
                "$documentenKeyPrefix (1)" to listOf(documentUuid2.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document1
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid2) } returns document2
            every { policyService.readDocumentRechten(document1, zaak) } returns createDocumentRechtenAllDeny(ondertekenen = true)
            every { policyService.readDocumentRechten(document2, zaak) } returns createDocumentRechtenAllDeny(ondertekenen = true)

            SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                .execute(delegateExecution)

            Then("all documents are signed") {
                verify(exactly = 1) { documentenKeyExpression.getValue(delegateExecution) }
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid)
                }
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(documentUuid2)
                }
            }
        }

        When("policy denies signing a document") {
            clearMocks(enkelvoudigInformatieObjectUpdateService, documentenKeyExpression, answers = false)

            val document = createEnkelvoudigInformatieObject(uuid = documentUuid, ondertekening = null)
            every { zaakVariabelenService.readZaakdata(zaakUuid) } returns mapOf(
                documentenKeyPrefix to listOf(documentUuid.toString())
            )
            every { drcClientService.readEnkelvoudigInformatieobject(documentUuid) } returns document
            every { policyService.readDocumentRechten(document, zaak) } returns createDocumentRechtenAllDeny()
            every { documentenKeyExpression.getValue(delegateExecution) } returns documentenKeyPrefix

            val policyException = shouldThrow<PolicyException> {
                SignDocumentDelegate().apply { documentenKey = documentenKeyExpression }
                    .execute(delegateExecution)
            }

            Then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("the document is not signed") {
                verify(exactly = 0) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(any())
                }
            }
        }
    }
})
