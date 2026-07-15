/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model.validation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject

class ValidRestEnkelvoudigInformatieobjectFileUploadFormValidatorTest : BehaviorSpec({

    val validator =
        ValidRestEnkelvoudigInformatieFileUploadFormValidator()

    given("a REST enkelvoudig informatie object with an allowed extension") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "fakeFileName.txt"
                formaat = "text/plain"
            }

        `when`("no file content provided") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it detects it as invalid") {
                result shouldBe false
            }
        }

        `when`("file is empty") {
            restEnkelvoudigInformatieobject.file = "".toByteArray()

            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it detects it as invalid") {
                result shouldBe false
            }
        }

        `when`("file content is provided") {
            restEnkelvoudigInformatieobject.file = "fake content".toByteArray()

            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it detects it as valid") {
                result shouldBe true
            }
        }
    }

    given("a REST enkelvoudig informatie object with file bytes but no bestandsnaam") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = null
                formaat = "application/pdf"
                file = "fake content".toByteArray()
            }

        `when`("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it is rejected because the allowlist can only be applied with a bestandsnaam") {
                result shouldBe false
            }
        }
    }

    given("a REST enkelvoudig informatie object with file bytes but a blank bestandsnaam") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "   "
                formaat = "application/pdf"
                file = "fake content".toByteArray()
            }

        `when`("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it is rejected") {
                result shouldBe false
            }
        }
    }

    given("a metadata-only update without a filename") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()

        `when`("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it is accepted because no upload is happening") {
                result shouldBe true
            }
        }
    }

    given("a REST enkelvoudig informatie object with a disallowed extension") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "malicious.exe"
                formaat = "application/x-msdownload"
                file = "MZ".toByteArray()
            }

        `when`("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it is rejected") {
                result shouldBe false
            }
        }
    }

    given("a REST enkelvoudig informatie object whose extension and media type disagree") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "document.pdf"
                formaat = "image/png"
                file = "fake content".toByteArray()
            }

        `when`("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it is accepted because only the extension is validated; the media type is OS-dependent") {
                result shouldBe true
            }
        }
    }

    given("a REST enkelvoudig informatie object with an allowed extension but no media type") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "report.PDF"
                formaat = null
                file = "fake content".toByteArray()
            }

        `when`("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            then("it is accepted because the extension check is the security gate") {
                result shouldBe true
            }
        }
    }
})
