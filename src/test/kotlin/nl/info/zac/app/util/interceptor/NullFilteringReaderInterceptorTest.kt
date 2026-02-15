/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.interceptor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.ext.ReaderInterceptorContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class NullFilteringReaderInterceptorTest : BehaviorSpec({
    val interceptor = NullFilteringReaderInterceptor()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a JSON request with null values") {
        val context = mockk<ReaderInterceptorContext>(relaxed = true)
        val inputStreamSlot = slot<InputStream>()
        var capturedInputStream: String? = null

        every { context.mediaType } returns MediaType.APPLICATION_JSON_TYPE
        every { context.inputStream = capture(inputStreamSlot) } answers {
            capturedInputStream = inputStreamSlot.captured.bufferedReader(StandardCharsets.UTF_8).readText()
        }
        every { context.proceed() } returns Unit

        When("the JSON contains explicit null values in an object") {
            val jsonWithNulls = """{"fakeKey1":"fakeValue1","fakeKey2":null,"fakeKey3":"fakeValue3"}"""
            every { context.inputStream } returns ByteArrayInputStream(jsonWithNulls.toByteArray(StandardCharsets.UTF_8))

            interceptor.aroundReadFrom(context)

            Then("the null values should be removed") {
                capturedInputStream shouldNotContain "null"
                capturedInputStream shouldContain "\"fakeKey1\":\"fakeValue1\""
                capturedInputStream shouldContain "\"fakeKey3\":\"fakeValue3\""
                capturedInputStream shouldNotContain "\"fakeKey2\""
            }
        }

        When("the JSON contains nested objects with null values") {
            val jsonWithNestedNulls = """{"fakeKey1":{"fakeKey2":"fakeValue2","fakeKey3":null},"fakeKey4":true}"""
            every { context.inputStream } returns ByteArrayInputStream(jsonWithNestedNulls.toByteArray(StandardCharsets.UTF_8))

            interceptor.aroundReadFrom(context)

            Then("the nested null values should be removed") {
                capturedInputStream shouldNotContain "null"
                capturedInputStream shouldContain "\"fakeKey2\":\"fakeValue2\""
                capturedInputStream shouldNotContain "\"fakeKey3\""
            }
        }

        When("the JSON contains arrays with null values") {
            val jsonWithArrayNulls = """{"fakeArray":[1,null,3],"fakeKey1":"fakeValue1"}"""
            every { context.inputStream } returns ByteArrayInputStream(jsonWithArrayNulls.toByteArray(StandardCharsets.UTF_8))

            interceptor.aroundReadFrom(context)

            Then("the null values in arrays should be removed") {
                capturedInputStream shouldNotContain "null"
                capturedInputStream shouldContain "\"fakeArray\":[1,3]"
            }
        }
    }

    Given("a non-JSON request") {
        val context = mockk<ReaderInterceptorContext>(relaxed = true)
        var proceedCalled = false

        every { context.mediaType } returns MediaType.TEXT_PLAIN_TYPE
        every { context.proceed() } answers { proceedCalled = true }

        When("the interceptor processes the request") {
            interceptor.aroundReadFrom(context)

            Then("it should proceed without modification") {
                proceedCalled shouldBe true
            }
        }
    }

    Given("a request with null media type") {
        val context = mockk<ReaderInterceptorContext>(relaxed = true)
        var proceedCalled = false

        every { context.mediaType } returns null
        every { context.proceed() } answers { proceedCalled = true }

        When("the interceptor processes the request") {
            interceptor.aroundReadFrom(context)

            Then("it should proceed without modification") {
                proceedCalled shouldBe true
            }
        }
    }

    Given("a JSON request with empty body") {
        val context = mockk<ReaderInterceptorContext>(relaxed = true)
        var proceedCalled = false

        every { context.mediaType } returns MediaType.APPLICATION_JSON_TYPE
        every { context.inputStream } returns ByteArrayInputStream("".toByteArray(StandardCharsets.UTF_8))
        every { context.proceed() } answers { proceedCalled = true }

        When("the interceptor processes the request") {
            interceptor.aroundReadFrom(context)

            Then("it should proceed without modification") {
                proceedCalled shouldBe true
            }
        }
    }

    Given("a JSON request with invalid JSON") {
        val context = mockk<ReaderInterceptorContext>(relaxed = true)
        val inputStreamSlot = slot<InputStream>()
        var capturedInputStream: String? = null

        every { context.mediaType } returns MediaType.APPLICATION_JSON_TYPE
        every { context.inputStream } returns ByteArrayInputStream("invalid json{".toByteArray(StandardCharsets.UTF_8))
        every { context.inputStream = capture(inputStreamSlot) } answers {
            capturedInputStream = inputStreamSlot.captured.bufferedReader(StandardCharsets.UTF_8).readText()
        }
        every { context.proceed() } returns Unit

        When("the interceptor processes the request") {
            interceptor.aroundReadFrom(context)

            Then("it should restore the original input and proceed") {
                capturedInputStream shouldBe "invalid json{"
            }
        }
    }
})
