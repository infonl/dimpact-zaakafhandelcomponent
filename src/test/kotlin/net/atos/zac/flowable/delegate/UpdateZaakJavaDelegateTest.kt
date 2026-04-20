/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.client.zgw.shared.model.createValidationZgwError
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatusSub
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import org.flowable.common.engine.impl.el.FixedValue
import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.impl.el.JuelExpression
import java.net.URI
import java.util.UUID

class UpdateZaakJavaDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zrcClientService = mockk<ZrcClientService>()
    val zgwApiService = mockk<ZgwApiService>()
    mockkObject(FlowableHelper)
    val flowableHelper = mockk<FlowableHelper>()
    val zaak = createZaak()
    val zaakStatusName = "fakeStatus"
    val zaakStatus = createZaakStatusSub()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("JUEL expression in a BPMN service task") {
        val juelExpression = mockk<JuelExpression>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = juelExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak
        every { juelExpression.getValue(delegateExecution) } returns zaakStatusName
        every {
            zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
        } returns zaakStatus

        When("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            Then("the expression was resolved") {
                verify(exactly = 1) {
                    juelExpression.getValue(delegateExecution)
                }
            }

            And("the status was updated in the ZGW API") {
                verify(exactly = 1) {
                    zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
                }
            }
        }
    }

    Given("Fixed value in a BPMN service task") {
        val fixedValueExpression = mockk<FixedValue>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = fixedValueExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak
        every { fixedValueExpression.getValue(delegateExecution) } returns zaakStatusName
        every {
            zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
        } returns zaakStatus

        When("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            Then("the value is obtained") {
                verify(exactly = 1) {
                    fixedValueExpression.getValue(delegateExecution)
                }
            }

            And("the status was updated in the ZGW API") {
                verify(exactly = 1) {
                    zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
                }
            }
        }
    }

    Given("a resultaattype omschrijving is set in a BPMN service task") {
        val resultaattypeDescription = "fakeResultaattype"
        val resultaattypeUuid = UUID.randomUUID()
        val resultaatType = createResultaatType(url = URI("https://example.com/resultaattype/$resultaattypeUuid"))
        val fixedValueExpression = mockk<FixedValue>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = mockk()
            resultaattypeOmschrijving = fixedValueExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak
        every { fixedValueExpression.getValue(delegateExecution) } returns resultaattypeDescription
        every { zgwApiService.getResultaatType(zaak.zaaktype, resultaattypeDescription) } returns resultaatType
        every { zgwApiService.closeZaak(zaak, resultaattypeUuid, "Aangepast vanuit proces") } just Runs

        When("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            Then("the resultaattype was looked up") {
                verify(exactly = 1) {
                    zgwApiService.getResultaatType(zaak.zaaktype, resultaattypeDescription)
                }
            }

            And("the zaak was closed via the ZGW API") {
                verify(exactly = 1) {
                    zgwApiService.closeZaak(zaak, resultaattypeUuid, "Aangepast vanuit proces")
                }
            }
        }
    }

    Given("a resultaattype omschrijving is set but ZGW validation fails") {
        val resultaattypeDescription = "fakeResultaattype"
        val resultaattypeUuid = UUID.randomUUID()
        val resultaatType = createResultaatType(url = URI("https://example.com/resultaattype/$resultaattypeUuid"))
        val fixedValueExpression = mockk<FixedValue>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = mockk()
            resultaattypeOmschrijving = fixedValueExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zrcClientService } returns zrcClientService
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zrcClientService.readZaakByID(zaak.identificatie) } returns zaak
        every { fixedValueExpression.getValue(delegateExecution) } returns resultaattypeDescription
        every { zgwApiService.getResultaatType(zaak.zaaktype, resultaattypeDescription) } returns resultaatType
        every { zgwApiService.closeZaak(zaak, resultaattypeUuid, "Aangepast vanuit proces") } throws
            ZgwValidationErrorException(createValidationZgwError())

        When("the delegate is called") {
            val exception = shouldThrow<FlowableZgwValidationErrorException> {
                updateZaakJavaDelegate.execute(delegateExecution)
            }

            Then("a FlowableZgwValidationErrorException is thrown") {
                exception.message shouldBe "Failed to end zaak"
            }
        }
    }
})
