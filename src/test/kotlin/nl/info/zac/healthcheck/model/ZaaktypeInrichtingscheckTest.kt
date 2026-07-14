/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck

class ZaaktypeInrichtingscheckTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    given("zaak type without one required status") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false
        )

        `when`("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            then("it is reported as invalid") {
                validity shouldBe false
            }
        }
    }

    given("zaak type without two required status") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false,
            statustypeHeropendAanwezig = false
        )

        `when`("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            then("it is reported as invalid") {
                validity shouldBe false
            }
        }
    }

    given("zaak type with all required statuses present") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()

        `when`("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            then("it is reported as valid") {
                validity shouldBe true
            }
        }
    }
})
