/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import java.util.UUID

class ZaaktypeConfigurationBeheerServiceTest : BehaviorSpec({
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val zaaktypeConfigurationBeheerService = ZaaktypeConfigurationBeheerService(smartDocumentsTemplatesService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A previous zaaktype UUID with existing smart documents template mappings") {
        val previousZaaktypeUuid = UUID.randomUUID()
        val newZaaktypeUuid = UUID.randomUUID()
        val templateMappings = setOf(
            RestMappedSmartDocumentsTemplateGroup(
                id = "fakeTemplateGroupId",
                name = "fakeTemplateGroupName",
                groups = null,
                templates = null
            )
        )
        every { smartDocumentsTemplatesService.getTemplatesMapping(previousZaaktypeUuid) } returns templateMappings
        every { smartDocumentsTemplatesService.storeTemplatesMapping(templateMappings, newZaaktypeUuid) } just runs

        When("mapping smart documents from the previous zaaktype to the new zaaktype") {
            zaaktypeConfigurationBeheerService.mapSmartDocuments(previousZaaktypeUuid, newZaaktypeUuid)

            Then("the template mappings are retrieved from the previous zaaktype and stored for the new zaaktype") {
                verify(exactly = 1) {
                    smartDocumentsTemplatesService.getTemplatesMapping(previousZaaktypeUuid)
                    smartDocumentsTemplatesService.storeTemplatesMapping(templateMappings, newZaaktypeUuid)
                }
            }
        }
    }
})
