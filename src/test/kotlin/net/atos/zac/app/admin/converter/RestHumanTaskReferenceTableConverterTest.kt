/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.HumanTaskReferentieTabel
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.app.admin.model.RestReferenceTable

class RestHumanTaskReferenceTableConverterTest : BehaviorSpec({
    val referenceTableService = mockk<ReferenceTableService>()

    val converter = RestHumanTaskReferenceTableConverter().also { instance ->
        instance.javaClass.getDeclaredField("referenceTableService").also {
            it.isAccessible = true
            it.set(instance, referenceTableService)
        }
    }

    afterEach { checkUnnecessaryStub() }

    Context("convertDefault(FormulierVeldDefinitie)") {
        Given("A veld definitie with a default tabel") {
            val veldDefinitie = nl.info.zac.admin.model.FormulierVeldDefinitie.ADVIES
            val fakeReferenceTable = createReferenceTable(id = 99L)
            every { referenceTableService.readReferenceTable(veldDefinitie.defaultTabel.name) } returns fakeReferenceTable

            When("convertDefault is called") {
                val result = converter.convertDefault(veldDefinitie)

                Then("the returned REST entry has veld set and tabel resolved via the service") {
                    result.veld shouldBe veldDefinitie.name
                    result.tabel.id shouldBe 99L
                }
            }
        }
    }

    Context("convert(Collection<HumanTaskReferentieTabel>)") {
        Given("A collection of two HumanTaskReferentieTabel entries") {
            val fakeReferenceTable1 = createReferenceTable(code = "FAKE_TABLE_1")
            val fakeReferenceTable2 = createReferenceTable(code = "FAKE_TABLE_2")
            val entry1 = HumanTaskReferentieTabel("fakeVeld1", fakeReferenceTable1).apply { id = 1L }
            val entry2 = HumanTaskReferentieTabel("fakeVeld2", fakeReferenceTable2).apply { id = 2L }

            When("convert is called with the collection") {
                val result = converter.convert(listOf(entry1, entry2))

                Then("a list of two RestHumanTaskReferenceTable objects is returned with all fields mapped") {
                    result.size shouldBe 2
                    result[0].id shouldBe 1L
                    result[0].veld shouldBe "fakeVeld1"
                    result[1].id shouldBe 2L
                    result[1].veld shouldBe "fakeVeld2"
                }
            }
        }
    }

    Context("convert(List<RestHumanTaskReferenceTable>)") {
        Given("A list of one RestHumanTaskReferenceTable") {
            val fakeReferenceTable = createReferenceTable(id = 99L)
            val restEntry = net.atos.zac.app.admin.model.RestHumanTaskReferenceTable().apply {
                id = 5L
                veld = "fakeVeld"
                tabel = RestReferenceTable(id = 99L, code = "fakeCode", name = "fakeNaam")
            }
            every { referenceTableService.readReferenceTable(99L) } returns fakeReferenceTable

            When("convert is called with the list") {
                val result = converter.convert(listOf(restEntry))

                Then("a list of one HumanTaskReferentieTabel is returned with tabel fetched from service") {
                    result.size shouldBe 1
                    result[0].id shouldBe 5L
                    result[0].veld shouldBe "fakeVeld"
                    result[0].tabel shouldBe fakeReferenceTable
                }
            }
        }
    }
})
