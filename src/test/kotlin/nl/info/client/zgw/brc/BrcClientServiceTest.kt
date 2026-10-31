/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.brc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.model.createZaak
import java.net.URI
import java.util.UUID

class BrcClientServiceTest : BehaviorSpec({
    val brcClient: BrcClient = mockk<BrcClient>()
    val zgwClientHeadersFactory: ZGWClientHeadersFactory = mockk<ZGWClientHeadersFactory>()
    val brcClientService = BrcClientService(
        brcClient = brcClient,
        zgwClientHeadersFactory = zgwClientHeadersFactory
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaak and two besluiten") {
        val zaak = createZaak()
        val besluiten = listOf(createBesluit(), createBesluit())
        val besluitResults = Results(besluiten, 1)
        every { brcClient.besluitList(any()) } returns besluitResults

        When("list besluiten is called") {
            val returnedBesluiten = brcClientService.listBesluiten(zaak)

            Then("it should return the list of besluiten") {
                returnedBesluiten shouldBe besluiten
            }
        }
    }
    Given("An existing besluit") {
        val besluitUuid = UUID.randomUUID()
        val besluit = createBesluit(
            url = URI("http://localhost/besluit/$besluitUuid")
        )
        val updateReason = "dummyReason"
        val returnedBesluit = createBesluit()
        every { zgwClientHeadersFactory.setAuditToelichting(updateReason) } just Runs
        every { brcClient.besluitUpdate(besluitUuid, besluit) } returns returnedBesluit

        When("update besluit is called with a reason string") {
            val updatedBesluit = brcClientService.updateBesluit(besluit, updateReason)

            Then("the update reason should added as a HTTP header and the besluit should be updated") {
                updatedBesluit shouldBe returnedBesluit
            }
        }
    }
})
