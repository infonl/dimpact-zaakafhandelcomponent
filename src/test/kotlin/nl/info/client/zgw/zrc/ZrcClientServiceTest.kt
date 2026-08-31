/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolMedewerkerForReads
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createRolOrganisatorischeEenheidForReads
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.model.createZaakobjectPand
import nl.info.client.zgw.util.ZgwClientHeadersFactory
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.zac.configuration.ConfigurationService
import java.util.UUID

class ZrcClientServiceTest : BehaviorSpec({
    val zrcClient = mockk<ZrcClient>()
    val zgwClientHeadersFactory = mockk<ZgwClientHeadersFactory>()
    val configurationService = mockk<ConfigurationService>()
    val zrcClientService = ZrcClientService(
        zrcClient = zrcClient,
        zgwClientHeadersFactory = zgwClientHeadersFactory,
        configurationService = configurationService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("An existing zaak") {
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

    given("A zaak and a new rol to be added") {
        val zaak = createZaak()
        val existingRoles = listOf(createRolMedewerker(), createRolOrganisatorischeEenheid())
        val newRole = createRolMedewerker(
            medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeIdentificatie123")
        )
        val auditExplanation = "fakeExplanation"
        every { zrcClient.rolList(any()) } returns Results(existingRoles, existingRoles.size)
        every { zgwClientHeadersFactory.setAuditExplanation(auditExplanation) } just Runs
        every { zrcClient.rolCreate(any()) } returns newRole

        `when`("updateRol is called") {
            zrcClientService.updateRol(zaak, newRole, auditExplanation)

            then("it should create the new role and set the audit description") {
                verify(exactly = 1) {
                    zgwClientHeadersFactory.setAuditExplanation(auditExplanation)
                    zrcClient.rolCreate(newRole)
                }
            }
        }
    }

    given("A zaak with existing roles") {
        val zaak = createZaak()
        val medewerkerRole1 = createRolMedewerkerForReads()
        val medewerkerRole2 = createRolMedewerkerForReads()
        val organisatorischeEenheidRol = createRolOrganisatorischeEenheidForReads()
        val existingRoles = listOf(medewerkerRole1, medewerkerRole2, organisatorischeEenheidRol)
        val description = "fakeDescription"
        every { zrcClient.rolList(any()) } returns Results(existingRoles, existingRoles.size)
        every { zrcClient.rolDelete(any()) } just Runs
        every { zgwClientHeadersFactory.setAuditExplanation(description) } just Runs

        `when`("deleteRol is called for betrokkeneType 'Medewerker'") {
            zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, description)

            then("it should remove only the first role of the matching betrokkene type") {
                verify(exactly = 1) {
                    zrcClient.rolDelete(medewerkerRole1.uuid!!)
                }
                verify(exactly = 0) {
                    zrcClient.rolDelete(medewerkerRole2.uuid!!)
                    zrcClient.rolDelete(organisatorischeEenheidRol.uuid!!)
                }
            }
        }
    }

    given("A rol to be deleted directly") {
        val rol = createRolMedewerkerForReads()
        val auditExplanation = "fakeExplanation"
        every { zrcClient.rolDelete(rol.uuid!!) } just Runs
        every { zgwClientHeadersFactory.setAuditExplanation(auditExplanation) } just Runs

        `when`("deleteRol is called with the rol directly") {
            zrcClientService.deleteRol(rol, auditExplanation)

            then("it should delete the rol and set the audit description") {
                verify(exactly = 1) {
                    zgwClientHeadersFactory.setAuditExplanation(auditExplanation)
                    zrcClient.rolDelete(rol.uuid!!)
                }
            }
        }
    }

    given("A zaakobject to be deleted") {
        val zaakobject = createZaakobjectPand()
        val toelichting = "fakeToelichting"
        every { zrcClient.zaakobjectDelete(zaakobject.uuid) } just Runs
        every { zgwClientHeadersFactory.setAuditExplanation(toelichting) } just Runs

        `when`("deleteZaakobject is called") {
            zrcClientService.deleteZaakobject(zaakobject, toelichting)

            then("it should delete the zaakobject and set the audit description") {
                verify(exactly = 1) {
                    zgwClientHeadersFactory.setAuditExplanation(toelichting)
                    zrcClient.zaakobjectDelete(zaakobject.uuid)
                }
            }
        }
    }

    given("An informatieobject that is not yet linked to any zaak") {
        val informatieobject = createEnkelvoudigInformatieObject()
        val targetZaak = createZaak()
        val description = "fakeDescription"
        every { zrcClient.zaakinformatieobjectList(any()) } returns emptyList()
        every { zrcClient.zaakinformatieobjectCreate(any()) } returns createZaakInformatieobjectForReads()
        every { zgwClientHeadersFactory.setAuditExplanation(description) } just Runs

        `when`("koppelInformatieobject is called") {
            zrcClientService.koppelInformatieobject(informatieobject, targetZaak, description)

            then("it should create a new zaakinformatieobject") {
                verify(exactly = 1) {
                    zrcClient.zaakinformatieobjectCreate(any())
                }
            }
        }
    }

    given("An informatieobject that is already linked to a zaak") {
        val informatieobject = createEnkelvoudigInformatieObject()
        val targetZaak = createZaak()
        val existingZaakInformatieobject = createZaakInformatieobjectForReads()
        every { zrcClient.zaakinformatieobjectList(any()) } returns listOf(existingZaakInformatieobject)

        `when`("koppelInformatieobject is called") {
            val exception = shouldThrow<IllegalStateException> {
                zrcClientService.koppelInformatieobject(informatieobject, targetZaak, "fakeDescription")
            }

            then("it should throw an exception mentioning the zaak the informatieobject is already linked to") {
                exception.message shouldBe
                    "Informatieobject is reeds gekoppeld aan zaak '${existingZaakInformatieobject.zaak.extractUuid()}'"
            }
        }
    }
})
