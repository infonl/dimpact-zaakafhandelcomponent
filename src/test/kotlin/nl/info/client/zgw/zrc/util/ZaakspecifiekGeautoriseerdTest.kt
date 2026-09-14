/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.exception.EigenschapNotFoundException
import nl.info.client.zgw.ztc.model.createEigenschap

class ZaakspecifiekGeautoriseerdTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()

    afterEach { checkUnnecessaryStub() }

    given("a zaak of a zaakspecifiek autoriseerbaar zaaktype that is not yet marked") {
        val zaak = createZaak()
        val eigenschap = createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
        val zaakEigenschapSlot = slot<ZaakEigenschap>()

        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
        every {
            ztcClientService.readEigenschap(zaak.zaaktype, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
        } returns eigenschap
        every {
            zrcClientService.createEigenschap(zaak.uuid, capture(zaakEigenschapSlot))
        } returns createZaakEigenschap()

        `when`("the zaak is marked as zaakspecifiek geautoriseerd") {
            zrcClientService.markZaakspecifiekGeautoriseerd(zaak, ztcClientService)

            then("a zaakeigenschap referring to the zaaktype's own eigenschap is created with value 'true'") {
                with(zaakEigenschapSlot.captured) {
                    this.eigenschap shouldBe eigenschap.url
                    this.zaak shouldBe zaak.url
                    waarde shouldBe "true"
                }
            }
        }
    }

    given("a zaak that is already marked as zaakspecifiek geautoriseerd") {
        val zaak = createZaak()

        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
            createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
        )

        `when`("the zaak is marked as zaakspecifiek geautoriseerd again") {
            zrcClientService.markZaakspecifiekGeautoriseerd(zaak, ztcClientService)

            then("no second zaakeigenschap is created") {
                verify(exactly = 0) { zrcClientService.createEigenschap(any(), any()) }
            }
        }
    }

    given("a zaak whose zaaktype does not define the zaakspecifiek geautoriseerd eigenschap") {
        val zaak = createZaak()

        every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
        every {
            ztcClientService.readEigenschap(zaak.zaaktype, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
        } throws EigenschapNotFoundException("fakeMessage")

        `when`("the zaak is marked as zaakspecifiek geautoriseerd") {
            val exception = shouldThrow<EigenschapNotFoundException> {
                zrcClientService.markZaakspecifiekGeautoriseerd(zaak, ztcClientService)
            }

            then("the marking fails and nothing is recorded in the zaakregister") {
                exception.message shouldBe "fakeMessage"
                verify(exactly = 0) { zrcClientService.createEigenschap(any(), any()) }
            }
        }
    }
})
