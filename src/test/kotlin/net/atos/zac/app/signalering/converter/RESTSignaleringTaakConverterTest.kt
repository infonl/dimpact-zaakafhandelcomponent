/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.signalering.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import net.atos.zac.flowable.createTestTask
import org.flowable.common.engine.api.scope.ScopeTypes.CMMN
import java.time.Month
import java.util.Calendar

class RESTSignaleringTaakConverterTest : BehaviorSpec({
    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A task of scope type CMMN with a zaakIdentificatie and zaaktypeOmschrijving") {
        val zaakIdentificatie = "dummyZzaakIdentificatie"
        val zaaktypeOmschrijving = "my-zaaktype-omschrijving"
        val cal = Calendar.getInstance()
        cal[Calendar.YEAR] = 1988
        cal[Calendar.MONTH] = Calendar.JANUARY
        cal[Calendar.DAY_OF_MONTH] = 1
        val task = createTestTask(
            createTime = cal.time,
            caseVariables = mapOf(
                "zaakIdentificatie" to zaakIdentificatie,
                "zaaktypeOmschrijving" to zaaktypeOmschrijving
            ),
            scopeType = CMMN
        )

        When("the task is converted to a rest signalering task summary") {
            val summary = task.toRestSignaleringTaakSummary()

            Then("it returns correct history lines") {
                with(summary) {
                    id shouldBe task.id
                    naam shouldBe task.name
                    zaakIdentificatie shouldBe zaakIdentificatie
                    zaaktypeOmschrijving shouldBe zaaktypeOmschrijving
                    with(creatiedatumTijd) {
                        year shouldBe 1988
                        month shouldBe Month.JANUARY
                        dayOfMonth shouldBe 1
                    }
                }
            }
        }
    }
})
