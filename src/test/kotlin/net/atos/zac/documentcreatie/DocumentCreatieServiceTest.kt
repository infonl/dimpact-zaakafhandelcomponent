package net.atos.zac.documentcreatie

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.client.sd.SmartDocumentsClient
import net.atos.client.sd.model.createListTemplatesResponse
import net.atos.client.sd.model.createWizardResponse
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.documentcreatie.converter.DataConverter
import net.atos.zac.documentcreatie.model.createData
import net.atos.zac.documentcreatie.model.createDocumentCreatieGegevens
import java.net.URI
import java.util.Optional
import java.util.UUID

class DocumentCreatieServiceTest : BehaviorSpec({
    val smartDocumentsClient = mockk<SmartDocumentsClient>()
    val smartDocumentsURL = "http://example.com/dummySmartDocumentsURL"
    val authenticationToken = "dummyAuthenticationToken"
    val fixedUserName = Optional.of("dummyFixedUserName")
    val dataConverter = mockk<DataConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val ztcClientService = mockk<ZTCClientService>()
    val zrcClientService = mockk<ZRCClientService>()

    val documentCreatieService = DocumentCreatieService(
        smartDocumentsClient,
        smartDocumentsURL,
        authenticationToken,
        fixedUserName,
        dataConverter,
        loggedInUserInstance,
        ztcClientService,
        zrcClientService
    )

    Given("document creation data") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakTypeURI = URI("http://example.com/$zaakTypeUUID")
        val zaakType = createZaakType(uri = zaakTypeURI)
        val documentCreatieGegevens = createDocumentCreatieGegevens(
            zaak = createZaak(zaakTypeURI = zaakTypeURI),
        )
        val externalZaakUrl = URI("http://example.com/dummyExternalZaakUrl")
        val loggedInUser = createLoggedInUser()
        val data = createData()
        val wizardResponse = createWizardResponse()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { zrcClientService.createUrlExternToZaak(documentCreatieGegevens.zaak.uuid) } returns externalZaakUrl
        every { dataConverter.createData(documentCreatieGegevens, loggedInUser) } returns data
        every { ztcClientService.readZaaktype(documentCreatieGegevens.zaak.zaaktype) } returns zaakType
        every {
            smartDocumentsClient.wizardDeposit("Basic $authenticationToken", fixedUserName.get(), any())
        } returns wizardResponse

        When("the create attended document method is called") {
            val documentCreatieResponse = documentCreatieService.creeerDocumentAttendedSD(documentCreatieGegevens)

            Then(
                """
                the attended SmartDocuments document creation wizard is started and a document creation response is returned
                """
            ) {
                with(documentCreatieResponse) {
                    redirectUrl shouldBe URI("$smartDocumentsURL/smartdocuments/wizard?ticket=${wizardResponse.ticket}")
                    message shouldBe null
                }
            }
        }
    }

    Given("SD contains templates") {
        val loggedInUser = createLoggedInUser()
        every { loggedInUserInstance.get() } returns loggedInUser

        val templatesResponse = createListTemplatesResponse()
        every {
            smartDocumentsClient.listTemplates("Basic $authenticationToken", fixedUserName.get())
        } returns templatesResponse

        When("list templates is called") {
            val templates = documentCreatieService.listTemplates()

            Then("it should return a list of templates") {
                with(templates.documentsStructure.templatesStructure) {
                    templateGroups.size shouldBe 2
                }
            }
        }
    }
})
