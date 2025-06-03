/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import nl.info.client.zgw.model.createMedewerker
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolMedewerkerForReads
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolOrganisatorischeEenheidForReads
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.zac.configuratie.ConfiguratieService
import java.util.UUID

class ZrcClientServiceTest : BehaviorSpec({
    val zrcClient = mockk<ZrcClient>()
    val zgwClientHeadersFactory = mockk<ZGWClientHeadersFactory>()
    val configuratieService = mockk<ConfiguratieService>()
    val zrcClientService = ZrcClientService(
        zrcClient = zrcClient,
        zgwClientHeadersFactory = zgwClientHeadersFactory,
        configuratieService = configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("An existing zaak") {
        val zaakUUID = UUID.randomUUID()
        val expectedZaak = createZaak(uuid = zaakUUID)

        every { zrcClient.zaakRead(zaakUUID) } returns expectedZaak

        `when`("readZaak is called") {
            val result = zrcClientService.readZaak(zaakUUID)

            then("it should return the corresponding zaak") {
                result shouldBe expectedZaak
            }
        }
    }

    Given("A zaak and a new rol to be added") {
        val zaak = createZaak()
        val existingRoles = listOf(createRolMedewerker(), createRolOrganisatorischeEenheid())
        val newRole = createRolMedewerker(
            betrokkeneIdentificatie = createMedewerker(identificatie = "fakeIdentificatie123")
        )
        val description = "fakeDescription"
        every { zrcClient.rolList(any()) } returns Results(existingRoles, existingRoles.size)
        every { zgwClientHeadersFactory.setAuditToelichting(description) } just Runs
        every { zrcClient.rolCreate(any()) } returns newRole

        When("updateRol is called") {
            zrcClientService.updateRol(zaak, newRole, description)

            Then("it should create the new role and set the audit description") {
                verify(exactly = 1) {
                    zgwClientHeadersFactory.setAuditToelichting(description)
                    zrcClient.rolCreate(newRole)
                }
            }
        }
    }

    Given("A zaak with existing roles") {
        val zaak = createZaak()
        val medewerkerRole1 = createRolMedewerkerForReads()
        val medewerkerRole2 = createRolMedewerkerForReads()
        val organisatorischeEenheidRol = createRolOrganisatorischeEenheidForReads()
        val existingRoles = listOf(medewerkerRole1, medewerkerRole2, organisatorischeEenheidRol)
        val description = "fakeDescription"
        every { zrcClient.rolList(any()) } returns Results(existingRoles, existingRoles.size)
        every { zrcClient.rolDelete(any()) } just Runs
        every { zgwClientHeadersFactory.setAuditToelichting(description) } just Runs

        When("deleteRol is called for betrokkeneType 'Medewerker'") {
            zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, description)

            Then("it should remove only the first role of the matching betrokkene type") {
                verify(exactly = 1) {
                    zrcClient.rolDelete(medewerkerRole1.uuid)
                }
                verify(exactly = 0) {
                    zrcClient.rolDelete(medewerkerRole2.uuid)
                    zrcClient.rolDelete(organisatorischeEenheidRol.uuid)
                }
            }
        }
    }
})
