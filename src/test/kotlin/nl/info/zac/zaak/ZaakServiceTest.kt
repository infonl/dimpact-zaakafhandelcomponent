/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
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
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.createPabcGroupRepresentation
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.ZacApplicationRole
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LargeClass")
class ZaakServiceTest : BehaviorSpec({
    val bpmnService = mockk<BpmnService>()
    val configuratieService = mockk<ConfiguratieService>()
    val eventingService = mockk<EventingService>()
    val identityService = mockk<IdentityService>()
    val indexingService = mockk<IndexingService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val pabcClientService = mockk<PabcClientService>()
    val zaakService = ZaakService(
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zgwApiService = zgwApiService,
        eventingService = eventingService,
        zaakVariabelenService = zaakVariabelenService,
        identityService = identityService,
        indexingService = indexingService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        bpmnService = bpmnService,
        configuratieService = configuratieService,
        pabcClientService = pabcClientService
    )
    val explanation = "fakeExplanation"
    val screenEventResourceId = "fakeResourceId"

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Assigning a zaak") {
        Given("a zaak exists, no user and no group are assigned and zaak assignment data is provided") {
            val zaak = createZaak()
            val user = createLoggedInUser()
            val rolSlot = mutableListOf<Rol<*>>()
            val group = createGroup()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val reason = "fakeReason"
            every { zrcClientService.updateRol(zaak, capture(rolSlot), reason) } just runs
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns null
            every { identityService.readUser(user.id) } returns user
            every { zgwApiService.findGroepForZaak(zaak) } returns null
            every { identityService.readGroup(group.name) } returns group
            every { ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR) } returns rolTypeBehandelaar
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.description) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName") } just runs
            every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs

            When("the zaak is assigned to a user and a group") {
                zaakService.assignZaak(zaak, group.name, user.id, "fakeReason")

                Then("the zaak is assigned both to the group and the user") {
                    verify(exactly = 2) {
                        zrcClientService.updateRol(zaak, any(), reason)
                    }
                    with(rolSlot[0]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                        with(betrokkeneIdentificatie as MedewerkerIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                    with(rolSlot[1]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                }

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.description)
                        zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName")
                    }
                }
            }
        }

        Given("a zaak with no user and group assigned and zaak assignment data is provided") {
            val zaak = createZaak()
            val groupId = "unknown"
            val userId = "fakeUser"

            every { identityService.validateIfUserIsInGroup(userId, groupId) } throws UserNotInGroupException()

            When("the zaak is assigned to an unknown group") {
                shouldThrow<UserNotInGroupException> {
                    zaakService.assignZaak(zaak, groupId, userId, "fakeReason")
                }

                Then("an exception is thrown") {}
            }
        }

        Given("a zaak exists, with a user and group already assigned and zaak assignment data is provided") {
            val zaak = createZaak()
            val user = createLoggedInUser()
            val rolSlot = mutableListOf<Rol<*>>()
            val existingRolMedewerker = createRolMedewerker()
            val group = createGroup()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val existingRolGroup = createRolOrganisatorischeEenheid()
            val reason = "fakeReason"

            every { zrcClientService.updateRol(zaak, capture(rolSlot), reason) } just runs
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns existingRolMedewerker
            every { identityService.readUser(user.id) } returns user
            every { zgwApiService.findGroepForZaak(zaak) } returns existingRolGroup
            every { identityService.readGroup(group.name) } returns group
            every { ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR) } returns rolTypeBehandelaar
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.description) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName") } just runs

            When("the zaak is assigned to a user and a group") {
                every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs

                zaakService.assignZaak(zaak, group.name, user.id, reason)

                Then("the zaak is assigned both to the group and the user") {
                    verify(exactly = 2) {
                        zrcClientService.updateRol(zaak, any(), reason)
                    }
                    with(rolSlot[0]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                        with(betrokkeneIdentificatie as MedewerkerIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                    with(rolSlot[1]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                }

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.description)
                        zaakVariabelenService.setUser(zaak.uuid, "fakeDisplayName")
                    }
                }
            }
        }

        Given(
            "a zaak exists, with a user and group already assigned and zaak assignment for a group only is provided"
        ) {
            val zaak = createZaak()
            val updateRolSlot = mutableListOf<Rol<*>>()
            val group = createGroup()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val existingRolGroup = createRolOrganisatorischeEenheid()
            val reason = "fakeReason"
            every { zrcClientService.updateRol(zaak, capture(updateRolSlot), reason) } just runs
            every { zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, reason) } just runs
            every { zgwApiService.findGroepForZaak(zaak) } returns existingRolGroup
            every { identityService.readGroup(group.name) } returns group
            every { ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.BEHANDELAAR) } returns rolTypeBehandelaar
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.description) } just runs
            every { zaakVariabelenService.removeUser(zaak.uuid) } just runs

            When("the zaak is assigned to a group only") {
                zaakService.assignZaak(zaak, group.name, null, reason)

                Then("the zaak is assigned both to the group and the user") {
                    verify(exactly = 1) {
                        zrcClientService.updateRol(zaak, any(), reason)
                    }
                    with(updateRolSlot.first()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                }

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.description)
                        zaakVariabelenService.removeUser(zaak.uuid)
                    }
                }
            }
        }

        Given("a zaak exists, with a user and group already assigned, but the process instance is not found when setting zaak variables") {
            val zaak = createZaak()
            val user = createLoggedInUser()
            val rolSlot = mutableListOf<Rol<*>>()
            val existingRolMedewerker = createRolMedewerker()
            val group = createGroup()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val existingRolGroup = createRolOrganisatorischeEenheid()
            val reason = "fakeReason"

            every { zrcClientService.updateRol(zaak, capture(rolSlot), reason) } just runs
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns existingRolMedewerker
            every { identityService.readUser(user.id) } returns user
            every { zgwApiService.findGroepForZaak(zaak) } returns existingRolGroup
            every { identityService.readGroup(group.name) } returns group
            every {
                ztcClientService.readRoltype(
                    zaak.zaaktype,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            } returns rolTypeBehandelaar
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every {
                zaakVariabelenService.setGroup(
                    zaak.uuid,
                    group.description
                )
            } throws RuntimeException("No case or process instance found for zaak with UUID: ${zaak.uuid}")

            When("the zaak is assigned to a user and a group, but setUser return an error") {
                every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs

                zaakService.assignZaak(zaak, group.name, user.id, reason)

                Then("the zaak is assigned both to the group and the user") {
                    verify(exactly = 2) {
                        zrcClientService.updateRol(zaak, any(), reason)
                    }
                    with(rolSlot[0]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                        with(betrokkeneIdentificatie as MedewerkerIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                    with(rolSlot[1]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        with(betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie) {
                            identificatie shouldBe "fakeId"
                        }
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe rolTypeBehandelaar.omschrijving
                    }
                }

                And("the zaken search index is updated") {
                    verify(exactly = 1) {
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }

                And("the zaak data is updated accordingly") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.description)
                    }
                }
            }
        }
    }

    Context("Assigning zaken") {
        Given(
            """
                A list of open zaken and a group that is authorised for the application role 'behandelaar' and the zaaktype of the zaken,
                and a user and PABC feature flag on
                """
        ) {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(
                uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
            )
            val zaken = listOf(
                createZaak(
                    zaaktypeUri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
                ),
                createZaak(
                    zaaktypeUri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
                )
            )
            val user = createUser()
            val group = createGroup()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val pabcGroupRepresentation = createPabcGroupRepresentation(
                name = group.name,
                description = group.description
            )
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every {
                    ztcClientService.readRoltype(
                        it.zaaktype,
                        OmschrijvingGeneriekEnum.BEHANDELAAR
                    )
                } returns rolTypeBehandelaar
                every { zrcClientService.updateRol(it, any(), explanation) } just Runs
                every { eventingService.send(capture(screenEventSlot)) } just Runs
            }
            every { configuratieService.featureFlagPabcIntegration() } returns true
            every { identityService.isUserInGroup(user.id, group.name) } returns true
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                    applicationRole = "behandelaar",
                    zaaktypeDescription = zaaktype.omschrijving
                )
            } returns listOf(pabcGroupRepresentation)

            When("the assign zaken function is called with a group, a user and a screen event resource id") {
                zaakService.assignZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                Then(
                    """for all zaken the group and user roles 
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

                And(
                    """the zaaktype CMMN configuration is not requested, because zaaktype - group authorisation using domains
                    will be implemented differently using the PABC and is not yet supported with the PABC feature flag on"""
                ) {
                    verify(exactly = 0) {
                        zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(any())
                    }
                }
            }
        }

        Given(
            """
            One open and one closed zaak and a group that is authorised for the application role 'behandelaar' and the zaaktype of the zaak,
             and a user and PABC feature flag on
            """
        ) {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(
                uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
            )
            val openZaak = createZaak(
                zaaktypeUri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
            )
            val closedZaak = createZaak(
                archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
            )
            val zakenList = listOf(openZaak, closedZaak)
            val group = createGroup()
            val pabcGroupRepresentation = createPabcGroupRepresentation(
                name = group.name,
                description = group.description
            )
            val user = createUser()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            zakenList.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                ztcClientService.readRoltype(
                    openZaak.zaaktype,
                    OmschrijvingGeneriekEnum.BEHANDELAAR
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(openZaak, any(), explanation) } just Runs
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(pabcGroupRepresentation)
            every { eventingService.send(any<ScreenEvent>()) } just Runs
            every { configuratieService.featureFlagPabcIntegration() } returns true
            every { identityService.isUserInGroup(user.id, group.name) } returns true

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
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val group = createGroup()
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

        Given(
            """
                A list of zaken and a group that is authorised for the application role 'behandelaar' and the zaaktype of the zaken,
                and a user and PABC feature flag on
                """
        ) {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(
                uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
            )
            val zaken = listOf(
                createZaak(
                    zaaktypeUri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
                ),
                createZaak(
                    zaaktypeUri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID")
                )
            )
            val group = createGroup()
            val pabcGroupRepresentation = createPabcGroupRepresentation(
                name = group.name,
                description = group.description
            )
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val screenEventSlot = slot<ScreenEvent>()
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                    applicationRole = "behandelaar",
                    zaaktypeDescription = zaaktype.omschrijving
                )
            } returns listOf(
                pabcGroupRepresentation
            )
            zaken.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every {
                    ztcClientService.readRoltype(
                        it.zaaktype,
                        OmschrijvingGeneriekEnum.BEHANDELAAR
                    )
                } returns rolTypeBehandelaar
                every { zrcClientService.updateRol(it, any(), explanation) } just Runs
                every { zrcClientService.deleteRol(it, any(), explanation) } just Runs
                every { eventingService.send(capture(screenEventSlot)) } just Runs
                every { configuratieService.featureFlagPabcIntegration() } returns true
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

        Given("A list of zaken with no domain and a group with ROL_DOMEIN_ELK_ZAAKTYPE and PABC feature flag off") {
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val user = createUser()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
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
            val groupWithAllDomains = createGroup(zacClientRoles = listOf(ZacApplicationRole.DOMEIN_ELK_ZAAKTYPE.value))
            every { identityService.isUserInGroup(user.id, groupWithAllDomains.name) } returns true
            every { configuratieService.featureFlagPabcIntegration() } returns false

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

        Given("A list of zaken with no domain and a group with domain and PABC feature flag off") {
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val user = createUser()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every { eventingService.send(capture(screenEventSlot)) } just Runs
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration()
            }
            val groupWithDomain = createGroup(zacClientRoles = listOf("another_domain"))
            every { identityService.isUserInGroup(user.id, groupWithDomain.name) } returns true
            every { configuratieService.featureFlagPabcIntegration() } returns false

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

        Given("A list of zaken with no domain and a group with no domain and PABC feature flag off") {
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val user = createUser()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every { eventingService.send(capture(screenEventSlot)) } just Runs
                every {
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(it.zaaktype.extractUuid())
                } returns createZaaktypeCmmnConfiguration(domein = null)
            }
            val groupWithNoDomain = createGroup(zacClientRoles = emptyList())
            every { identityService.isUserInGroup(user.id, groupWithNoDomain.name) } returns true
            every { configuratieService.featureFlagPabcIntegration() } returns false

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

        Given(
            "A list of two zaken and the second one has a group not matching the requested one and PABC feature flag off"
        ) {
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val user = createUser()
            val rolTypeBehandelaar = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR
            )
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
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
            every { identityService.isUserInGroup(user.id, group.name) } returns true
            every { configuratieService.featureFlagPabcIntegration() } returns false

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
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val group = createGroup()
            val user = createUser()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { identityService.isUserInGroup(user.id, group.name) } returns false
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
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val group = createGroup()
            val user = createUser()
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
                every { eventingService.send(capture(screenEventSlot)) } just Runs
            }
            every { identityService.isUserInGroup(user.id, group.name) } returns false

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
            val zaken = listOf(
                createZaak(),
                createZaak()
            )
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach {
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
            val zaak = createZaak()
            val roleTypeUUID = UUID.randomUUID()
            val roleTypeAdviseur = createRolType(
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.ADVISEUR,
                uri = URI("https://example.com/roltype/$roleTypeUUID")
            )
            val identification = "fakeBSN"
            val roleAdviseur = createRolNatuurlijkPersoon(
                zaakURI = zaak.url,
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
                    zaakURI = zaak.url,
                    rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BELANGHEBBENDE)
                ),
                createRolOrganisatorischeEenheid(
                    zaakURI = zaak.url,
                    rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BESLISSER)
                ),
                createRolNatuurlijkPersoon(
                    zaakURI = zaak.url,
                    rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
                ),
                createRolNatuurlijkPersoon(
                    zaakURI = zaak.url,
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
