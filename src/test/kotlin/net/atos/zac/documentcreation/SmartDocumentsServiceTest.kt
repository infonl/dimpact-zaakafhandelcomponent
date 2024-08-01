package net.atos.zac.documentcreation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.client.smartdocuments.SmartDocumentsClient
import net.atos.client.smartdocuments.model.createTemplatesResponse
import net.atos.client.smartdocuments.model.createWizardResponse
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter
import net.atos.zac.documentcreation.model.createData
import net.atos.zac.documentcreation.model.createDocumentCreationData
import java.net.URI
import java.util.Optional
import java.util.UUID

class SmartDocumentsServiceTest : BehaviorSpec({
    val smartDocumentsClient = mockk<SmartDocumentsClient>()
    val smartDocumentsURL = "http://example.com/dummySmartDocumentsURL"
    val authenticationToken = "dummyAuthenticationToken"
    val fixedUserName = Optional.of("dummyFixedUserName")
    val documentCreationDataConverter = mockk<DocumentCreationDataConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val smartDocumentsService = SmartDocumentsService(
        smartDocumentsClient,
        smartDocumentsURL,
        authenticationToken,
        fixedUserName,
        documentCreationDataConverter,
        loggedInUserInstance,
        ztcClientService,
        zrcClientService
    )

    Given("Document creation data with an information object type") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakTypeURI = URI("http://example.com/$zaakTypeUUID")
        val zaakType = createZaakType(uri = zaakTypeURI)
        val documentCreationData = createDocumentCreationData(
            zaak = createZaak(zaakTypeURI = zaakTypeURI),
            informatieobjecttype = createInformatieObjectType()
        )
        val externalZaakUrl = URI("http://example.com/dummyExternalZaakUrl")
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val wizardResponse = createWizardResponse()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { zrcClientService.createUrlExternToZaak(documentCreationData.zaak.uuid) } returns externalZaakUrl
        every {
            documentCreationDataConverter.createData(
                loggedInUser,
                documentCreationData.zaak,
                documentCreationData.taskId
            )
        } returns data
        every { ztcClientService.readZaaktype(documentCreationData.zaak.zaaktype) } returns zaakType
        every {
            smartDocumentsClient.attendedDeposit("Basic $authenticationToken", fixedUserName.get(), any())
        } returns wizardResponse

        When("the 'create document attended' method is called") {
            val documentCreationResponse = smartDocumentsService.createDocumentAttended(documentCreationData)

            Then(
                """
                the attended SmartDocuments document creation wizard is started and a document creation response is returned
                """
            ) {
                with(documentCreationResponse) {
                    redirectUrl shouldBe URI("$smartDocumentsURL/smartdocuments/wizard?ticket=${wizardResponse.ticket}")
                    message shouldBe null
                }
            }
        }
    }

    Given("SD contains templates") {
        val loggedInUser = createLoggedInUser()
        every { loggedInUserInstance.get() } returns loggedInUser

        val templatesResponse = createTemplatesResponse()
        every {
            smartDocumentsClient.listTemplates("Basic $authenticationToken", fixedUserName.get())
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
