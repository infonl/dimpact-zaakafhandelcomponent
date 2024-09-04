package net.atos.zac.app.informatieobjecten.model.validation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject

class ValidRestEnkelvoudigInformatieobjectFileUploadFormValidatorTest : BehaviorSpec({

    val validator =
        ValidRestEnkelvoudigInformatieFileUploadFormValidator()

    Given("REST enkelvoudig informatie object") {
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject()
            .apply {
                bestandsnaam = "dummyFileName.txt"
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

        When("file is provided") {
            restEnkelvoudigInformatieobject.file = "dummy content".toByteArray()

            val result = validator.isValid(restEnkelvoudigInformatieobject, null)

            Then("it detects it as valid") {
                result shouldBe true
            }
        }
    }
})
