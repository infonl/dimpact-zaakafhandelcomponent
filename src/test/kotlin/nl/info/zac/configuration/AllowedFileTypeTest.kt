/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub

class AllowedFileTypeTest : BehaviorSpec({
    beforeEach { checkUnnecessaryStub() }

    Context("AllowedFileType.fromFilename") {
        Given("a filename with an allowed extension") {
            When("fromFilename is called") {
                Then("the matching enum entry is returned") {
                    AllowedFileType.fromFilename("fakeReport.pdf") shouldBe AllowedFileType.PDF
                    AllowedFileType.fromFilename("fakePhoto.JPG") shouldBe AllowedFileType.JPG
                    AllowedFileType.fromFilename("fakeMovie.MP4") shouldBe AllowedFileType.MP4
                }
            }
        }

        Given("a filename with a disallowed extension") {
            When("fromFilename is called") {
                Then("null is returned") {
                    AllowedFileType.fromFilename("fakeMalware.exe") shouldBe null
                    AllowedFileType.fromFilename("fakeArchive.zip") shouldBe null
                }
            }
        }

        Given("a filename without an extension") {
            When("fromFilename is called") {
                Then("null is returned") {
                    AllowedFileType.fromFilename("fakeReadme") shouldBe null
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
    }

    Context("AllowedFileType.isAllowed") {
        Given("a filename with an allowed extension") {
            When("isAllowed is called") {
                Then("it returns true (case-insensitive on extension)") {
                    AllowedFileType.isAllowed("fakeReport.pdf") shouldBe true
                    AllowedFileType.isAllowed("fakeReport.PDF") shouldBe true
                    AllowedFileType.isAllowed("fakeMovie.mkv") shouldBe true
                }
            }
        }

        Given("a filename with a disallowed extension") {
            When("isAllowed is called") {
                Then("it returns false") {
                    AllowedFileType.isAllowed("fakeMalware.exe") shouldBe false
                    AllowedFileType.isAllowed("fakeArchive.zip") shouldBe false
                }
            }
        }

        Given("a null, blank or extensionless filename") {
            When("isAllowed is called") {
                Then("it returns false") {
                    AllowedFileType.isAllowed(null) shouldBe false
                    AllowedFileType.isAllowed("") shouldBe false
                    AllowedFileType.isAllowed("fakeReadme") shouldBe false
                }
            }
        }
    }
})
