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

    context("AllowedFileType.fromFilename") {
        given("a filename with an allowed extension") {
            `when`("fromFilename is called") {
                then("the matching enum entry is returned") {
                    AllowedFileType.fromFilename("fakeReport.pdf") shouldBe AllowedFileType.PDF
                    AllowedFileType.fromFilename("fakePhoto.JPG") shouldBe AllowedFileType.JPG
                    AllowedFileType.fromFilename("fakeMovie.MP4") shouldBe AllowedFileType.MP4
                }
            }
        }

        given("a filename with a disallowed extension") {
            `when`("fromFilename is called") {
                then("null is returned") {
                    AllowedFileType.fromFilename("fakeMalware.exe") shouldBe null
                    AllowedFileType.fromFilename("fakeArchive.zip") shouldBe null
                }
            }
        }

        given("a filename without an extension") {
            `when`("fromFilename is called") {
                then("null is returned") {
                    AllowedFileType.fromFilename("fakeReadme") shouldBe null
                }
            }
        }

        given("a null or blank filename") {
            `when`("fromFilename is called") {
                then("null is returned") {
                    AllowedFileType.fromFilename(null) shouldBe null
                    AllowedFileType.fromFilename("") shouldBe null
                    AllowedFileType.fromFilename("   ") shouldBe null
                }
            }
        }
    }

    context("AllowedFileType.isAllowed") {
        given("a filename with an allowed extension") {
            `when`("isAllowed is called") {
                then("it returns true (case-insensitive on extension)") {
                    AllowedFileType.isAllowed("fakeReport.pdf") shouldBe true
                    AllowedFileType.isAllowed("fakeReport.PDF") shouldBe true
                    AllowedFileType.isAllowed("fakeMovie.mkv") shouldBe true
                }
            }
        }

        given("a filename with a disallowed extension") {
            `when`("isAllowed is called") {
                then("it returns false") {
                    AllowedFileType.isAllowed("fakeMalware.exe") shouldBe false
                    AllowedFileType.isAllowed("fakeArchive.zip") shouldBe false
                }
            }
        }

        given("a null, blank or extensionless filename") {
            `when`("isAllowed is called") {
                then("it returns false") {
                    AllowedFileType.isAllowed(null) shouldBe false
                    AllowedFileType.isAllowed("") shouldBe false
                    AllowedFileType.isAllowed("fakeReadme") shouldBe false
                }
            }
        }
    }
})
