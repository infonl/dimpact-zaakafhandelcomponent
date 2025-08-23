package nl.info.client.zgw.ztc.model.extensions

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.ztc.model.createZaakType

class ZaakTypeExtensionsTest : BehaviorSpec({

    Context("isServicenormBeschikbaar") {
        Given("servicenorm was never set") {
            val zaakType = createZaakType(servicenorm = null)

            When("calling isServicenormBeschikbaar") {
                val result = zaakType.isServicenormBeschikbaar()

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("servicenorm is not set") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W0D")

            When("calling isServicenormBeschikbaar") {
                val result = zaakType.isServicenormBeschikbaar()

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("servicenorm is set") {
            val zaakType = createZaakType(servicenorm = "P0Y0M0W30D")

            When("calling isServicenormBeschikbaar") {
                val result = zaakType.isServicenormBeschikbaar()

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }
    }
})
