/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model.validation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.app.informatieobjecten.model.RestFileUpload

class ValidRestFileUploadFormValidatorTest : BehaviorSpec({

    val validator = ValidRestFileUploadFormValidator()

    Given("a task file upload with an allowed extension and matching media type") {
        val upload = RestFileUpload(
            file = "fake content".toByteArray(),
            fileSize = 12,
            filename = "report.pdf",
            type = "application/pdf"
        )

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is accepted") {
                result shouldBe true
            }
        }
    }

    Given("a task file upload with a disallowed extension") {
        val upload = RestFileUpload(
            file = "MZ".toByteArray(),
            fileSize = 2,
            filename = "malware.exe",
            type = "application/x-msdownload"
        )

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is rejected") {
                result shouldBe false
            }
        }
    }

    Given("a task file upload with no filename") {
        val upload = RestFileUpload()

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is accepted because no actual upload is happening") {
                result shouldBe true
            }
        }
    }

    Given("a task file upload with a filename but no file content") {
        val upload = RestFileUpload(
            file = null,
            fileSize = 0,
            filename = "report.pdf",
            type = "application/pdf"
        )

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is rejected") {
                result shouldBe false
            }
        }
    }

    Given("a task file upload with file bytes but no filename") {
        val upload = RestFileUpload(
            file = "fake content".toByteArray(),
            fileSize = 12,
            filename = null,
            type = "application/pdf"
        )

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is rejected because the allowlist can only be applied with a filename") {
                result shouldBe false
            }
        }
    }

    Given("a task file upload with file bytes but a blank filename") {
        val upload = RestFileUpload(
            file = "fake content".toByteArray(),
            fileSize = 12,
            filename = "   ",
            type = "application/pdf"
        )

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is rejected") {
                result shouldBe false
            }
        }
    }

    Given("a task file upload with a filename but an empty file byte array") {
        val upload = RestFileUpload(
            file = ByteArray(0),
            fileSize = 0,
            filename = "report.pdf",
            type = "application/pdf"
        )

        When("validated") {
            val result = validator.isValid(upload, null)

            Then("it is rejected") {
                result shouldBe false
            }
        }
    }
})
