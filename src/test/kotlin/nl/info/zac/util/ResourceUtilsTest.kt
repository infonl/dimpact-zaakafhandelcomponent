/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.InputStream

class ResourceUtilsTest : BehaviorSpec({
    fun classLoaderWithResource(path: String, content: String) = object : ClassLoader() {
        override fun getResourceAsStream(name: String): InputStream? =
            if (name == path) content.byteInputStream() else null
    }

    context("Reading the non-blank lines of a resource") {
        given("a resource with blank lines between its lines") {
            val classLoader = classLoaderWithResource("fakeFolder/fakeFile", "first\n\n  \nsecond\n")

            `when`("the non-blank lines are read") {
                val lines = classLoader.readNonBlankResourceLines("fakeFolder/fakeFile")

                then("only the non-blank lines are returned") {
                    lines shouldBe listOf("first", "second")
                }
            }
        }

        given("a resource that does not exist") {
            val classLoader = classLoaderWithResource("fakeFolder/fakeFile", "first")

            `when`("the non-blank lines are read") {
                val exception = shouldThrow<IllegalStateException> {
                    classLoader.readNonBlankResourceLines("fakeFolder/fakeOtherFile")
                }

                then("an exception names the missing resource") {
                    exception.message shouldBe "Resource 'fakeFolder/fakeOtherFile' not found"
                }
            }
        }
    }

    context("Reading the bytes of a resource") {
        given("a resource with content") {
            val classLoader = classLoaderWithResource("fakeFolder/fakeFile", "fakeContent")

            `when`("the bytes are read") {
                val bytes = classLoader.readResourceBytes("fakeFolder/fakeFile")

                then("the content of the resource is returned") {
                    bytes.decodeToString() shouldBe "fakeContent"
                }
            }
        }

        given("a resource that does not exist") {
            val classLoader = classLoaderWithResource("fakeFolder/fakeFile", "fakeContent")

            `when`("the bytes are read") {
                val exception = shouldThrow<IllegalStateException> {
                    classLoader.readResourceBytes("fakeFolder/fakeOtherFile")
                }

                then("an exception names the missing resource") {
                    exception.message shouldBe "Resource 'fakeFolder/fakeOtherFile' not found"
                }
            }
        }
    }
})
