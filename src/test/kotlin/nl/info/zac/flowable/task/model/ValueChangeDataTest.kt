/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.task.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.util.JsonbUtil

class ValueChangeDataTest : BehaviorSpec({
    given("Task history data stored without an explanation") {
        val storedData = """{"newValue":"fakeNewValue","oldValue":"fakeOldValue"}"""

        `when`("the data is deserialized") {
            val valueChangeData = JsonbUtil.JSONB.fromJson(storedData, ValueChangeData::class.java)

            then("the old and new values are read and the explanation is null") {
                valueChangeData shouldBe ValueChangeData(
                    oldValue = "fakeOldValue",
                    newValue = "fakeNewValue",
                    explanation = null
                )
            }
        }
    }
    given("Value change data with an explanation") {
        val valueChangeData = ValueChangeData(
            oldValue = "fakeOldValue",
            newValue = "fakeNewValue",
            explanation = "fakeExplanation"
        )

        `when`("the data is serialized the way it is stored in the task history") {
            val json = JsonbUtil.FIELD_VISIBILITY_STRATEGY.toJson(valueChangeData)

            then("the JSON keeps the field names of the stored task history data") {
                json shouldBe """{"explanation":"fakeExplanation","newValue":"fakeNewValue","oldValue":"fakeOldValue"}"""
            }
            and("it deserializes to the same value change data") {
                JsonbUtil.JSONB.fromJson(json, ValueChangeData::class.java) shouldBe valueChangeData
            }
        }
    }
})
