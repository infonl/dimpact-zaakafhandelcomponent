/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.Eigenschap
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_HAS_LOCKED_INFORMATION_OBJECTS
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.ZACRole
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.exception.CaseHasLockedInformationObjectsException
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
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaakService = ZaakService(
        eventingService = eventingService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zaakVariabelenService = zaakVariabelenService,
        lockService = lockService,
        identityService = identityService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
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
    val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Assigning zaken") {
        Given("A list of open zaken and a group and a user") {
            val screenEventSlot = slot<ScreenEvent>()
            zaken.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns zaaktypeCmmnConfiguration
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
            clearAllMocks()
            val openZaak = createZaak()
            val closedZaak = createZaak(
                archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
            )
            val zakenList = listOf(openZaak, closedZaak)
            zakenList.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(openZaak.zaaktype.extractUuid())
            } returns zaaktypeCmmnConfiguration
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

        Given(
            """
         A list of zaken and a failing ZRC client service that throws an exception when retrieving the second zaak 
            """
        ) {
            clearAllMocks()
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
            clearAllMocks()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns zaaktypeCmmnConfiguration
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

        Given("A list of zaken with no domain and a group with ROL_DOMEIN_ELK_ZAAKTYPE") {
            clearAllMocks()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every { eventingService.send(capture(screenEventSlot)) } just Runs
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration(domein = null)
                every {
                    ztcClientService.readRoltype(
                        it.zaaktype,
                        OmschrijvingGeneriekEnum.BEHANDELAAR
                    )
                } returns rolTypeBehandelaar
                every { zrcClientService.updateRol(it, any(), explanation) } just Runs
            }
            val groupWithAllDomains = createGroup(zacClientRoles = listOf(ZACRole.DOMEIN_ELK_ZAAKTYPE.value))
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
            clearAllMocks()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every { eventingService.send(capture(screenEventSlot)) } just Runs
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration()
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
            clearAllMocks()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every { eventingService.send(capture(screenEventSlot)) } just Runs
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration(domein = null)
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
            clearAllMocks()
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
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration(domein = "zaaktype_domain")
            }
            zaken[1].let {
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration(domein = "another_domein")
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

        Given("A list of zaken and a group, but user is not in group") {
            clearAllMocks()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.map {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { identityService.isUserInGroup(user.id, group.id) } returns false
            every { eventingService.send(capture(screenEventSlot)) } just Runs

            When("the assign zaken function is called with a group, a user and a screen event resource id") {
                zaakService.assignZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                Then("the skipped screenevent is sent for ZAKEN_VERDELEN with the resource id") {
                    verify(exactly = 1) {
                        eventingService.send(ScreenEventType.ZAKEN_VERDELEN.skipped(screenEventResourceId))
                    }
                }
            }
        }

        Given("A list of zaken and a user not belonging to a group") {
            clearAllMocks()
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

                And("a final screen event of type 'zaken verdelen' should be skipped") {
                    with(screenEventSlot.captured) {
                        opcode shouldBe Opcode.SKIPPED
                        objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                        objectId.resource shouldBe screenEventResourceId
                    }
                }
            }
        }
    }

    Context("Releasing zaken") {
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
            clearAllMocks()
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
    }

    Context("Add betrokkenen to zaak") {
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
            clearAllMocks()
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
                natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = identification)
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
            clearAllMocks()
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
                natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = identification)
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
    }

    Context("List betrokkenen for zaak") {
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
        Given("A zaak that has no locked information objects") {
            val zaak = createZaak()
            every { lockService.hasLockedInformatieobjecten(zaak) } returns false

            When("the zaak is checked if it is closeable") {
                shouldNotThrowAny { zaakService.checkZaakAfsluitbaar(zaak) }

                Then("it should not throw any exceptions") {
                    verify(exactly = 1) {
                        lockService.hasLockedInformatieobjecten(zaak)
                    }
                }
            }
        }
    }

    Context("Check zaak afsluitbaar") {
        Given("A zaak that has locked information objects") {
            val zaak = createZaak()
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
    }

    Context("Set ontvangstbevestiging verstuurd") {
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
    }

    Context("Add initiator to zaak") {
        Given("An existing zaak and a vestiging identification with a KVK number and a vestigingsnummer") {
            val kvkNummer = "12345567"
            val vestingsnummer = "fakeVestigingsnummer"
            val identification = "$kvkNummer|$vestingsnummer"
            val explanation = "fakeExplanation"
            val zaak = createZaak()
            val roleType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            val roleSlot = slot<Rol<*>>()
            val createdRole = createRolNietNatuurlijkPersoon()
            every {
                ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns roleType
            every { zrcClientService.createRol(capture(roleSlot), explanation) } returns createdRole

            When("an initiator of type vestiging is added to the zaak") {
                zaakService.addInitiatorToZaak(
                    identificationType = IdentificatieType.VN,
                    identification = identification,
                    zaak = zaak,
                    explanation = explanation
                )

                Then(
                    "an initiator role of type niet-natuurlijk persoon with a KVK number and a vestigingsnummer is added to the zaak"
                ) {
                    with(roleSlot.captured) {
                        this.zaak shouldBe zaak.url
                        roltype shouldBe roleType.url
                        roltoelichting shouldBe explanation
                        omschrijving shouldBe zaak.omschrijving
                        omschrijvingGeneriek shouldBe OmschrijvingGeneriekEnum.INITIATOR.toString()
                        with((this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie!!) {
                            this.kvkNummer shouldBe kvkNummer
                            this.vestigingsNummer shouldBe vestingsnummer
                        }
                    }
                }
            }
        }

        Given("An existing zaak and an identication of KVK number") {
            val kvkNummer = "12345567"
            val explanation = "fakeExplanation"
            val zaak = createZaak()
            val roleType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            val roleSlot = slot<Rol<*>>()
            val createdRole = createRolNietNatuurlijkPersoon()
            every {
                ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns roleType
            every { zrcClientService.createRol(capture(roleSlot), explanation) } returns createdRole

            When("an initiator of type rechtspersoon (RSIN) is added to the zaak") {
                zaakService.addInitiatorToZaak(
                    identificationType = IdentificatieType.RSIN,
                    identification = kvkNummer,
                    zaak = zaak,
                    explanation = explanation
                )

                Then("an initiator role of type niet-natuurlijk persoon with a KVK number is added to the zaak") {
                    with(roleSlot.captured) {
                        this.zaak shouldBe zaak.url
                        roltype shouldBe roleType.url
                        roltoelichting shouldBe explanation
                        omschrijving shouldBe zaak.omschrijving
                        omschrijvingGeneriek shouldBe OmschrijvingGeneriekEnum.INITIATOR.toString()
                        (this as RolNietNatuurlijkPersoon).betrokkeneIdentificatie!!.kvkNummer shouldBe kvkNummer
                    }
                }
            }
        }
    }

    Context("Retrieve zaak and zaaktype by zaak ID") {
        Given("A valid zaak ID") {
            val zaakID = "fakeZaakID"
            val zaak = createZaak(identificatie = zaakID)
            val zaakType = createZaakType()
            every { zrcClientService.readZaakByID(zaakID) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

            When("readZaakAndZaakTypeByZaakID is called") {
                val result = zaakService.readZaakAndZaakTypeByZaakID(zaakID)

                Then("it should return the correct zaak and zaaktype") {
                    result.first shouldBe zaak
                    result.second shouldBe zaakType
                }
            }
        }
    }

    Context("Retrieve zaak and zaaktype by zaak UUID") {
        Given("A valid zaak UUID") {
            val zaakUUID = UUID.randomUUID()
            val zaak = createZaak(uuid = zaakUUID)
            val zaakType = createZaakType()
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

            When("readZaakAndZaakTypeByZaakUUID is called") {
                val result = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)

                Then("it should return the correct zaak and zaaktype") {
                    result.first shouldBe zaak
                    result.second shouldBe zaakType
                }
            }
        }
    }

    Context("Retrieve zaaktype by zaak") {
        Given("A zaak with a valid zaaktype") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

            When("retrieveZaakTypeByZaak is called") {
                val result = zaakService.readZaakTypeByZaak(zaak)

                Then("it should return the correct zaaktype") {
                    result shouldBe zaakType
                }
            }
        }
    }

    Context("Retrieve zaaktype by UUID") {
        Given("A zaaktype UUID") {
            val zaakTypeUUID = UUID.randomUUID()
            val zaakType = createZaakType()
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType

            When("readZaakTypeByUUID is called") {
                val result = zaakService.readZaakTypeByUUID(zaakTypeUUID)

                Then("it should return the correct zaaktype") {
                    result shouldBe zaakType
                }
            }
        }
    }

    Context("Process special brondatum procedure") {
        Given("A zaak and resultaattype with EIGENSCHAP afleidingswijze and existing zaakeigenschap") {
            val zaak = createZaak()
            val resultaatTypeUUID = UUID.randomUUID()
            val resultaatType = createResultaatType(
                brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                    afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP
                )
            )
            val brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
            )

            val existingZaakEigenschap = ZaakEigenschap(
                URI(""),
                UUID.randomUUID(),
                brondatumArchiefprocedure.datumkenmerk
            ).apply {
                waarde = "testWaarde"
            }

            every { ztcClientService.readResultaattype(resultaatTypeUUID) } returns resultaatType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(existingZaakEigenschap)
            every { zrcClientService.updateZaakeigenschap(any(), any(), any()) } returns existingZaakEigenschap

            When("processBrondatumProcedure is called with existing zaakeigenschap") {
                zaakService.processBrondatumProcedure(zaak, resultaatTypeUUID, brondatumArchiefprocedure)

                Then("it should update the existing zaakeigenschap") {
                    verify { zrcClientService.updateZaakeigenschap(zaak.uuid, existingZaakEigenschap.uuid, any()) }
                }

                And("it should not create a new zaakeigenschap") {
                    verify(exactly = 0) { zrcClientService.createEigenschap(zaak.uuid, any()) }
                }
            }
        }

        Given("A zaak and resultaattype with EIGENSCHAP afleidingswijze and non-existing zaakeigenschap") {
            val zaak = createZaak()
            val resultaatTypeUUID = UUID.randomUUID()
            val resultaatType = createResultaatType(
                brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                    afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP
                )
            )
            val brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP
            )
            val eigenschap = Eigenschap()
            every { ztcClientService.readResultaattype(resultaatTypeUUID) } returns resultaatType
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()
            every { ztcClientService.readEigenschap(zaak.zaaktype, brondatumArchiefprocedure.datumkenmerk) } returns eigenschap
            every { zrcClientService.createEigenschap(any(), any()) } returns mockk()

            When("processBrondatumProcedure is called with non-existing zaakeigenschap") {
                zaakService.processBrondatumProcedure(zaak, resultaatTypeUUID, brondatumArchiefprocedure)

                Then("it should create a new zaakeigenschap") {
                    verify { zrcClientService.createEigenschap(zaak.uuid, any()) }
                }

                And("it should not update any existing zaakeigenschap") {
                    verify(exactly = 0) { zrcClientService.updateZaakeigenschap(zaak.uuid, any(), any()) }
                }
            }
        }
    }

    Context("Listing status types for a zaaktype") {
        Given("a zaak with status types") {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val statusTypes = listOf(
                createStatusType(omschrijving = "first"),
                createStatusType(omschrijving = "second")
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readStatustypen(zaakType.url) } returns statusTypes

            When("list of zaak status types is requested") {
                val statusTypeData = zaakService.listStatusTypes(zaaktypeUuid)

                Then("correct status type data is returned") {
                    statusTypeData shouldHaveSize 2
                    with(statusTypeData.first()) {
                        naam shouldBe "first"
                    }
                    with(statusTypeData.last()) {
                        naam shouldBe "second"
                    }
                }
            }
        }
    }

    Context("Listing result types for a zaaktype") {
        Given("a zaak with result types") {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val resultTypes = listOf(
                createResultaatType(omschrijving = "first"),
                createResultaatType(omschrijving = "second")
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readResultaattypen(zaakType.url) } returns resultTypes

            When("list of zaak result types is requested") {
                val resultTypeData = zaakService.listResultTypes(zaaktypeUuid)

                Then("correct result type data is returned") {
                    resultTypeData shouldHaveSize 2
                    with(resultTypeData.first()) {
                        naam shouldBe "first"
                    }
                    with(resultTypeData.last()) {
                        naam shouldBe "second"
                    }
                }
            }
        }
    }
})
