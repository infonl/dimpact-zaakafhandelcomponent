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
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.app.documentcreation.rest.RestDocumentCreationAttendedData
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.DocumentCreationService
import net.atos.zac.documentcreation.model.DocumentCreationDataAttended
import net.atos.zac.documentcreation.model.createDocumentCreationAttendedResponse
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createZaakRechtenAllDeny
import java.net.URI
import java.util.UUID

class DocumentCreationRestServiceTest : BehaviorSpec({
    val documentCreationService = mockk<DocumentCreationService>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = mockk<ConfiguratieService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val documentCreationRestService = DocumentCreationRestService(
        policyService = policyService,
        documentCreationService = documentCreationService,
        zrcClientService = zrcClientService,
        configurationService = configurationService,
        zaakafhandelParameterService = zaakafhandelParameterService
    )

    isolationMode = IsolationMode.InstancePerTest

    Given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaak = createZaak(
            zaakTypeURI = URI("https://example.com/$zaakTypeUUID"),
        )
        val restDocumentCreationAttendedData = RestDocumentCreationAttendedData(
            zaakUuid = zaak.uuid,
            taskId = "dummyTaskId",
            smartDocumentsTemplateGroupId = "groupId",
            smartDocumentsTemplateId = "templateId",
            title = "Title",
        )
        val documentCreationResponse = createDocumentCreationAttendedResponse()
        val documentCreationDataAttended = slot<DocumentCreationDataAttended>()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { ztcClientService.readInformatieobjecttypen(zaak.zaaktype) } returns listOf(
            createInformatieObjectType(omschrijving = "bijlage")
        )
        every {
            documentCreationService.createDocumentAttended(capture(documentCreationDataAttended))
        } returns documentCreationResponse

        When("createDocument is called by a role that is allowed to change the zaak") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creeerenDocument = true
            )
            every { zaakafhandelParameterService.isDocumentCreationEnabled(zaakTypeUUID) } returns true

            val restDocumentCreationResponse = documentCreationRestService.createDocumentAttended(
                restDocumentCreationAttendedData
            )

            Then("the document creation service is called to create the document") {
                restDocumentCreationResponse.message shouldBe documentCreationResponse.message
                restDocumentCreationResponse.redirectURL shouldBe documentCreationResponse.redirectUrl
                with(documentCreationDataAttended.captured) {
                    this.zaak shouldBe zaak
                    taskId shouldBe restDocumentCreationAttendedData.taskId
                    templateId shouldBe restDocumentCreationAttendedData.smartDocumentsTemplateId
                    templateGroupId shouldBe restDocumentCreationAttendedData.smartDocumentsTemplateGroupId
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

        When("createDocument is called with disabled document creation") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                creeerenDocument = true
            )
            every { zaakafhandelParameterService.isDocumentCreationEnabled(zaakTypeUUID) } returns false

            val exception = shouldThrow<PolicyException> {
                documentCreationRestService.createDocumentAttended(restDocumentCreationAttendedData)
            }

            Then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }
    }
})
