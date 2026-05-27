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

    Given("a REST enkelvoudig informatie object with an allowed extension and matching media type") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "fakeFileName.txt"
                formaat = "text/plain"
            }

        When("no file content provided") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it detects it as invalid") {
                result shouldBe false
            }
        }

        When("file is empty") {
            restEnkelvoudigInformatieobject.file = "".toByteArray()

            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it detects it as invalid") {
                result shouldBe false
            }
        }

        When("file content is provided") {
            restEnkelvoudigInformatieobject.file = "fake content".toByteArray()

            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it detects it as valid") {
                result shouldBe true
            }
        }
    }

    Given("a metadata-only update without a filename") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()

        When("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it is accepted because no upload is happening") {
                result shouldBe true
            }
        }
    }

    Given("a REST enkelvoudig informatie object with a disallowed extension") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "malicious.exe"
                formaat = "application/x-msdownload"
                file = "MZ".toByteArray()
            }

        When("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it is rejected") {
                result shouldBe false
            }
        }
    }

    Given("a REST enkelvoudig informatie object whose extension and media type disagree") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "document.pdf"
                formaat = "image/png"
                file = "fake content".toByteArray()
            }

        When("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it is rejected because the declared media type does not match the extension") {
                result shouldBe false
            }
        }
    }

    Given("a REST enkelvoudig informatie object with an allowed extension but no media type") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "report.PDF"
                formaat = null
                file = "fake content".toByteArray()
            }

        When("validated") {
            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it is accepted (the extension check is the security gate; media type is optional)") {
                result shouldBe true
            }
        }
    }
})
