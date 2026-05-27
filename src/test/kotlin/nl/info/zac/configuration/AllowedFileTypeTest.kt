/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AllowedFileTypeTest : BehaviorSpec({

    Given("a filename with an allowed extension") {
        When("fromFilename is called") {
            Then("the matching enum entry is returned") {
                AllowedFileType.fromFilename("report.pdf") shouldBe AllowedFileType.PDF
                AllowedFileType.fromFilename("photo.JPG") shouldBe AllowedFileType.JPG
                AllowedFileType.fromFilename("movie.MP4") shouldBe AllowedFileType.MP4
            }
        }
    }

    Given("a filename with a disallowed extension") {
        When("fromFilename is called") {
            Then("null is returned") {
                AllowedFileType.fromFilename("malware.exe") shouldBe null
                AllowedFileType.fromFilename("archive.zip") shouldBe null
            }
        }
    }

    Given("a filename without an extension") {
        When("fromFilename is called") {
            Then("null is returned") {
                AllowedFileType.fromFilename("README") shouldBe null
            }
        }
    }

    Given("a null or blank filename") {
        When("fromFilename is called") {
            Then("null is returned") {
                AllowedFileType.fromFilename(null) shouldBe null
                AllowedFileType.fromFilename("") shouldBe null
                AllowedFileType.fromFilename("   ") shouldBe null
            }
        }
    }

    Given("an allowed extension and matching media type") {
        When("isAllowed is called") {
            Then("it returns true (case-insensitive on media type)") {
                AllowedFileType.isAllowed("report.pdf", "application/pdf") shouldBe true
                AllowedFileType.isAllowed("report.pdf", "APPLICATION/PDF") shouldBe true
            }
        }
    }

    Given("an allowed extension but a mismatching media type") {
        When("isAllowed is called") {
            Then("it returns false") {
                AllowedFileType.isAllowed("document.pdf", "image/png") shouldBe false
            }
        }
    }

    Given("an allowed extension and a missing/blank media type") {
        When("isAllowed is called") {
            Then("it returns true (extension is the security gate, media type is optional)") {
                AllowedFileType.isAllowed("document.pdf", null) shouldBe true
                AllowedFileType.isAllowed("document.pdf", "") shouldBe true
            }
        }
    }

    Given("a disallowed extension") {
        When("isAllowed is called") {
            Then("it returns false regardless of media type") {
                AllowedFileType.isAllowed("malware.exe", "application/x-msdownload") shouldBe false
                AllowedFileType.isAllowed("malware.exe", null) shouldBe false
            }
        }
    }
})
