/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.InputStream

private class ClosableInputStream(bytes: ByteArray) : InputStream() {
    private val delegate = ByteArrayInputStream(bytes)
    var isClosed = false
        private set

    override fun read() = delegate.read()

    override fun read(buffer: ByteArray, offset: Int, length: Int) = delegate.read(buffer, offset, length)

    override fun close() {
        isClosed = true
    }
}

class BestandsDeelInputStreamTest : BehaviorSpec({
    context("reading one bestandsdeel from the content of a document") {
        given("content that holds more bytes than the bestandsdeel") {
            val content = ClosableInputStream("0123456789".toByteArray())
            val bestandsDeelInputStream = BestandsDeelInputStream(content = content, sizeInBytes = 4)

            `when`("it is read to the end") {
                val bytes = bestandsDeelInputStream.readBytes()

                then("it yields only the bytes of the bestandsdeel") {
                    String(bytes) shouldBe "0123"
                    bestandsDeelInputStream.bytesRead shouldBe 4
                }

                and("the bytes that follow are left for the next bestandsdeel") {
                    String(BestandsDeelInputStream(content = content, sizeInBytes = 6).readBytes()) shouldBe "456789"
                }
            }
        }

        given("content that runs out before the bestandsdeel is complete") {
            val content = ClosableInputStream("0123".toByteArray())
            val bestandsDeelInputStream = BestandsDeelInputStream(content = content, sizeInBytes = 10)

            `when`("it is read to the end") {
                val bytes = bestandsDeelInputStream.readBytes()

                then("it reports how many bytes it could yield, so that the caller can refuse the part") {
                    String(bytes) shouldBe "0123"
                    bestandsDeelInputStream.bytesRead shouldBe 4
                }
            }
        }

        given("a bestandsdeel that has been consumed") {
            val content = ClosableInputStream("0123456789".toByteArray())
            val bestandsDeelInputStream = BestandsDeelInputStream(content = content, sizeInBytes = 4)
            bestandsDeelInputStream.readBytes()

            `when`("it is closed") {
                bestandsDeelInputStream.close()

                then("the content is left open, because the bestandsdelen that follow are read from it") {
                    content.isClosed shouldBe false
                }
            }
        }
    }
})
