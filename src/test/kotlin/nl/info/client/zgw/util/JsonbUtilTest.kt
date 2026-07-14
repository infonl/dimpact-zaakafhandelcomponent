/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.checkUnnecessaryStub

class SamplePayload {
    var value: String = ""
        get() = "overridden-$field"
}

class JsonbUtilTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    Given("a class with a public field and a getter that transforms its value") {
        When("JSONB serializes an instance") {
            val samplePayload = SamplePayload().apply { value = "rawValue" }
            val json = JSONB.toJson(samplePayload)

            Then("the raw field value is serialized, not the getter's transformed value") {
                json shouldContain "rawValue"
                json shouldNotContain "overridden-rawValue"
            }
        }

        When("JSONB deserializes a JSON object") {
            val samplePayload = JSONB.fromJson("""{"value":"fromJson"}""", SamplePayload::class.java)

            Then("the field is set directly and the getter transforms it on read") {
                samplePayload.value shouldBe "overridden-fromJson"
            }
        }
    }
})
