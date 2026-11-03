/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.BRP_DOELBINDING_RAADPLEEG_WAARDE
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.BRP_DOELBINDING_ZOEK_WAARDE
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.BRP_VERWERKINGSREGISTER_WAARDE
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue

class BrpInstellingenCheckTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    given("BRP reference tables that each have only ASCII values") {
        val referenceTableService = mockk<ReferenceTableService>()
        val brpInstellingenCheck = BrpInstellingenCheck(referenceTableService)
        listOf(BRP_DOELBINDING_ZOEK_WAARDE, BRP_DOELBINDING_RAADPLEEG_WAARDE, BRP_VERWERKINGSREGISTER_WAARDE).forEach {
            every {
                referenceTableService.readSystemReferenceTable(it)
            } returns createReferenceTable(values = mutableListOf(createReferenceTableValue(name = "fakeAsciiValue")))
        }

        `when`("the BRP settings are checked twice in the same request") {
            val isCorrectAtFirstCheck = brpInstellingenCheck.isBrpInstellingenCorrect()
            val isCorrectAtSecondCheck = brpInstellingenCheck.isBrpInstellingenCorrect()

            then("the BRP settings are correct and each reference table is read only once") {
                isCorrectAtFirstCheck shouldBe true
                isCorrectAtSecondCheck shouldBe true
                verify(exactly = 1) {
                    referenceTableService.readSystemReferenceTable(BRP_DOELBINDING_ZOEK_WAARDE)
                    referenceTableService.readSystemReferenceTable(BRP_DOELBINDING_RAADPLEEG_WAARDE)
                    referenceTableService.readSystemReferenceTable(BRP_VERWERKINGSREGISTER_WAARDE)
                }
            }
        }
    }

    given("A BRP reference table with a non-ASCII value") {
        val referenceTableService = mockk<ReferenceTableService>()
        val brpInstellingenCheck = BrpInstellingenCheck(referenceTableService)
        every {
            referenceTableService.readSystemReferenceTable(BRP_DOELBINDING_ZOEK_WAARDE)
        } returns createReferenceTable(values = mutableListOf(createReferenceTableValue(name = "Bíj́na")))

        `when`("the BRP settings are checked twice in the same request") {
            val isCorrectAtFirstCheck = brpInstellingenCheck.isBrpInstellingenCorrect()
            val isCorrectAtSecondCheck = brpInstellingenCheck.isBrpInstellingenCorrect()

            then("the BRP settings are incorrect, also when the result is remembered") {
                isCorrectAtFirstCheck shouldBe false
                isCorrectAtSecondCheck shouldBe false
                verify(exactly = 1) { referenceTableService.readSystemReferenceTable(BRP_DOELBINDING_ZOEK_WAARDE) }
            }
        }
    }

    given("A BRP reference table without values") {
        val referenceTableService = mockk<ReferenceTableService>()
        val brpInstellingenCheck = BrpInstellingenCheck(referenceTableService)
        every {
            referenceTableService.readSystemReferenceTable(BRP_DOELBINDING_ZOEK_WAARDE)
        } returns createReferenceTable(values = mutableListOf())

        `when`("the BRP settings are checked") {
            val isBrpInstellingenCorrect = brpInstellingenCheck.isBrpInstellingenCorrect()

            then("the BRP settings are incorrect") {
                isBrpInstellingenCorrect shouldBe false
            }
        }
    }
}
)
