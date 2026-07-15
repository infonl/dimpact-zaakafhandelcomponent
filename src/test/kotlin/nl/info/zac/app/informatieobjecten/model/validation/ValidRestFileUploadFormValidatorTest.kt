/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model.validation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.app.informatieobjecten.model.RestFileUpload

class ValidRestFileUploadFormValidatorTest : BehaviorSpec({
    beforeEach { checkUnnecessaryStub() }

    val validator = ValidRestFileUploadFormValidator()

    context("ValidRestFileUploadFormValidator.isValid") {
        given("a task file upload with an allowed extension") {
            val upload = RestFileUpload(
                file = "fake content".toByteArray(),
                fileSize = 12,
                filename = "fakeReport.pdf",
                type = "application/pdf"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is accepted") {
                    result shouldBe true
                }
            }
        }

        given("a task file upload with an allowed extension and an OS-specific media type") {
            val upload = RestFileUpload(
                file = "fake content".toByteArray(),
                fileSize = 12,
                filename = "fakeMovie.mkv",
                type = "video/webm"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is accepted because only the extension is validated") {
                    result shouldBe true
                }
            }
        }

        given("a task file upload with a disallowed extension") {
            val upload = RestFileUpload(
                file = "MZ".toByteArray(),
                fileSize = 2,
                filename = "fakeMalware.exe",
                type = "application/x-msdownload"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is rejected") {
                    result shouldBe false
                }
            }
        }

        given("a task file upload with no filename") {
            val upload = RestFileUpload()

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is accepted because no actual upload is happening") {
                    result shouldBe true
                }
            }
        }

        given("a task file upload with a filename but no file content") {
            val upload = RestFileUpload(
                file = null,
                fileSize = 0,
                filename = "fakeReport.pdf",
                type = "application/pdf"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is rejected") {
                    result shouldBe false
                }
            }
        }

        given("a task file upload with file bytes but no filename") {
            val upload = RestFileUpload(
                file = "fake content".toByteArray(),
                fileSize = 12,
                filename = null,
                type = "application/pdf"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is rejected because the allowlist can only be applied with a filename") {
                    result shouldBe false
                }
            }
        }

        given("a task file upload with file bytes but a blank filename") {
            val upload = RestFileUpload(
                file = "fake content".toByteArray(),
                fileSize = 12,
                filename = "   ",
                type = "application/pdf"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is rejected") {
                    result shouldBe false
                }
            }
        }

        given("a task file upload with a filename but an empty file byte array") {
            val upload = RestFileUpload(
                file = ByteArray(0),
                fileSize = 0,
                filename = "fakeReport.pdf",
                type = "application/pdf"
            )

            `when`("validated") {
                val result = validator.isValid(upload, null)

                then("it is rejected") {
                    result shouldBe false
                }
            }
        }
    }
})
