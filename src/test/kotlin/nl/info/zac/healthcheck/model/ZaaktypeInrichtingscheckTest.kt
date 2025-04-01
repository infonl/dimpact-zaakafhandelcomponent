/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.healthcheck.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck

class ZaaktypeInrichtingscheckTest : BehaviorSpec({

    Given("zaak type without one required status") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false
        )

        When("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            Then("it is reported as invalid") {
                validity shouldBe false
            }
        }
    }

    Given("zaak type without two required status") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false,
            statustypeHeropendAanwezig = false
        )

        When("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            Then("it is reported as invalid") {
                validity shouldBe false
            }
        }
    }

    Given("zaak type with all required statuses present") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()

        When("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            Then("it is reported as valid") {
                validity shouldBe true
            }
        }
    }
})
