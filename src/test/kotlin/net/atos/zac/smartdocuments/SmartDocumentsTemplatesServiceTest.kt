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
import jakarta.persistence.EntityManager
import net.atos.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import net.atos.zac.admin.ZaakafhandelParameterService

class SmartDocumentsTemplatesServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val smartDocumentsTemplatesService = SmartDocumentsTemplatesService(
        entityManager = entityManager,
        smartDocumentsService = smartDocumentsService,
        zaakafhandelParameterService = zaakafhandelParameterService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A list of SmartDocuments templates") {
        val smartDocumentsTemplatesResponse = createsmartDocumentsTemplatesResponse()
        every { smartDocumentsService.listTemplates() } returns smartDocumentsTemplatesResponse

        When("a list of template is requested") {
            val restSmartDocumentsTemplateGroupSet = smartDocumentsTemplatesService.listTemplates()

            Then("the template is returned") {
                restSmartDocumentsTemplateGroupSet.size shouldBe
                    smartDocumentsTemplatesResponse.documentsStructure.templatesStructure.templateGroups.size
            }
        }
    }
})
