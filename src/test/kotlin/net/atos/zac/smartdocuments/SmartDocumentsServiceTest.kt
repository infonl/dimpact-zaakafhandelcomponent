/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.model.createAttendedResponse
import net.atos.client.smartdocuments.model.createFile
import net.atos.client.smartdocuments.model.createRegistratie
import net.atos.client.smartdocuments.model.createSmartDocument
import net.atos.client.smartdocuments.model.createUnattendedResponse
import net.atos.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.documentcreation.model.createData
import java.net.URI
import java.util.Optional

class SmartDocumentsServiceTest : BehaviorSpec({
    val smartDocumentsURL = "https://example.com/dummySmartDocumentsURL"
    val authenticationToken = "dummyAuthenticationToken"
    val fixedUserName = Optional.of("dummyFixedUserName")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val smartDocumentsClient = mockk<SmartDocumentsClient>()
    val smartDocumentsService = SmartDocumentsService(
        smartDocumentsClient = smartDocumentsClient,
        smartDocumentsURL = smartDocumentsURL,
        authenticationToken = authenticationToken,
        loggedInUserInstance = loggedInUserInstance,
        fixedUserName = fixedUserName
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("Document creation data with a zaak and an information object type") {
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val registratie = createRegistratie()
        val smartDocument = createSmartDocument()
        val attendedResponse = createAttendedResponse()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { smartDocumentsClient.attendedDeposit(any(), any(), any()) } returns attendedResponse

        When("the 'create document attended' method is called") {
            val documentCreationResponse = smartDocumentsService.createDocumentAttended(
                data = data,
                registratie = registratie,
                smartDocument = smartDocument
            )

            Then(
                """
                the attended SmartDocuments document creation wizard is started and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    redirectUrl shouldBe URI("$smartDocumentsURL/smartdocuments/wizard?ticket=${attendedResponse.ticket}")
                    message shouldBe null
                }
            }
        }
    }
    Given("Document creation data with a zaak, a template group name and a template name") {
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val smartDocument = createSmartDocument()
        val file = createFile(
            fileName = "dummyTemplateName.docx",
            outputFormat = "DOCX"
        )
        val unattendedResponse = createUnattendedResponse(
            files = listOf(file)
        )
        every { loggedInUserInstance.get() } returns loggedInUser
        every { smartDocumentsClient.unattendedDeposit(any(), any(), any()) } returns unattendedResponse

        When("the 'create document unattended' method is called") {
            val documentCreationResponse = smartDocumentsService.createDocumentUnattended(
                data = data,
                smartDocument = smartDocument
            )

            Then(
                """
                the create unattended SmartDocuments document method is called and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    message shouldBe "SmartDocuments document with filename: '${file.fileName}' was created successfully " +
                        "but the document is not stored yet in the zaakregister."
                }
            }
        }
    }
    Given("SD contains templates") {
        val loggedInUser = createLoggedInUser()
        every { loggedInUserInstance.get() } returns loggedInUser

        val templatesResponse = createsmartDocumentsTemplatesResponse()
        every {
            smartDocumentsClient.listTemplates(any(), any())
        } returns templatesResponse

        When("list templates is called") {
            val templatesList = smartDocumentsService.listTemplates()

            Then("it should return a list of templates") {
                with(templatesList.documentsStructure.templatesStructure.templateGroups) {
                    size shouldBe 1
                    with(first()) {
                        name shouldBe "Dimpact"
                        templateGroups!!.size shouldBe 2
                        templateGroups!!.first().name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                        templates!!.size shouldBe 2
                        templates!!.first().name shouldBe "Aanvullende informatie nieuw"
                    }
                }
            }
        }
    }
})
