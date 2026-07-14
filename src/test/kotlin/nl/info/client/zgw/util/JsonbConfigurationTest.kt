/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import java.net.URI

class SampleUriPayload {
    var url: URI? = null
}

class JsonbConfigurationTest : BehaviorSpec({
    val jsonbConfiguration = JsonbConfiguration()

    afterEach {
        checkUnnecessaryStub()
    }

    Given("the JsonbConfiguration context resolver") {
        When("getContext is called for different types") {
            val jsonbForUri = jsonbConfiguration.getContext(SampleUriPayload::class.java)
            val jsonbForString = jsonbConfiguration.getContext(String::class.java)

            Then("the same configured Jsonb instance is returned regardless of type") {
                (jsonbForUri === jsonbForString) shouldBe true
            }
        }

        When("a URI field is deserialized") {
            val jsonb = jsonbConfiguration.getContext(SampleUriPayload::class.java)
            val samplePayload = jsonb.fromJson("""{"url":"https://example.com/zaken/1"}""", SampleUriPayload::class.java)

            Then("the URIJsonbDeserializer is used to parse it") {
                samplePayload.url shouldBe URI("https://example.com/zaken/1")
            }
        }
    }
})
