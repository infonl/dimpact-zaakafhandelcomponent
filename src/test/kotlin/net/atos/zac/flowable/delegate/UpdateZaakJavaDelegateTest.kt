/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.shared.exception.ZgwValidationErrorException
import net.atos.client.zgw.shared.model.createValidationZgwError
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.exception.FlowableZgwValidationErrorException
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatusSub
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.ztc.model.createResultaatType
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
import java.net.URI
import java.util.UUID

class UpdateZaakJavaDelegateTest : BehaviorSpec({
    val delegateExecution = mockk<DelegateExecution>()
    val parentDelegateExecution = mockk<DelegateExecution>()
    val zgwApiService = mockk<ZgwApiService>()
    mockkObject(FlowableHelper)
    val flowableHelper = mockk<FlowableHelper>()
    val zaakService = mockk<ZaakService>()
    val policyService = mockk<PolicyService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()
    val zaak = createZaak()
    val zaaktype = createZaakType(uri = zaak.zaaktype)
    val zaakStatusName = "fakeStatus"
    val zaakStatus = createZaakStatusSub()

    afterEach {
        checkUnnecessaryStub()
    }

    given("JUEL expression in a BPMN service task") {
        val juelExpression = mockk<JuelExpression>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = juelExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(zaak, zaaktype, loggedInUser)
        } returns createZaakRechtenAllDeny(behandelen = true)
        every { juelExpression.getValue(delegateExecution) } returns zaakStatusName
        every {
            zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
        } returns zaakStatus

        `when`("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            then("the expression was resolved") {
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

    given("Fixed value in a BPMN service task") {
        val fixedValueExpression = mockk<FixedValue>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = fixedValueExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(zaak, zaaktype, loggedInUser)
        } returns createZaakRechtenAllDeny(behandelen = true)
        every { fixedValueExpression.getValue(delegateExecution) } returns zaakStatusName
        every {
            zgwApiService.createStatusForZaak(zaak, zaakStatusName, "Aangepast vanuit proces")
        } returns zaakStatus

        `when`("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            then("the value is obtained") {
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

    given("a resultaattype omschrijving is set in a BPMN service task") {
        val resultaattypeDescription = "fakeResultaattype"
        val resultaattypeUuid = UUID.randomUUID()
        val resultaatType = createResultaatType(url = URI("https://example.com/resultaattype/$resultaattypeUuid"))
        val fixedValueExpression = mockk<FixedValue>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = mockk()
            resultaattypeOmschrijving = fixedValueExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(zaak, zaaktype, loggedInUser)
        } returns createZaakRechtenAllDeny(behandelen = true)
        every { fixedValueExpression.getValue(delegateExecution) } returns resultaattypeDescription
        every { zgwApiService.getResultaatType(zaak.zaaktype, resultaattypeDescription) } returns resultaatType
        every { zgwApiService.closeZaak(zaak, resultaattypeUuid, "Aangepast vanuit proces", null) } just Runs

        `when`("the delegate is called") {
            updateZaakJavaDelegate.execute(delegateExecution)

            then("the resultaattype was looked up") {
                verify(exactly = 1) {
                    zgwApiService.getResultaatType(zaak.zaaktype, resultaattypeDescription)
                }
            }

            And("the zaak was closed via the ZGW API") {
                verify(exactly = 1) {
                    zgwApiService.closeZaak(zaak, resultaattypeUuid, "Aangepast vanuit proces", null)
                }
            }
        }
    }

    given("a resultaattype omschrijving is set but ZGW validation fails") {
        val resultaattypeDescription = "fakeResultaattype"
        val resultaattypeUuid = UUID.randomUUID()
        val resultaatType = createResultaatType(url = URI("https://example.com/resultaattype/$resultaattypeUuid"))
        val fixedValueExpression = mockk<FixedValue>()
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = mockk()
            resultaattypeOmschrijving = fixedValueExpression
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zgwApiService } returns zgwApiService
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(zaak, zaaktype, loggedInUser)
        } returns createZaakRechtenAllDeny(behandelen = true)
        every { fixedValueExpression.getValue(delegateExecution) } returns resultaattypeDescription
        every { zgwApiService.getResultaatType(zaak.zaaktype, resultaattypeDescription) } returns resultaatType
        every { zgwApiService.closeZaak(zaak, resultaattypeUuid, "Aangepast vanuit proces", null) } throws
            ZgwValidationErrorException(createValidationZgwError())

        `when`("the delegate is called") {
            val exception = shouldThrow<FlowableZgwValidationErrorException> {
                updateZaakJavaDelegate.execute(delegateExecution)
            }

            then("a FlowableZgwValidationErrorException is thrown") {
                exception.message shouldBe "Failed to close zaak with UUID: '${zaak.uuid}'"
            }
        }
    }

    given("Policy denies handling zaak in a BPMN service task") {
        val updateZaakJavaDelegate = UpdateZaakJavaDelegate().apply {
            statustypeOmschrijving = mockk()
        }

        every { FlowableHelper.getInstance() } returns flowableHelper
        every { flowableHelper.zaakService } returns zaakService
        every { flowableHelper.policyService } returns policyService
        every { flowableHelper.loggedInUserInstance } returns loggedInUserInstance
        every { delegateExecution.parent } returns parentDelegateExecution
        every { parentDelegateExecution.getVariable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE) } returns zaak.identificatie
        every { zaakService.readZaakAndZaakTypeByZaakID(zaak.identificatie) } returns Pair(zaak, zaaktype)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(zaak, zaaktype, loggedInUser)
        } returns createZaakRechtenAllDeny()

        `when`("the delegate is called") {
            val policyException = shouldThrow<PolicyException> {
                updateZaakJavaDelegate.execute(delegateExecution)
            }

            then("a PolicyException is thrown") {
                policyException shouldNotBe null
            }

            And("no ZGW API calls are made") {
                verify(exactly = 0) {
                    zgwApiService.createStatusForZaak(any(), any(), any())
                    zgwApiService.closeZaak(any(), any(), any(), any())
                }
            }
        }
    }
})
