/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.authentication.UserPrincipalFilter.Companion.ROL_DOMEIN_ELK_ZAAKTYPE
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_OPEN_SUBCASES
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.exception.CaseHasLockedInformationObjectsException
import nl.info.zac.zaak.exception.CaseHasOpenSubcasesException
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LargeClass")
class ZaakServiceTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val lockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val identityService = mockk<IdentityService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()

    val zaakService = ZaakService(
        eventingService = eventingService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zaakVariabelenService = zaakVariabelenService,
        lockService = lockService,
        identityService = identityService,
        zaakafhandelParameterService = zaakafhandelParameterService
    )
    val explanation = "fakeExplanation"
    val screenEventResourceId = "fakeResourceId"
    val zaken = listOf(
        createZaak(),
        createZaak()
    )
    val group = createGroup()
    val user = createUser()
    val rolTypeBehandelaar = createRolType(
        omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
    )
    val zaakafhandelParameters = createZaakafhandelParameters()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A list of open zaken and a group and a user") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns zaakafhandelParameters
            every {
                ztcClientService.readRoltype(
                    it.zaaktype,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(it, any(), explanation) } just Runs
            every { eventingService.send(capture(screenEventSlot)) } just Runs
        }
        every { identityService.isUserInGroup(user.id, group.id) } returns true

        When("the assign zaken function is called with a group, a user and a screen event resource id") {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = group,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then(
                """for both zaken the group and user roles 
                and the search index should be updated and
                a screen event of type 'zaken verdelen' should be sent"""
            ) {
                zaken.map {
                    verify(exactly = 2) {
                        zrcClientService.updateRol(it, any(), explanation)
                    }
                }
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("One open and one closed zaak and a group and a user") {
        val openZaak = createZaak()
        val closedZaak = createZaak(
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        )
        val zakenList = listOf(openZaak, closedZaak)
        zakenList.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
        }
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(openZaak.zaaktype.extractUuid())
        } returns zaakafhandelParameters
        every {
            ztcClientService.readRoltype(
                openZaak.zaaktype,
                OmschrijvingGeneriekEnum.BEHANDELAAR
            )
        } returns rolTypeBehandelaar
        every { zrcClientService.updateRol(openZaak, any(), explanation) } just Runs
        every { eventingService.send(any<ScreenEvent>()) } just Runs
        every { identityService.isUserInGroup(user.id, group.id) } returns true

        When(
            """the assign zaken function is called with a group, a user
                and a screen event resource id"""
        ) {
            zaakService.assignZaken(
                zaakUUIDs = zakenList.map { it.uuid },
                explanation = explanation,
                group = group,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then(
                """
                    only for the open zaak the group and user roles 
                    and the search index should be updated and
                    a screen event of type 'zaken verdelen' should sent
                    and the closed zaak should be skipped
                    """
            ) {
                // only the open zaak should have group and user roles assigned to it
                verify(exactly = 2) {
                    zrcClientService.updateRol(openZaak, any(), explanation)
                }
                verify(exactly = 1) {
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(closedZaak))
                    eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(screenEventResourceId))
                }
            }
        }
    }

    Given("A list of zaken and a screen event resource id") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { zrcClientService.deleteRol(it, any(), explanation) } just Runs
        }
        every { eventingService.send(capture(screenEventSlot)) } just Runs
        When(
            """the release zaken function is called with
                 a screen event resource id"""
        ) {
            zaakService.releaseZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                screenEventResourceId = screenEventResourceId
            )
            Then(
                """both zaken should no longer have a user assigned
                     but the group should still be assigned
                    and the search index should be updated and
                    a screen event of type 'zaken vrijgeven' should sent"""
            ) {
                zaken.map {
                    verify(exactly = 1) {
                        zrcClientService.deleteRol(it, BetrokkeneTypeEnum.MEDEWERKER, explanation)
                    }
                }
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VRIJGEVEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("One open and one closed zaak and a screen event resource id") {
        val openZaak = createZaak()
        val closedZaak = createZaak(
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        )
        val zakenList = listOf(openZaak, closedZaak)
        zakenList.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
        }
        every { zrcClientService.deleteRol(openZaak, any(), explanation) } just Runs
        every { eventingService.send(any<ScreenEvent>()) } just Runs
        When(
            """the release zaken function is called with
                 a screen event resource id"""
        ) {
            zaakService.releaseZaken(
                zaakUUIDs = zakenList.map { it.uuid },
                explanation = explanation,
                screenEventResourceId = screenEventResourceId
            )
            Then(
                """only the open zaak should no longer have a user assigned
                     but the group should still be assigned
                    and the search index should be updated and
                    a screen event of type 'zaken vrijgeven' should sent"""
            ) {
                verify(exactly = 1) {
                    zrcClientService.deleteRol(openZaak, BetrokkeneTypeEnum.MEDEWERKER, explanation)
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(closedZaak))
                    eventingService.send(ScreenEventType.ZAKEN_VRIJGEVEN.updated(screenEventResourceId))
                }
            }
        }
    }

    Given(
        """
         A list of zaken and a failing ZRC client service that throws an exception when retrieving the second zaak 
            """
    ) {
        every { zrcClientService.readZaak(zaken[0].uuid) } returns zaken[0]
        every { zrcClientService.readZaak(zaken[1].uuid) } throws RuntimeException("fakeRuntimeException")
        When(
            """the assign zaken function is called with a group
                and a screen event resource id"""
        ) {
            shouldThrow<RuntimeException> {
                zaakService.assignZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    screenEventResourceId = screenEventResourceId
                )
            }
            Then(
                """the exception should be thrown and for neither of the zaken 
                    the group and user role nor the search index should be updated
                    and no screen event of type 'zaken verdelen' should be sent"""
            ) {
                verify(exactly = 0) {
                    zrcClientService.updateRol(any(), any(), explanation)
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    Given("A list of zaken") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns zaakafhandelParameters
            every {
                ztcClientService.readRoltype(
                    it.zaaktype,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(it, any(), explanation) } just Runs
            every { zrcClientService.deleteRol(it, any(), explanation) } just Runs
            every { eventingService.send(capture(screenEventSlot)) } just Runs
        }
        When(
            """the assign zaken function is called with a group, WITHOUT a user
                and with a screen event resource id"""
        ) {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = group,
                screenEventResourceId = screenEventResourceId
            )

            Then(
                """for both zaken the group roles should be updated
                    and the user roles should be deleted"""
            ) {
                zaken.map {
                    verify(exactly = 1) {
                        zrcClientService.updateRol(it, any(), explanation)
                        zrcClientService.deleteRol(it, any(), explanation)
                    }
                }
            }
        }
    }

    Given("A list of zaken and a user not belonging to a group") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { eventingService.send(capture(screenEventSlot)) } just Runs
        }
        every { identityService.isUserInGroup(user.id, group.id) } returns false

        When("the assign zaken function is called") {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = group,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then("all the zaken should be skipped") {
                verify(exactly = 1) {
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaken[0]))
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaken[1]))
                }
            }

            And("a final screen event of type 'zaken verdelen' should be sent") {
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("A list of zaken with no domain and a group with ROL_DOMEIN_ELK_ZAAKTYPE") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { eventingService.send(capture(screenEventSlot)) } just Runs
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns createZaakafhandelParameters(domein = null)
            every {
                ztcClientService.readRoltype(
                    it.zaaktype,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(it, any(), explanation) } just Runs
        }
        val groupWithAllDomains = createGroup(zacClientRoles = listOf(ROL_DOMEIN_ELK_ZAAKTYPE))
        every { identityService.isUserInGroup(user.id, groupWithAllDomains.id) } returns true

        When("the assign zaken function is called") {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = groupWithAllDomains,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then("group and user roles are updated") {
                verify(exactly = 2) {
                    zrcClientService.updateRol(zaken[0], any(), explanation)
                    zrcClientService.updateRol(zaken[1], any(), explanation)
                }
            }

            And("no skip event is generated") {
                verify(exactly = 0) {
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaken[1]))
                }
            }

            And("a final screen event of type 'zaken verdelen' is sent") {
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("A list of zaken with no domain and a group with domain") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { eventingService.send(capture(screenEventSlot)) } just Runs
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns createZaakafhandelParameters()
        }
        val groupWithDomain = createGroup(zacClientRoles = listOf("another_domain"))
        every { identityService.isUserInGroup(user.id, groupWithDomain.id) } returns true

        When("the assign zaken function is called") {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = groupWithDomain,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then("no roles are updated") {
                verify(exactly = 0) {
                    zrcClientService.updateRol(any<Zaak>(), any<RolMedewerker>(), explanation)
                }
            }

            And("a final screen event of type 'zaken verdelen' is sent") {
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("A list of zaken with no domain and a group with no domain") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { eventingService.send(capture(screenEventSlot)) } just Runs
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns createZaakafhandelParameters(domein = null)
        }
        val groupWithNoDomain = createGroup(zacClientRoles = emptyList())
        every { identityService.isUserInGroup(user.id, groupWithNoDomain.id) } returns true

        When("the assign zaken function is called") {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = groupWithNoDomain,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then("no zaken roles are updated") {
                verify(exactly = 0) {
                    zrcClientService.updateRol(zaken[0], any(), explanation)
                    zrcClientService.updateRol(zaken[1], any(), explanation)
                }
            }

            And("a final screen event of type 'zaken verdelen' is sent") {
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("A list of two zaken and the second one has a group not matching the requested one") {
        val screenEventSlot = slot<ScreenEvent>()
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { eventingService.send(capture(screenEventSlot)) } just Runs
        }
        zaken[0].let {
            every {
                ztcClientService.readRoltype(
                    it.zaaktype,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(it, any(), explanation) } just Runs
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns createZaakafhandelParameters(domein = "zaaktype_domain")
        }
        zaken[1].let {
            every {
                zaakafhandelParameterService.readZaakafhandelParameters(it.zaaktype.extractUuid())
            } returns createZaakafhandelParameters(domein = "another_domein")
        }
        val group = createGroup(zacClientRoles = listOf("zaaktype_domain"))
        every { identityService.isUserInGroup(user.id, group.id) } returns true

        When("the assign zaken function is called") {
            zaakService.assignZaken(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = group,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then("group and user roles of the first zaak are updated") {
                verify(exactly = 2) {
                    zrcClientService.updateRol(zaken[0], any(), explanation)
                }
            }

            And("one skip event is generated") {
                verify(exactly = 1) {
                    eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaken[1]))
                }
            }

            And("a final screen event of type 'zaken verdelen' should be sent") {
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }

    Given("A zaak without any betrokkenen") {
        val zaak = createZaak()
        val roleTypeUUID = UUID.randomUUID()
        val roleTypeBelanghebbende = createRolType(
            omschrijvingGeneriek = OmschrijvingGeneriekEnum.BELANGHEBBENDE,
            uri = URI("https://example.com/roltype/$roleTypeUUID")
        )
        val roleSlot = slot<Rol<*>>()
        every { ztcClientService.readRoltype(roleTypeUUID) } returns roleTypeBelanghebbende
        every { zrcClientService.listRollen(zaak) } returns emptyList()
        every { zrcClientService.createRol(capture(roleSlot), explanation) } returns createRolNatuurlijkPersoon()

        When("a betrokkene of type natuurlijk persoon is added") {
            zaakService.addBetrokkeneToZaak(
                roleTypeUUID = roleTypeUUID,
                identificationType = IdentificatieType.BSN,
                identification = "fakeBSN",
                zaak = zaak,
                explanation = explanation
            )

            Then("the betrokkene is successfully added to the zaak") {
                verify(exactly = 1) {
                    zrcClientService.createRol(any(), explanation)
                }
                with(roleSlot.captured) {
                    this.zaak shouldBe zaak.url
                    roltype shouldBe roleTypeBelanghebbende.url
                    roltoelichting shouldBe explanation
                    omschrijving shouldBe roleTypeBelanghebbende.omschrijving
                    omschrijvingGeneriek shouldBe OmschrijvingGeneriekEnum.BELANGHEBBENDE.toString()
                }
            }
        }
    }

    Given("A zaak with a betrokkenen of type natuurlijk persoon and role type 'adviseur'") {
        val zaak = createZaak()
        val roleTypeUUID = UUID.randomUUID()
        val roleTypeAdviseur = createRolType(
            omschrijvingGeneriek = OmschrijvingGeneriekEnum.ADVISEUR
        )
        val roleTypeBelanghebbende = createRolType(
            omschrijvingGeneriek = OmschrijvingGeneriekEnum.BELANGHEBBENDE,
            uri = URI("https://example.com/roltype/$roleTypeUUID")
        )
        val identification = "fakeBSN"
        val roleAdviseur = createRolNatuurlijkPersoon(
            zaakURI = zaak.url,
            rolType = roleTypeAdviseur,
            natuurlijkPersoon = createNatuurlijkPersoonIdentificatie(bsn = identification)
        )
        val roleSlot = slot<Rol<*>>()
        every { ztcClientService.readRoltype(roleTypeUUID) } returns roleTypeBelanghebbende
        every { zrcClientService.listRollen(zaak) } returns listOf(roleAdviseur)
        every { zrcClientService.createRol(capture(roleSlot), explanation) } returns createRolNatuurlijkPersoon()

        When("the same betrokkene is added again but with the role type 'belanghebbende'") {
            zaakService.addBetrokkeneToZaak(
                roleTypeUUID = roleTypeUUID,
                identificationType = IdentificatieType.BSN,
                identification = identification,
                zaak = zaak,
                explanation = explanation
            )

            Then("the betrokkene is added to the zaak again with role type 'belanghebbende'") {
                verify(exactly = 1) {
                    zrcClientService.createRol(any(), any())
                }
                with(roleSlot.captured) {
                    this.zaak shouldBe zaak.url
                    roltype shouldBe roleTypeBelanghebbende.url
                    roltoelichting shouldBe explanation
                    omschrijving shouldBe roleTypeBelanghebbende.omschrijving
                    omschrijvingGeneriek shouldBe OmschrijvingGeneriekEnum.BELANGHEBBENDE.toString()
                }
            }
        }
    }

    Given("A zaak with a betrokkenen of type natuurlijk persoon and role type adviseur") {
        val zaak = createZaak()
        val roleTypeUUID = UUID.randomUUID()
        val roleTypeAdviseur = createRolType(
            omschrijvingGeneriek = OmschrijvingGeneriekEnum.ADVISEUR,
            uri = URI("https://example.com/roltype/$roleTypeUUID")
        )
        val identification = "fakeBSN"
        val roleAdviseur = createRolNatuurlijkPersoon(
            zaakURI = zaak.zaaktype,
            rolType = roleTypeAdviseur,
            natuurlijkPersoon = createNatuurlijkPersoonIdentificatie(bsn = identification)
        )
        every { ztcClientService.readRoltype(roleTypeUUID) } returns roleTypeAdviseur
        every { zrcClientService.listRollen(zaak) } returns listOf(roleAdviseur)

        When("the same betrokkene is added again with the same role type") {
            val exception = shouldThrow<BetrokkeneIsAlreadyAddedToZaakException> {
                zaakService.addBetrokkeneToZaak(
                    roleTypeUUID = roleTypeUUID,
                    identificationType = IdentificatieType.BSN,
                    identification = identification,
                    zaak = zaak,
                    explanation = explanation
                )
            }

            Then("an exception is thrown and the betrokkene is not added to the zaak again") {
                exception.message shouldBe "Betrokkene with type 'BSN' and identification 'fakeBSN' " +
                    "was already added to the zaak with UUID '${zaak.uuid}'. Ignoring."
                verify(exactly = 0) {
                    zrcClientService.createRol(any(), any())
                }
            }
        }
    }
    Given(
        """A zaak with one initiator, one behandelaar and two other betrokkenen roles
            not of type initiator or behandelaar"""
    ) {
        val zaak = createZaak()
        val rolNatuurlijkPersonen = listOf(
            createRolNatuurlijkPersoon(
                zaakURI = zaak.zaaktype,
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BELANGHEBBENDE)
            ),
            createRolOrganisatorischeEenheid(
                zaakURI = zaak.zaaktype,
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BESLISSER)
            ),
            createRolNatuurlijkPersoon(
                zaakURI = zaak.zaaktype,
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            ),
            createRolNatuurlijkPersoon(
                zaakURI = zaak.zaaktype,
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
            )
        )
        every { zrcClientService.listRollen(zaak) } returns rolNatuurlijkPersonen

        When("the list of betrokkenen is retrieved") {
            val betrokkenenRoles = zaakService.listBetrokkenenforZaak(zaak)

            Then("the list should consist of the two betrokkenen not of type initiator or behandelaar") {
                betrokkenenRoles.size shouldBe 2
                betrokkenenRoles[0] shouldBe rolNatuurlijkPersonen[0]
                betrokkenenRoles[1] shouldBe rolNatuurlijkPersonen[1]
            }
        }
    }
    Given("A zaak that has no open deelzaken and no locked information objects") {
        val zaak = createZaak()
        every { zrcClientService.heeftOpenDeelzaken(zaak) } returns false
        every { lockService.hasLockedInformatieobjecten(zaak) } returns false

        When("the zaak is checked if it is closeable") {
            shouldNotThrowAny { zaakService.checkZaakAfsluitbaar(zaak) }

            Then("it should not throw any exceptions") {
                verify(exactly = 1) {
                    zrcClientService.heeftOpenDeelzaken(zaak)
                    lockService.hasLockedInformatieobjecten(zaak)
                }
            }
        }
    }
    Given("A zaak that has open deelzaken") {
        val zaak = createZaak()
        every { zrcClientService.heeftOpenDeelzaken(zaak) } returns true

        When("the zaak is checked if it is closeable") {
            val exception = shouldThrow<CaseHasOpenSubcasesException> { zaakService.checkZaakAfsluitbaar(zaak) }

            Then("it should throw an exception") {
                exception.errorCode shouldBe ERROR_CODE_CASE_HAS_OPEN_SUBCASES
                exception.message shouldBe "Case ${zaak.uuid} has open subcases"
            }
        }
    }
    Given("A zaak that has no open deelzaken but has locked information objects") {
        val zaak = createZaak()
        every { zrcClientService.heeftOpenDeelzaken(zaak) } returns false
        every { lockService.hasLockedInformatieobjecten(zaak) } returns true

        When("the zaak is checked if it is closeable") {
            val exception =
                shouldThrow<CaseHasLockedInformationObjectsException> { zaakService.checkZaakAfsluitbaar(zaak) }

            Then("it should throw an exception") {
                exception.errorCode shouldBe ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
                exception.message shouldBe "Case ${zaak.uuid} has locked information objects"
            }
        }
    }

    Given("a zaak that is not heropend") {
        val zaakUuid = UUID.randomUUID()
        val statusUuid = UUID.randomUUID()
        val zaak = createZaak(
            uuid = zaakUuid,
            status = URI(statusUuid.toString())
        )
        val statusType = createStatusType().apply {
            omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING
        }
        val status = createZaakStatus(
            statusUuid,
            URI(statusUuid.toString()),
            zaak.url,
            statusType.url,
            ZonedDateTime.now().toOffsetDateTime()
        )

        every { zrcClientService.readStatus(zaak.status) } returns status
        every { ztcClientService.readStatustype(status.statustype) } returns statusType
        every {
            zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true)
        } just runs

        When("setOntvangstbevestigingVerstuurdIfNotHeropend is called") {
            zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)

            Then("ontvangstbevestiging is true") {
                verify(exactly = 1) {
                    zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true)
                }
            }
        }
    }

    Given("a zaak is heropend") {
        val zaakUuid = UUID.randomUUID()
        val statusUuid = UUID.randomUUID()
        val zaak = createZaak(
            uuid = zaakUuid,
            status = URI(statusUuid.toString())
        )
        val statusType = createStatusType().apply {
            omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND
        }
        val status = createZaakStatus(
            uuid = statusUuid,
            uri = URI(statusUuid.toString()),
            zaakURI = zaak.url,
            statustypeURI = statusType.url,
            datumStatusGezet = ZonedDateTime.now().toOffsetDateTime()
        )

        every { zrcClientService.readStatus(zaak.status) } returns status
        every { ztcClientService.readStatustype(status.statustype) } returns statusType

        When("setOntvangstbevestigingVerstuurdIfNotHeropend is called") {
            zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)

            Then("ontvangstbevestiging is false") {
                verify(exactly = 0) {
                    zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, false)
                }
            }
        }
    }
})
