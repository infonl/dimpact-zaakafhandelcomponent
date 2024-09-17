package net.atos.zac.configuratie

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.CatalogusListParameters
import net.atos.client.zgw.ztc.model.generated.Catalogus
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

class ConfiguratieServiceTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaak exists") {
        val entityManager = mockk<EntityManager>()
        val ztcClientService = mockk<ZtcClientService>()
        val catalogus = mockk<Catalogus>()

        val additionalAllowedFileTypes = ""
        val zgwApiClientMpRestUrl = "https://example.com:1111"
        val contextUrl = "https://example.com:2222"
        val gemeenteCode = "gemeenteCode"
        val gemeenteNaam = "Gemeente Name"
        val gemeenteMail = "gemeente@example.com"
        val bpmnSupport = false
        val catalogusUri = "https://example.com/catalogus"
        val zaakUuid = UUID.randomUUID()
        val templateGroupId = "groupId"
        val templateId = "templateId"
        val userName = "Full User Name"

        every { catalogus.url } returns URI(catalogusUri)
        every { ztcClientService.readCatalogus(any<CatalogusListParameters>()) } returns catalogus

        val configurationService = ConfiguratieService(
            entityManager,
            ztcClientService,
            additionalAllowedFileTypes,
            zgwApiClientMpRestUrl,
            contextUrl,
            gemeenteCode,
            gemeenteNaam,
            gemeenteMail,
            bpmnSupport
        )

        When("Document creation URL is requested for zaak") {
            val uri = configurationService.documentCreationCallbackUrl(
                zaakUuid = zaakUuid,
                null,
                templateGroupId,
                templateId,
                userName
            )

            Then("Correct URl is provided") {
                uri.toString() shouldBe "$contextUrl/rest/document-creation/smartdocuments/callback/zaak/" +
                    "$zaakUuid?templateId=$templateId&templateGroupId=$templateGroupId&userName=" +
                    URLEncoder.encode(userName, Charsets.UTF_8)
            }
        }

        When("Document creation URL is requested for taak") {
            val taakUuid = UUID.randomUUID().toString()
            val uri = configurationService.documentCreationCallbackUrl(
                zaakUuid,
                taakUuid,
                templateGroupId,
                templateId,
                userName
            )

            Then("Correct URl is provided") {
                uri.toString() shouldBe "$contextUrl/rest/document-creation/smartdocuments/callback/zaak/" +
                    "$zaakUuid/task/$taakUuid?templateId=$templateId&templateGroupId=$templateGroupId" +
                    "&userName=" + URLEncoder.encode(userName, Charsets.UTF_8)
            }
        }
    }
})
