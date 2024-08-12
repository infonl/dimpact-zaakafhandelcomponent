/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.documentcreation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.zac.app.documentcreation.model.RestDocumentCreationAttendedData
import net.atos.zac.documentcreation.DocumentCreationService
import net.atos.zac.documentcreation.model.DocumentCreationData
import net.atos.zac.documentcreation.model.createDocumentCreationAttendedResponse
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createZaakRechtenAllDeny

class DocumentCreationRestServiceTest : BehaviorSpec({
    val documentCreationService = mockk<DocumentCreationService>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val documentCreationRestService = DocumentCreationRestService(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        policyService = policyService,
        documentCreationService = documentCreationService,
    )

    isolationMode = IsolationMode.InstancePerTest

    Given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
        val zaak = createZaak()
        val restDocumentCreationAttendedData = RestDocumentCreationAttendedData(
            zaakUUID = zaak.uuid,
            taskId = "dummyTaskId"
        )
        val documentCreationResponse = createDocumentCreationAttendedResponse()
        val documentCreationData = slot<DocumentCreationData>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readInformatieobjecttypen(zaak.zaaktype) } returns listOf(
            createInformatieObjectType(omschrijving = "bijlage")
        )
        every {
            documentCreationService.createDocumentAttended(capture(documentCreationData))
        } returns documentCreationResponse

        When("createDocument is called by a role that is allowed to change the zaak") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creeerenDocument = true
            )

            val restDocumentCreationResponse = documentCreationRestService.createDocumentAttended(
                restDocumentCreationAttendedData
            )

            Then("the document creation service is called to create the document") {
                restDocumentCreationResponse.message shouldBe null
                restDocumentCreationResponse.redirectURL shouldBe documentCreationResponse.redirectUrl
                with(documentCreationData.captured) {
                    this.zaak shouldBe zaak
                    this.taskId shouldBe restDocumentCreationAttendedData.taskId
                    this.informatieobjecttype?.omschrijving shouldBe "bijlage"
                }
            }
        }

        When("createDocument is called by a user that has no access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            Then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }
    }
})
