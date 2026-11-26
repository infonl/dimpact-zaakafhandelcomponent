/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.zac.time.ZacTime
import java.net.URI
import java.util.UUID

class ZaakUtilTest : BehaviorSpec({
    context("Whether a zaak is open") {
        given("a zaak without an einddatum") {
            val zaak = createZaak(endDate = null, archiefnominatie = null)

            `when`("it is checked whether the zaak is open") {
                val isOpen = zaak.isOpen()

                then("the zaak is open") {
                    isOpen shouldBe true
                }
            }
        }

        given("a zaak with an einddatum and an archiefnominatie") {
            val zaak = createZaak(endDate = ZacTime.today(), archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN)

            `when`("it is checked whether the zaak is open") {
                val isOpen = zaak.isOpen()

                then("the zaak is afgehandeld") {
                    isOpen shouldBe false
                }
            }
        }

        given(
            """
            a deelzaak with an einddatum but without an archiefnominatie, because Open Zaak derives its archiving
            from the hoofdzaak that is still open
            """
        ) {
            val deelzaak = createZaak(
                endDate = ZacTime.today(),
                archiefnominatie = null,
                hoofdzaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
            )

            `when`("it is checked whether the deelzaak is open") {
                val isOpen = deelzaak.isOpen()

                then("the deelzaak is afgehandeld, because archiving is a separate process") {
                    isOpen shouldBe false
                }
            }
        }

        given("a zaak without an einddatum that already has an archiefnominatie") {
            val zaak = createZaak(endDate = null, archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN)

            `when`("it is checked whether the zaak is open") {
                val isOpen = zaak.isOpen()

                then("the zaak is open, because only the eindstatus ends a zaak") {
                    isOpen shouldBe true
                }
            }
        }
    }
})
