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
import io.mockk.verifyOrder
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.exception.CaseOrProcessNotFoundException
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.createPabcGroupRepresentation
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createOrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.MedewerkerIdentificatie
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBehandelaarRolType
import nl.info.client.zgw.ztc.model.createBrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.createEigenschap
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.createZaakspecifiekGeautoriseerdeMedewerkerRolType
import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.model.ZaakAssignment
import nl.info.zac.zaak.model.createZaakToewijzing

@Suppress("LargeClass")
class ZaakServiceTest : BehaviorSpec({
    val bpmnService = mockk<BpmnService>()
    val eventingService = mockk<EventingService>()
    val identityService = mockk<IdentityService>()
    val indexingService = mockk<IndexingService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val pabcClientService = mockk<PabcClientService>()
    val zaakspecifiekeAutorisatieService = mockk<ZaakspecifiekeAutorisatieService>()
    val zaakService = ZaakService(
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        zgwApiService = zgwApiService,
        eventingService = eventingService,
        zaakVariabelenService = zaakVariabelenService,
        identityService = identityService,
        indexingService = indexingService,
        bpmnService = bpmnService,
        pabcClientService = pabcClientService,
        zaakspecifiekeAutorisatieService = zaakspecifiekeAutorisatieService
    )
    val explanation = "fakeExplanation"
    val screenEventResourceId = "fakeResourceId"

    afterEach {
        checkUnnecessaryStub()
    }

    context("Reading a zaak assignment") {
        given("a user that is a member of the group") {
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            every { identityService.validateIfUserIsInGroup("fakeUserId", "fakeGroupId") } just runs
            every { identityService.readUser("fakeUserId") } returns user
            every { identityService.readGroup("fakeGroupId") } returns group

            `when`("the zaak assignment is read") {
                val zaakAssignment = zaakService.readZaakAssignment(groupId = "fakeGroupId", userName = "fakeUserId")

                then("it holds the group and the user") {
                    zaakAssignment shouldBe ZaakAssignment(group = group, user = user)
                }
                and("no zaak is changed") {
                    verify(exactly = 0) {
                        zrcClientService.createRol(any(), any())
                        zrcClientService.updateRol(any(), any(), any())
                        zrcClientService.deleteRol(any<Rol<*>>(), any())
                    }
                }
            }
        }

        given("a user that is not a member of the group") {
            every {
                identityService.validateIfUserIsInGroup("fakeUserId", "fakeGroupId")
            } throws UserNotInGroupException()

            `when`("the zaak assignment is read") {
                val userNotInGroupException = shouldThrow<UserNotInGroupException> {
                    zaakService.readZaakAssignment(groupId = "fakeGroupId", userName = "fakeUserId")
                }

                then("it is refused with its own error code") {
                    userNotInGroupException.errorCode shouldBe ErrorCode.ERROR_CODE_USER_NOT_IN_GROUP
                }
                and("neither the user nor the group is read") {
                    verify(exactly = 0) {
                        identityService.readUser(any())
                        identityService.readGroup(any())
                    }
                }
            }
        }

        given("a group and an empty user name") {
            val group = createGroup(id = "fakeGroupId")
            every { identityService.readGroup("fakeGroupId") } returns group

            `when`("the zaak assignment is read") {
                val zaakAssignment = zaakService.readZaakAssignment(groupId = "fakeGroupId", userName = "")

                then("it holds the group and no user, so that the behandelaar is removed") {
                    zaakAssignment shouldBe ZaakAssignment(group = group, user = null)
                }
                and("the membership of the empty user is not validated") {
                    verify(exactly = 0) { identityService.validateIfUserIsInGroup(any(), any()) }
                }
            }
        }
    }

    context("Assigning a zaak") {
        given("a zaak without a groep and without a behandelaar") {
            val zaak = createZaak()
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val zaakToewijzing = createZaakToewijzing()
            val createdRollen = mutableListOf<Rol<*>>()
            val updatedRollen = mutableListOf<Rol<*>>()
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, user.id) } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs
            every { identityService.readUser(user.id) } returns user
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.createRol(capture(createdRollen), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, capture(updatedRollen), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, group.name) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, user.id) } just runs
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is assigned to a group and a user") {
                zaakService.assignZaak(zaak = zaak, groupId = group.name, userName = user.id, reason = reason)

                then("a behandelaar rol is created for the user") {
                    with(createdRollen.single()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER
                        (betrokkeneIdentificatie as MedewerkerIdentificatie).identificatie shouldBe user.id
                        this.zaak shouldBe zaak.url
                        omschrijving shouldBe behandelaarRolType.omschrijving
                    }
                }

                and("a groep rol is set for the group") {
                    with(updatedRollen.single()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        (betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie).identificatie shouldBe
                            group.name
                        this.zaak shouldBe zaak.url
                    }
                }

                and("no rol is deleted, because the zaak had no behandelaar") {
                    verify(exactly = 0) { zrcClientService.deleteRol(any<Rol<*>>(), reason) }
                }

                and("the flowable variables and the search index are brought in line") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, group.name)
                        zaakVariabelenService.setUser(zaak.uuid, user.id)
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }
            }
        }

        given("a zaak and a user that is not a member of the requested group") {
            val zaak = createZaak()
            val reason = "fakeReason"
            every {
                identityService.validateIfUserIsInGroup("fakeUserId", "fakeGroupId")
            } throws UserNotInGroupException()

            `when`("the zaak is assigned to that user and that group") {
                val userNotInGroupException = shouldThrow<UserNotInGroupException> {
                    zaakService.assignZaak(
                        zaak = zaak,
                        groupId = "fakeGroupId",
                        userName = "fakeUserId",
                        reason = reason
                    )
                }

                then("the assignment is refused with its own error code") {
                    userNotInGroupException.errorCode shouldBe ErrorCode.ERROR_CODE_USER_NOT_IN_GROUP
                }

                and("no rol is written and the rollen of the zaak are not even read") {
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.readZaakToewijzing(any())
                        zrcClientService.createRol(any(), reason)
                        zrcClientService.updateRol(zaak, any(), reason)
                        zrcClientService.deleteRol(any<Rol<*>>(), reason)
                    }
                }
            }
        }

        given("an ordinary zaak that is assigned to another behandelaar and another groep") {
            val zaak = createZaak()
            val newBehandelaar = createUser(id = "fakeNewBehandelaarId")
            val newGroup = createGroup(id = "fakeNewGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val currentBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeCurrentBehandelaarId")
            )
            val zaakToewijzing = createZaakToewijzing(
                groep = createRolOrganisatorischeEenheid(zaakURI = zaak.url, rolType = behandelaarRolType),
                behandelaarRollen = listOf(currentBehandelaarRol)
            )
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, newBehandelaar.id)
            } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(newBehandelaar.id, newGroup.name) } just runs
            every { identityService.readUser(newBehandelaar.id) } returns newBehandelaar
            every { identityService.readGroup(newGroup.name) } returns newGroup
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.deleteRol(currentBehandelaarRol, reason) } just runs
            every { zrcClientService.createRol(any(), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, newGroup.name) } just runs
            every { zaakVariabelenService.setUser(zaak.uuid, newBehandelaar.id) } just runs
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is assigned to the new behandelaar and the new groep") {
                zaakService.assignZaak(
                    zaak = zaak,
                    groupId = newGroup.name,
                    userName = newBehandelaar.id,
                    reason = reason
                )

                then("the rol of the current behandelaar is deleted before the new one is created") {
                    verifyOrder {
                        zrcClientService.deleteRol(currentBehandelaarRol, reason)
                        zrcClientService.createRol(any(), reason)
                    }
                }

                and("the previous behandelaar keeps no access, because the zaak is not zaakspecifiek geautoriseerd") {
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(zaak, any(), any(), any())
                    }
                }

                and("the groep rol is updated and the search index is refreshed") {
                    verify(exactly = 1) {
                        zrcClientService.updateRol(zaak, any(), reason)
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }
            }
        }

        given("a zaak that is already assigned to the requested behandelaar and the requested groep") {
            val zaak = createZaak()
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val zaakToewijzing = createZaakToewijzing(
                groep = createRolOrganisatorischeEenheid(
                    zaakURI = zaak.url,
                    rolType = behandelaarRolType,
                    organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(
                        identificatie = group.name
                    )
                ),
                behandelaarRollen = listOf(
                    createRolMedewerker(
                        zaakURI = zaak.url,
                        rolType = behandelaarRolType,
                        medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = user.id)
                    )
                )
            )
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, user.id) } just runs
            every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs
            every { identityService.readUser(user.id) } returns user
            every { identityService.readGroup(group.name) } returns group
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false

            `when`("the zaak is assigned to that same behandelaar and groep") {
                zaakService.assignZaak(zaak = zaak, groupId = group.name, userName = user.id, reason = reason)

                then("no rol is written and the search index is left alone") {
                    verify(exactly = 0) {
                        zrcClientService.createRol(any(), reason)
                        zrcClientService.deleteRol(any<Rol<*>>(), reason)
                        zrcClientService.updateRol(zaak, any(), reason)
                        indexingService.indexeerDirect(zaak.uuid.toString(), any(), any())
                    }
                }
            }
        }

        given("a zaak with a behandelaar and a groep that is assigned to another groep only") {
            val zaak = createZaak()
            val newGroup = createGroup(id = "fakeNewGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val currentBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeCurrentBehandelaarId")
            )
            val zaakToewijzing = createZaakToewijzing(
                groep = createRolOrganisatorischeEenheid(zaakURI = zaak.url, rolType = behandelaarRolType),
                behandelaarRollen = listOf(currentBehandelaarRol)
            )
            val updatedRollen = mutableListOf<Rol<*>>()
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, null) } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.readGroup(newGroup.name) } returns newGroup
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.deleteRol(currentBehandelaarRol, reason) } just runs
            every { zrcClientService.updateRol(zaak, capture(updatedRollen), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.setGroup(zaak.uuid, newGroup.name) } just runs
            every { zaakVariabelenService.removeUser(zaak.uuid) } just runs
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is assigned to the new groep without naming a user") {
                zaakService.assignZaak(zaak = zaak, groupId = newGroup.name, userName = null, reason = reason)

                then("the groep rol is updated and the behandelaar rol is deleted without a replacement") {
                    with(updatedRollen.single()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.ORGANISATORISCHE_EENHEID
                        (betrokkeneIdentificatie as OrganisatorischeEenheidIdentificatie).identificatie shouldBe
                            newGroup.name
                    }
                    verify(exactly = 1) { zrcClientService.deleteRol(currentBehandelaarRol, reason) }
                    verify(exactly = 0) { zrcClientService.createRol(any(), reason) }
                }

                and("the flowable user variable is removed") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setGroup(zaak.uuid, newGroup.name)
                        zaakVariabelenService.removeUser(zaak.uuid)
                    }
                }
            }
        }

        given("an ordinary zaak with a behandelaar and an individually authorised medewerker") {
            val zaak = createZaak()
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val behandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId")
            )
            val geautoriseerdeMedewerkerRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaak.zaaktype),
                medewerkerIdentificatie = createMedewerkerIdentificatie(
                    identificatie = "fakeGeautoriseerdeMedewerkerId"
                )
            )
            val zaakToewijzing = createZaakToewijzing(
                groep = createRolOrganisatorischeEenheid(zaakURI = zaak.url, rolType = behandelaarRolType),
                behandelaarRollen = listOf(behandelaarRol),
                zaakspecifiekGeautoriseerdeMedewerkers = listOf(geautoriseerdeMedewerkerRol)
            )
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, null) } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { zrcClientService.deleteRol(behandelaarRol, reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every { zaakVariabelenService.removeUser(zaak.uuid) } just runs
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is released") {
                zaakService.assignZaak(zaak = zaak, groupId = null, userName = null, reason = reason)

                then("only the behandelaar rol is deleted, so the individual authorisation survives") {
                    verify(exactly = 1) { zrcClientService.deleteRol(behandelaarRol, reason) }
                    verify(exactly = 0) { zrcClientService.deleteRol(geautoriseerdeMedewerkerRol, any()) }
                }

                and("the groep of the zaak is left untouched") {
                    verify(exactly = 0) { zrcClientService.updateRol(zaak, any(), any()) }
                }
            }
        }

        given("a zaak whose flowable case or process instance no longer exists") {
            val zaak = createZaak()
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val zaakToewijzing = createZaakToewijzing()
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, user.id) } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs
            every { identityService.readUser(user.id) } returns user
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.createRol(any(), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
            every {
                zaakVariabelenService.setGroup(zaak.uuid, group.name)
            } throws CaseOrProcessNotFoundException("fakeCaseOrProcessNotFoundMessage")
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is assigned to a group and a user") {
                zaakService.assignZaak(zaak = zaak, groupId = group.name, userName = user.id, reason = reason)

                then("the rollen are still written and the search index is still refreshed") {
                    verify(exactly = 1) {
                        zrcClientService.createRol(any(), reason)
                        zrcClientService.updateRol(zaak, any(), reason)
                        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                    }
                }
            }
        }

        given("a zaak that was given two behandelaar rollen outside ZAC") {
            val zaak = createZaak()
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val firstBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId1")
            )
            val secondBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId2")
            )
            val zaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(firstBehandelaarRol, secondBehandelaarRol)
            )
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, user.id) } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(user.id, group.name) } just runs
            every { identityService.readUser(user.id) } returns user
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.deleteRol(any<Rol<*>>(), reason) } just runs
            every { zrcClientService.createRol(any(), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is assigned to a new behandelaar") {
                zaakService.assignZaak(zaak = zaak, groupId = group.name, userName = user.id, reason = reason)

                then("both rollen are purged before a single new behandelaar rol is created") {
                    verify(exactly = 2) { zrcClientService.deleteRol(any<Rol<*>>(), reason) }
                    verify(exactly = 1) { zrcClientService.createRol(any(), reason) }
                    verifyOrder {
                        zrcClientService.deleteRol(firstBehandelaarRol, reason)
                        zrcClientService.deleteRol(secondBehandelaarRol, reason)
                        zrcClientService.createRol(any(), reason)
                    }
                }
            }
        }
    }

    context("Assigning a zaakspecifiek geautoriseerde zaak") {
        given("a zaakspecifiek geautoriseerde zaak with a behandelaar") {
            val zaak = createZaak()
            val newBehandelaar = createUser(id = "fakeNewBehandelaarId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val previousBehandelaarIdentificatie = createMedewerkerIdentificatie(
                identificatie = "fakePreviousBehandelaarId"
            )
            val previousBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = previousBehandelaarIdentificatie
            )
            val zaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(previousBehandelaarRol),
                isZaakspecifiekGeautoriseerd = true
            )
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, newBehandelaar.id)
            } just runs
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    zaak,
                    previousBehandelaarIdentificatie,
                    reason,
                    any()
                )
            } returns true
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(newBehandelaar.id, group.name) } just runs
            every { identityService.readUser(newBehandelaar.id) } returns newBehandelaar
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.deleteRol(previousBehandelaarRol, reason) } just runs
            every { zrcClientService.createRol(any(), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is handed over to another behandelaar") {
                zaakService.assignZaak(
                    zaak = zaak,
                    groupId = group.name,
                    userName = newBehandelaar.id,
                    reason = reason
                )

                then("the previous behandelaar is granted an individual authorisation before losing their rol") {
                    verifyOrder {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                            zaak,
                            previousBehandelaarIdentificatie,
                            reason,
                            any()
                        )
                        zrcClientService.deleteRol(previousBehandelaarRol, reason)
                        zrcClientService.createRol(any(), reason)
                    }
                }

                and("the taken and documenten of the zaak are reindexed, because who may see them changed") {
                    verify(exactly = 1) {
                        zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)
                    }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak with a behandelaar and an individually authorised medewerker") {
            val zaak = createZaak()
            val returningBehandelaar = createUser(id = "fakeReturningBehandelaarId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val currentBehandelaarIdentificatie = createMedewerkerIdentificatie(
                identificatie = "fakeCurrentBehandelaarId"
            )
            val currentBehandelaarRol = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = currentBehandelaarIdentificatie
            )
            val zaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(currentBehandelaarRol),
                zaakspecifiekGeautoriseerdeMedewerkers = listOf(
                    createRolMedewerker(
                        zaakURI = zaak.url,
                        rolType = createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaak.zaaktype),
                        medewerkerIdentificatie = createMedewerkerIdentificatie(
                            identificatie = returningBehandelaar.id
                        )
                    )
                ),
                isZaakspecifiekGeautoriseerd = true
            )
            val createdRollen = mutableListOf<Rol<*>>()
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, returningBehandelaar.id)
            } just runs
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    zaak,
                    currentBehandelaarIdentificatie,
                    reason,
                    any()
                )
            } returns true
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(returningBehandelaar.id, group.name) } just runs
            every { identityService.readUser(returningBehandelaar.id) } returns returningBehandelaar
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.deleteRol(currentBehandelaarRol, reason) } just runs
            every { zrcClientService.createRol(capture(createdRollen), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("the zaak is handed back to the individually authorised medewerker") {
                zaakService.assignZaak(
                    zaak = zaak,
                    groupId = group.name,
                    userName = returningBehandelaar.id,
                    reason = reason
                )

                then("only their behandelaar rol is created, so they do not end up with a second rol") {
                    with(createdRollen.single()) {
                        this.roltype shouldBe behandelaarRolType.url
                        (betrokkeneIdentificatie as MedewerkerIdentificatie).identificatie shouldBe
                            returningBehandelaar.id
                    }
                }

                and("the behandelaar they take over from is granted an individual authorisation") {
                    verify(exactly = 1) {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                            zaak,
                            currentBehandelaarIdentificatie,
                            reason,
                            any()
                        )
                    }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak whose behandelaar was removed outside ZAC") {
            val zaak = createZaak()
            val newBehandelaar = createUser(id = "fakeNewBehandelaarId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val zaakToewijzing = createZaakToewijzing(isZaakspecifiekGeautoriseerd = true)
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, newBehandelaar.id)
            } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(newBehandelaar.id, group.name) } just runs
            every { identityService.readUser(newBehandelaar.id) } returns newBehandelaar
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.createRol(any(), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("a behandelaar is assigned to it") {
                zaakService.assignZaak(
                    zaak = zaak,
                    groupId = group.name,
                    userName = newBehandelaar.id,
                    reason = reason
                )

                then("the behandelaar rol is created and there is nobody to grant an authorisation to") {
                    verify(exactly = 1) { zrcClientService.createRol(any(), reason) }
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(zaak, any(), any(), any())
                        zrcClientService.deleteRol(any<Rol<*>>(), reason)
                    }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak with a behandelaar that the guard refuses to release") {
            val zaak = createZaak()
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val behandelaarRol = createRolMedewerker(zaakURI = zaak.url, rolType = behandelaarRolType)
            val zaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(behandelaarRol),
                isZaakspecifiekGeautoriseerd = true
            )
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, null)
            } throws ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException()

            `when`("the zaak is released") {
                val zaakspecifiekGeautoriseerdeZaakCannotBeReleasedException =
                    shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException> {
                        zaakService.assignZaak(zaak = zaak, groupId = null, userName = null, reason = reason)
                    }

                then("the release is refused with its own error code") {
                    zaakspecifiekGeautoriseerdeZaakCannotBeReleasedException.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_RELEASED
                }

                and("the zaak keeps its behandelaar") {
                    verify(exactly = 0) { zrcClientService.deleteRol(behandelaarRol, any()) }
                }
            }
        }
    }

    context("Concurrent zaak assignment") {
        given("two concurrent requests assigning different users to the same zaak") {
            val zaak = createZaak()
            val firstUser = createUser(id = "fakeUserId1")
            val secondUser = createUser(id = "fakeUserId2")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val zaakToewijzing = createZaakToewijzing()
            val reason = "fakeReason"
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns zaakToewijzing
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(zaakToewijzing, any()) } just runs
            every { zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak) } just runs
            every { identityService.validateIfUserIsInGroup(any(), group.name) } just runs
            every { identityService.readUser(firstUser.id) } returns firstUser
            every { identityService.readUser(secondUser.id) } returns secondUser
            every { identityService.readGroup(group.name) } returns group
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.createRol(any(), reason) } returns createRolMedewerker()
            every { zrcClientService.updateRol(zaak, any(), reason) } just runs
            every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
            every { indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false) } just runs

            `when`("both requests are processed concurrently") {
                val firstThread = Thread {
                    zaakService.assignZaak(
                        zaak = zaak,
                        groupId = group.name,
                        userName = firstUser.id,
                        reason = reason
                    )
                }
                val secondThread = Thread {
                    zaakService.assignZaak(
                        zaak = zaak,
                        groupId = group.name,
                        userName = secondUser.id,
                        reason = reason
                    )
                }
                firstThread.start()
                secondThread.start()
                firstThread.join(5000)
                secondThread.join(5000)

                then("both calls complete without deadlock and each creates exactly one behandelaar rol") {
                    verify(exactly = 2) { zrcClientService.createRol(any(), reason) }
                }
            }
        }
    }

    context("Assigning zaken in a batch") {
        given("two open zaken, a group that is authorised for their zaaktype and a user") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val zaken = listOf(createZaak(zaaktypeUri = zaaktype.url), createZaak(zaaktypeUri = zaaktype.url))
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaaktype.url)
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach { zaak ->
                every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns createZaakToewijzing()
                every {
                    zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)
                } just runs
                every { zrcClientService.updateRol(zaak, any(), explanation) } just runs
                every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
                every {
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                } just runs
            }
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), user.id) } just runs
            every { zrcClientService.createRol(any(), explanation) } returns createRolMedewerker()
            every { zgwApiService.readBehandelaarRoltype(zaaktype.url) } returns behandelaarRolType
            every { identityService.isUserInGroup(user.id, group.name) } returns true
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                    applicationRole = "behandelaar",
                    zaaktypeDescription = zaaktype.omschrijving
                )
            } returns listOf(createPabcGroupRepresentation(name = group.name, description = group.description))
            every { eventingService.send(capture(screenEventSlot)) } just runs

            `when`("the zaken are assigned to the group and the user") {
                zaakService.assignZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                then("every zaak gets a groep rol and a behandelaar rol") {
                    zaken.forEach { verify(exactly = 1) { zrcClientService.updateRol(it, any(), explanation) } }
                    verify(exactly = 2) { zrcClientService.createRol(any(), explanation) }
                }

                and("a 'zaken verdelen' screen event reports the batch as updated") {
                    with(screenEventSlot.captured) {
                        opcode shouldBe Opcode.UPDATED
                        objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                        objectId.resource shouldBe screenEventResourceId
                    }
                }
            }
        }

        given(
            """
            a zaakspecifiek geautoriseerde zaak whose zaaktype does not define the zaakspecifiek geautoriseerde
            medewerker roltype, an ordinary open zaak, a group and a user
            """
        ) {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val markedZaak = createZaak(zaaktypeUri = zaaktype.url)
            val ordinaryZaak = createZaak(zaaktypeUri = zaaktype.url)
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            listOf(markedZaak, ordinaryZaak).forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(markedZaak) } returns createZaakToewijzing(
                behandelaarRollen = listOf(createRolMedewerker()),
                isZaakspecifiekGeautoriseerd = true
            )
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(ordinaryZaak) } returns createZaakToewijzing()
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), user.id) } just runs
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    zaak = markedZaak,
                    medewerker = any(),
                    reason = explanation,
                    zaakspecifiekGeautoriseerdeMedewerkers = any()
                )
            } throws ZaakspecifiekGeautoriseerdeMedewerkerRoltypeNotFoundException("fakeMessage")
            every { zrcClientService.createRol(any(), explanation) } returns createRolMedewerker()
            every { zrcClientService.updateRol(ordinaryZaak, any(), explanation) } just runs
            every { zgwApiService.readBehandelaarRoltype(zaaktype.url) } returns createBehandelaarRolType(
                zaakTypeUri = zaaktype.url
            )
            every { bpmnService.isZaakProcessDriven(ordinaryZaak.uuid) } returns false
            every {
                indexingService.indexeerDirect(ordinaryZaak.uuid.toString(), ZoekObjectType.ZAAK, false)
            } just runs
            every {
                zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(ordinaryZaak)
            } just runs
            every { identityService.isUserInGroup(user.id, group.name) } returns true
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(createPabcGroupRepresentation(name = group.name, description = group.description))
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are assigned to the group and the user") {
                zaakService.assignZaken(
                    zaakUUIDs = listOf(markedZaak.uuid, ordinaryZaak.uuid),
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                then("the marked zaak is reported as skipped and none of its rollen is changed") {
                    verify(exactly = 1) { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(markedZaak)) }
                    verify(exactly = 0) {
                        zrcClientService.updateRol(markedZaak, any(), explanation)
                        zrcClientService.deleteRol(any<Rol<*>>(), explanation)
                    }
                }
                and("the ordinary zaak is still assigned") {
                    verify(exactly = 1) { zrcClientService.updateRol(ordinaryZaak, any(), explanation) }
                }
                and("a 'zaken verdelen' screen event reports the batch as updated") {
                    verify(exactly = 1) {
                        eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(screenEventResourceId))
                    }
                }
                and("the group membership of the user is not validated again for every zaak") {
                    verify(exactly = 0) { identityService.validateIfUserIsInGroup(any(), any()) }
                }
            }
        }

        given("one open and one closed zaak, a group that is authorised for their zaaktype and a user") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val openZaak = createZaak(zaaktypeUri = zaaktype.url)
            val closedZaak = createZaak(archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN)
            val user = createUser(id = "fakeUserId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaaktype.url)
            listOf(openZaak, closedZaak).forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(openZaak) } returns createZaakToewijzing()
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), user.id) } just runs
            every {
                zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(openZaak)
            } just runs
            every { zgwApiService.readBehandelaarRoltype(openZaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.createRol(any(), explanation) } returns createRolMedewerker()
            every { zrcClientService.updateRol(openZaak, any(), explanation) } just runs
            every { bpmnService.isZaakProcessDriven(openZaak.uuid) } returns false
            every {
                indexingService.indexeerDirect(openZaak.uuid.toString(), ZoekObjectType.ZAAK, false)
            } just runs
            every { identityService.isUserInGroup(user.id, group.name) } returns true
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(createPabcGroupRepresentation(name = group.name, description = group.description))
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are assigned to the group and the user") {
                zaakService.assignZaken(
                    zaakUUIDs = listOf(openZaak.uuid, closedZaak.uuid),
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                then("only the open zaak is assigned and the closed one is reported as skipped") {
                    verify(exactly = 1) {
                        zrcClientService.updateRol(openZaak, any(), explanation)
                        eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(closedZaak))
                        eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(screenEventResourceId))
                    }
                }
            }
        }

        given("an open zaak and a group that is not authorised for its zaaktype") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val zaak = createZaak(zaaktypeUri = zaaktype.url)
            val group = createGroup(id = "fakeGroupId")
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(createPabcGroupRepresentation(name = "fakeOtherGroupId", description = "fakeOtherGroup"))
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaak is assigned to that group") {
                zaakService.assignZaken(
                    zaakUUIDs = listOf(zaak.uuid),
                    explanation = explanation,
                    group = group,
                    screenEventResourceId = screenEventResourceId
                )

                then("the zaak is reported as skipped and is never assigned") {
                    verify(exactly = 1) { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaak)) }
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak)
                        zrcClientService.updateRol(zaak, any(), any())
                    }
                }
            }
        }

        given("a list of zaken and a ZRC client that fails while reading the second zaak") {
            val zaken = listOf(createZaak(), createZaak())
            val group = createGroup(id = "fakeGroupId")
            every { zrcClientService.readZaak(zaken[0].uuid) } returns zaken[0]
            every { zrcClientService.readZaak(zaken[1].uuid) } throws RuntimeException("fakeRuntimeException")

            `when`("the zaken are assigned to the group") {
                val runtimeException = shouldThrow<RuntimeException> {
                    zaakService.assignZaken(
                        zaakUUIDs = zaken.map { it.uuid },
                        explanation = explanation,
                        group = group,
                        screenEventResourceId = screenEventResourceId
                    )
                }

                then("the failure is passed on to the caller") {
                    runtimeException.message shouldBe "fakeRuntimeException"
                }

                and("neither zaak is assigned and no screen event is sent for the batch") {
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.readZaakToewijzing(zaken[0])
                        zrcClientService.updateRol(any(), any(), explanation)
                        eventingService.send(ScreenEventType.ZAKEN_VERDELEN.updated(screenEventResourceId))
                    }
                }
            }
        }

        given("two open zaken with a behandelaar, and a group but no user") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val zaken = listOf(createZaak(zaaktypeUri = zaaktype.url), createZaak(zaaktypeUri = zaaktype.url))
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaaktype.url)
            zaken.forEach { zaak ->
                every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns createZaakToewijzing(
                    behandelaarRollen = listOf(
                        createRolMedewerker(zaakURI = zaak.url, rolType = behandelaarRolType)
                    )
                )
                every {
                    zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)
                } just runs
                every { zrcClientService.updateRol(zaak, any(), explanation) } just runs
                every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
                every {
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                } just runs
            }
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), null) } just runs
            every { zrcClientService.deleteRol(any<Rol<*>>(), explanation) } just runs
            every { zgwApiService.readBehandelaarRoltype(zaaktype.url) } returns behandelaarRolType
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(createPabcGroupRepresentation(name = group.name, description = group.description))
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are assigned to the group only") {
                zaakService.assignZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    screenEventResourceId = screenEventResourceId
                )

                then("every zaak gets the new groep and loses its behandelaar") {
                    zaken.forEach { verify(exactly = 1) { zrcClientService.updateRol(it, any(), explanation) } }
                    verify(exactly = 2) { zrcClientService.deleteRol(any<Rol<*>>(), explanation) }
                    verify(exactly = 0) { zrcClientService.createRol(any(), explanation) }
                }
            }
        }

        given("a list of zaken and a user that is not a member of the group") {
            val zaken = listOf(createZaak(), createZaak())
            val group = createGroup(id = "fakeGroupId")
            val user = createUser(id = "fakeUserId")
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach { every { zrcClientService.readZaak(it.uuid) } returns it }
            every { identityService.isUserInGroup(user.id, group.name) } returns false
            every { eventingService.send(capture(screenEventSlot)) } just runs

            `when`("the zaken are assigned to that group and user") {
                zaakService.assignZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                then("every zaak is reported as skipped") {
                    verify(exactly = 1) {
                        eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaken[0]))
                        eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(zaken[1]))
                    }
                }

                and("the batch itself is reported as skipped") {
                    with(screenEventSlot.captured) {
                        opcode shouldBe Opcode.SKIPPED
                        objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                        objectId.resource shouldBe screenEventResourceId
                    }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde and an ordinary open zaak, a group and a user") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val markedZaak = createZaak(zaaktypeUri = zaaktype.url)
            val ordinaryZaak = createZaak(zaaktypeUri = zaaktype.url)
            val user = createUser(id = "fakeNewBehandelaarId")
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaaktype.url)
            val previousBehandelaarIdentificatie = createMedewerkerIdentificatie(
                identificatie = "fakePreviousBehandelaarId"
            )
            listOf(markedZaak, ordinaryZaak).forEach { zaak ->
                every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                every {
                    zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)
                } just runs
                every { zrcClientService.updateRol(zaak, any(), explanation) } just runs
                every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns false
                every {
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                } just runs
            }
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(markedZaak) } returns createZaakToewijzing(
                behandelaarRollen = listOf(
                    createRolMedewerker(
                        zaakURI = markedZaak.url,
                        rolType = behandelaarRolType,
                        medewerkerIdentificatie = previousBehandelaarIdentificatie
                    )
                ),
                isZaakspecifiekGeautoriseerd = true
            )
            every {
                zaakspecifiekeAutorisatieService.readZaakToewijzing(ordinaryZaak)
            } returns createZaakToewijzing()
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), user.id) } just runs
            every {
                zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                    markedZaak,
                    previousBehandelaarIdentificatie,
                    explanation,
                    any()
                )
            } returns true
            every { zgwApiService.readBehandelaarRoltype(zaaktype.url) } returns behandelaarRolType
            every { zrcClientService.deleteRol(any<Rol<*>>(), explanation) } just runs
            every { zrcClientService.createRol(any(), explanation) } returns createRolMedewerker()
            every { identityService.isUserInGroup(user.id, group.name) } returns true
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(createPabcGroupRepresentation(name = group.name, description = group.description))
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are assigned to the group and the user") {
                zaakService.assignZaken(
                    zaakUUIDs = listOf(markedZaak.uuid, ordinaryZaak.uuid),
                    explanation = explanation,
                    group = group,
                    user = user,
                    screenEventResourceId = screenEventResourceId
                )

                then("the marked zaak is handed over instead of being skipped") {
                    verify(exactly = 1) {
                        zrcClientService.updateRol(markedZaak, any(), explanation)
                        zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatie(
                            markedZaak,
                            previousBehandelaarIdentificatie,
                            explanation,
                            any()
                        )
                    }
                    verify(exactly = 0) { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(markedZaak)) }
                }

                and("the ordinary zaak in the same batch is assigned as well") {
                    verify(exactly = 1) { zrcClientService.updateRol(ordinaryZaak, any(), explanation) }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde and an ordinary open zaak, and a group but no user") {
            val zaaktypeUUID = UUID.randomUUID()
            val zaaktype = createZaakType(uri = URI.create("https://ztc/zaaktypen/$zaaktypeUUID"))
            val markedZaak = createZaak(zaaktypeUri = zaaktype.url)
            val ordinaryZaak = createZaak(zaaktypeUri = zaaktype.url)
            val group = createGroup(id = "fakeGroupId")
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaaktype.url)
            val markedZaakBehandelaarRol = createRolMedewerker(
                zaakURI = markedZaak.url,
                rolType = behandelaarRolType
            )
            val markedZaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(markedZaakBehandelaarRol),
                isZaakspecifiekGeautoriseerd = true
            )
            val ordinaryZaakToewijzing = createZaakToewijzing()
            listOf(markedZaak, ordinaryZaak).forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(markedZaak) } returns markedZaakToewijzing
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(ordinaryZaak) } returns ordinaryZaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(markedZaakToewijzing, null)
            } throws ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException()
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(ordinaryZaakToewijzing, null)
            } just runs
            every { zgwApiService.readBehandelaarRoltype(ordinaryZaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.updateRol(ordinaryZaak, any(), explanation) } just runs
            every { bpmnService.isZaakProcessDriven(ordinaryZaak.uuid) } returns false
            every {
                indexingService.indexeerDirect(ordinaryZaak.uuid.toString(), ZoekObjectType.ZAAK, false)
            } just runs
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaaktype
            every {
                pabcClientService.getGroupsByApplicationRoleAndZaaktype("behandelaar", zaaktype.omschrijving)
            } returns listOf(createPabcGroupRepresentation(name = group.name, description = group.description))
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are assigned to the group only") {
                zaakService.assignZaken(
                    zaakUUIDs = listOf(markedZaak.uuid, ordinaryZaak.uuid),
                    explanation = explanation,
                    group = group,
                    screenEventResourceId = screenEventResourceId
                )

                then("the marked zaak keeps its behandelaar and is reported as skipped") {
                    verify(exactly = 1) { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(markedZaak)) }
                    verify(exactly = 0) {
                        zrcClientService.updateRol(markedZaak, any(), explanation)
                        zrcClientService.deleteRol(markedZaakBehandelaarRol, any())
                    }
                }

                and("the ordinary zaak in the same batch is still assigned to the group") {
                    verify(exactly = 1) { zrcClientService.updateRol(ordinaryZaak, any(), explanation) }
                }
            }
        }
    }

    context("Releasing zaken") {
        given("two open zaken with a behandelaar and a screen event resource id") {
            val zaken = listOf(createZaak(), createZaak())
            val behandelaarRolPerZaak = zaken.associateWith {
                createRolMedewerker(
                    zaakURI = it.url,
                    rolType = createBehandelaarRolType(zaakTypeUri = it.zaaktype)
                )
            }
            val screenEventSlot = slot<ScreenEvent>()
            zaken.forEach { zaak ->
                every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                every { zaakspecifiekeAutorisatieService.readZaakToewijzing(zaak) } returns createZaakToewijzing(
                    behandelaarRollen = listOf(behandelaarRolPerZaak.getValue(zaak))
                )
                every {
                    zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(zaak)
                } just runs
                every { zrcClientService.deleteRol(behandelaarRolPerZaak.getValue(zaak), explanation) } just runs
                every { bpmnService.isZaakProcessDriven(zaak.uuid) } returns true
                every { zaakVariabelenService.removeUser(zaak.uuid) } just runs
                every {
                    indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
                } just runs
            }
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), null) } just runs
            every { eventingService.send(capture(screenEventSlot)) } just runs

            `when`("the zaken are released") {
                zaakService.releaseZaken(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    screenEventResourceId = screenEventResourceId
                )

                then("every zaak loses its behandelaar rol while its groep is left untouched") {
                    zaken.forEach {
                        verify(exactly = 1) {
                            zrcClientService.deleteRol(behandelaarRolPerZaak.getValue(it), explanation)
                            zaakVariabelenService.removeUser(it.uuid)
                        }
                        verify(exactly = 0) { zrcClientService.updateRol(it, any(), any()) }
                    }
                }

                and("a 'zaken vrijgeven' screen event reports the batch as updated") {
                    with(screenEventSlot.captured) {
                        opcode shouldBe Opcode.UPDATED
                        objectType shouldBe ScreenEventType.ZAKEN_VRIJGEVEN
                        objectId.resource shouldBe screenEventResourceId
                    }
                }
            }
        }

        given("one open and one closed zaak to release") {
            val openZaak = createZaak()
            val closedZaak = createZaak(archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN)
            val behandelaarRol = createRolMedewerker(
                zaakURI = openZaak.url,
                rolType = createBehandelaarRolType(zaakTypeUri = openZaak.zaaktype)
            )
            listOf(openZaak, closedZaak).forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(openZaak) } returns createZaakToewijzing(
                behandelaarRollen = listOf(behandelaarRol)
            )
            every { zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(any(), null) } just runs
            every {
                zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(openZaak)
            } just runs
            every { zrcClientService.deleteRol(behandelaarRol, explanation) } just runs
            every { bpmnService.isZaakProcessDriven(openZaak.uuid) } returns false
            every {
                indexingService.indexeerDirect(openZaak.uuid.toString(), ZoekObjectType.ZAAK, false)
            } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are released") {
                zaakService.releaseZaken(
                    zaakUUIDs = listOf(openZaak.uuid, closedZaak.uuid),
                    explanation = explanation,
                    screenEventResourceId = screenEventResourceId
                )

                then("only the open zaak is released and the closed one is reported as skipped") {
                    verify(exactly = 1) {
                        zrcClientService.deleteRol(behandelaarRol, explanation)
                        eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(closedZaak))
                        eventingService.send(ScreenEventType.ZAKEN_VRIJGEVEN.updated(screenEventResourceId))
                    }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde and an ordinary open zaak to release") {
            val markedZaak = createZaak()
            val ordinaryZaak = createZaak()
            val markedZaakBehandelaarRol = createRolMedewerker(
                zaakURI = markedZaak.url,
                rolType = createBehandelaarRolType(zaakTypeUri = markedZaak.zaaktype)
            )
            val markedZaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(markedZaakBehandelaarRol),
                isZaakspecifiekGeautoriseerd = true
            )
            val ordinaryBehandelaarRol = createRolMedewerker(
                zaakURI = ordinaryZaak.url,
                rolType = createBehandelaarRolType(zaakTypeUri = ordinaryZaak.zaaktype)
            )
            val ordinaryZaakToewijzing = createZaakToewijzing(
                behandelaarRollen = listOf(ordinaryBehandelaarRol)
            )
            listOf(markedZaak, ordinaryZaak).forEach {
                every { zrcClientService.readZaak(it.uuid) } returns it
            }
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(markedZaak) } returns markedZaakToewijzing
            every { zaakspecifiekeAutorisatieService.readZaakToewijzing(ordinaryZaak) } returns ordinaryZaakToewijzing
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(markedZaakToewijzing, null)
            } throws ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException()
            every {
                zaakspecifiekeAutorisatieService.assertBehandelaarMayChange(ordinaryZaakToewijzing, null)
            } just runs
            every {
                zaakspecifiekeAutorisatieService.reindexZaakspecifiekeAutorisatieDependents(ordinaryZaak)
            } just runs
            every { zrcClientService.deleteRol(ordinaryBehandelaarRol, explanation) } just runs
            every { bpmnService.isZaakProcessDriven(ordinaryZaak.uuid) } returns false
            every {
                indexingService.indexeerDirect(ordinaryZaak.uuid.toString(), ZoekObjectType.ZAAK, false)
            } just runs
            every { eventingService.send(any<ScreenEvent>()) } just runs

            `when`("the zaken are released") {
                zaakService.releaseZaken(
                    zaakUUIDs = listOf(markedZaak.uuid, ordinaryZaak.uuid),
                    explanation = explanation,
                    screenEventResourceId = screenEventResourceId
                )

                then("the marked zaak keeps its behandelaar and is reported as skipped") {
                    verify(exactly = 1) { eventingService.send(ScreenEventType.ZAAK_ROLLEN.skipped(markedZaak)) }
                    verify(exactly = 0) { zrcClientService.deleteRol(markedZaakBehandelaarRol, any()) }
                }

                and("the ordinary zaak in the same batch is still released") {
                    verify(exactly = 1) { zrcClientService.deleteRol(ordinaryBehandelaarRol, explanation) }
                }
            }
        }
    }

    context("Add betrokkenen to zaak") {
        given("A zaak without any betrokkenen") {
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

            `when`("a betrokkene of type natuurlijk persoon is added") {
                zaakService.addBetrokkeneToZaak(
                    roleTypeUUID = roleTypeUUID,
                    identificationType = IdentificatieType.BSN,
                    identification = "fakeBSN",
                    zaak = zaak,
                    explanation = explanation
                )

                then("the betrokkene is successfully added to the zaak") {
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

        given("A zaak with a betrokkenen of type natuurlijk persoon and role type 'adviseur'") {
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

            `when`("the same betrokkene is added again but with the role type 'belanghebbende'") {
                zaakService.addBetrokkeneToZaak(
                    roleTypeUUID = roleTypeUUID,
                    identificationType = IdentificatieType.BSN,
                    identification = identification,
                    zaak = zaak,
                    explanation = explanation
                )

                then("the betrokkene is added to the zaak again with role type 'belanghebbende'") {
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

        given("A zaak with a betrokkenen of type natuurlijk persoon and role type adviseur") {
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

            `when`("the same betrokkene is added again with the same role type") {
                val exception = shouldThrow<BetrokkeneIsAlreadyAddedToZaakException> {
                    zaakService.addBetrokkeneToZaak(
                        roleTypeUUID = roleTypeUUID,
                        identificationType = IdentificatieType.BSN,
                        identification = identification,
                        zaak = zaak,
                        explanation = explanation
                    )
                }

                then("an exception is thrown and the betrokkene is not added to the zaak again") {
                    exception.message shouldBe "Betrokkene with type 'BSN' and identification 'fakeBSN' " +
                        "was already added to the zaak with UUID '${zaak.uuid}'. Ignoring."
                    verify(exactly = 0) {
                        zrcClientService.createRol(any(), any())
                    }
                }
            }
        }
    }

    context("List betrokkenen for zaak") {
        given(
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

            `when`("the list of betrokkenen is retrieved") {
                val betrokkenenRoles = zaakService.listBetrokkenenforZaak(zaak)

                then("the list should consist of the two betrokkenen not of type initiator or behandelaar") {
                    betrokkenenRoles.size shouldBe 2
                    betrokkenenRoles[0] shouldBe rolNatuurlijkPersonen[0]
                    betrokkenenRoles[1] shouldBe rolNatuurlijkPersonen[1]
                }
            }
        }
    }

    context("Set ontvangstbevestiging verstuurd") {
        given("a zaak that is not heropend") {
            val zaakUuid = UUID.randomUUID()
            val statusUuid = UUID.randomUUID()
            val zaak = createZaak(
                uuid = zaakUuid,
                status = URI(statusUuid.toString())
            )
            val statusType = createStatusType().apply {
                omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING
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
            every { eventingService.send(any<ScreenEvent>()) } just Runs

            `when`("setOntvangstbevestigingVerstuurdIfNotHeropend is called") {
                zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)

                then("ontvangstbevestiging is true") {
                    verify(exactly = 1) {
                        zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true)
                    }
                }

                and("a zaak screen event is sent so that all open screens of this zaak are refreshed") {
                    verify(exactly = 1) {
                        eventingService.send(ScreenEventType.ZAAK.updated(zaakUuid))
                    }
                }
            }
        }

        given("a zaak is heropend") {
            val zaakUuid = UUID.randomUUID()
            val statusUuid = UUID.randomUUID()
            val zaak = createZaak(
                uuid = zaakUuid,
                status = URI(statusUuid.toString())
            )
            val statusType = createStatusType().apply {
                omschrijving = ConfigurationService.STATUSTYPE_OMSCHRIJVING_HEROPEND
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

            `when`("setOntvangstbevestigingVerstuurdIfNotHeropend is called") {
                zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)

                then("ontvangstbevestiging is not set") {
                    verify(exactly = 0) {
                        zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, any())
                    }
                }

                and("no zaak screen event is sent") {
                    verify(exactly = 0) {
                        eventingService.send(any<ScreenEvent>())
                    }
                }
            }
        }
    }

    context("Add initiator to zaak") {
        given("An existing zaak and a vestiging identification with a KVK number and a vestigingsnummer") {
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

            `when`("an initiator of type vestiging is added to the zaak") {
                zaakService.addInitiatorToZaak(
                    identificationType = IdentificatieType.VN,
                    identification = identification,
                    zaak = zaak,
                    explanation = explanation
                )

                then(
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

        given("An existing zaak and an identication of KVK number") {
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

            `when`("an initiator of type rechtspersoon (RSIN) is added to the zaak") {
                zaakService.addInitiatorToZaak(
                    identificationType = IdentificatieType.RSIN,
                    identification = kvkNummer,
                    zaak = zaak,
                    explanation = explanation
                )

                then("an initiator role of type niet-natuurlijk persoon with a KVK number is added to the zaak") {
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

    context("Retrieve zaak and zaaktype by zaak ID") {
        given("A valid zaak ID") {
            val zaakID = "fakeZaakID"
            val zaak = createZaak(identificatie = zaakID)
            val zaakType = createZaakType()
            every { zrcClientService.readZaakByID(zaakID) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

            `when`("readZaakAndZaakTypeByZaakID is called") {
                val result = zaakService.readZaakAndZaakTypeByZaakID(zaakID)

                then("it should return the correct zaak and zaaktype") {
                    result.first shouldBe zaak
                    result.second shouldBe zaakType
                }
            }
        }
    }

    context("Retrieve zaak and zaaktype by zaak UUID") {
        given("A valid zaak UUID") {
            val zaakUUID = UUID.randomUUID()
            val zaak = createZaak(uuid = zaakUUID)
            val zaakType = createZaakType()
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

            `when`("readZaakAndZaakTypeByZaakUUID is called") {
                val result = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)

                then("it should return the correct zaak and zaaktype") {
                    result.first shouldBe zaak
                    result.second shouldBe zaakType
                }
            }
        }
    }

    context("Retrieve zaaktype by zaak") {
        given("A zaak with a valid zaaktype") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType

            `when`("retrieveZaakTypeByZaak is called") {
                val result = zaakService.readZaakTypeByZaak(zaak)

                then("it should return the correct zaaktype") {
                    result shouldBe zaakType
                }
            }
        }
    }

    context("Retrieve zaaktype by UUID") {
        given("A zaaktype UUID") {
            val zaakTypeUUID = UUID.randomUUID()
            val zaakType = createZaakType()
            every { ztcClientService.readZaaktype(zaakTypeUUID) } returns zaakType

            `when`("readZaakTypeByUUID is called") {
                val result = zaakService.readZaakTypeByUUID(zaakTypeUUID)

                then("it should return the correct zaaktype") {
                    result shouldBe zaakType
                }
            }
        }
    }

    context("Listing status types for a zaaktype") {
        given("a zaak with status types") {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val statusTypes = listOf(
                createStatusType(omschrijving = "first"),
                createStatusType(omschrijving = "second")
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readStatustypen(zaakType.url) } returns statusTypes

            `when`("list of zaak status types is requested") {
                val statusTypeData = zaakService.listStatusTypes(zaaktypeUuid)

                then("correct status type data is returned") {
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

    context("Listing result types for a zaaktype") {
        given("a zaak with result types that do not derive their date from an eigenschap") {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val resultTypes = listOf(
                createResultaatType(omschrijving = "first"),
                createResultaatType(omschrijving = "second")
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readResultaattypen(zaakType.url) } returns resultTypes
            every { ztcClientService.readEigenschappen(zaakType.url) } returns emptyList()

            `when`("list of zaak result types is requested") {
                val resultTypeData = zaakService.listResultTypes(zaaktypeUuid)

                then("correct result type data is returned without a datumkenmerk omschrijving") {
                    resultTypeData shouldHaveSize 2
                    with(resultTypeData.first()) {
                        naam shouldBe "first"
                        datumKenmerkOmschrijving shouldBe null
                    }
                    with(resultTypeData.last()) {
                        naam shouldBe "second"
                        datumKenmerkOmschrijving shouldBe null
                    }
                }
            }
        }

        given(
            """a zaak with a result type that derives its date from an eigenschap
            matching one of the zaaktype's eigenschappen"""
        ) {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val matchingEigenschap = createEigenschap(
                naam = "fakeDatumkenmerk",
                definitie = "fakeEigenschapDefinitie",
                zaaktype = zaakType.url
            )
            val resultType = createResultaatType(
                omschrijving = "fakeResultaatTypeOmschrijving",
                brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                    afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
                    datumkenmerk = "fakeDatumkenmerk"
                )
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readResultaattypen(zaakType.url) } returns listOf(resultType)
            every { ztcClientService.readEigenschappen(zaakType.url) } returns listOf(matchingEigenschap)

            `when`("list of zaak result types is requested") {
                val resultTypeData = zaakService.listResultTypes(zaaktypeUuid)

                then("the datumkenmerk omschrijving is taken from the matching eigenschap's definitie") {
                    resultTypeData shouldHaveSize 1
                    resultTypeData.first().datumKenmerkOmschrijving shouldBe "fakeEigenschapDefinitie"
                }
            }
        }

        given(
            """a zaak with a result type that derives its date from an eigenschap
            not matching any of the zaaktype's eigenschappen"""
        ) {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val nonMatchingEigenschap = createEigenschap(
                naam = "fakeOtherDatumkenmerk",
                definitie = "fakeEigenschapDefinitie",
                zaaktype = zaakType.url
            )
            val resultType = createResultaatType(
                omschrijving = "fakeResultaatTypeOmschrijving",
                brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                    afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
                    datumkenmerk = "fakeDatumkenmerk"
                )
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readResultaattypen(zaakType.url) } returns listOf(resultType)
            every { ztcClientService.readEigenschappen(zaakType.url) } returns listOf(nonMatchingEigenschap)

            `when`("list of zaak result types is requested") {
                val resultTypeData = zaakService.listResultTypes(zaaktypeUuid)

                then("no datumkenmerk omschrijving is set") {
                    resultTypeData shouldHaveSize 1
                    resultTypeData.first().datumKenmerkOmschrijving shouldBe null
                }
            }
        }

        given(
            """a zaak with a result type that derives its date from an eigenschap
            matching one of the zaaktype's eigenschappen but with a blank definitie"""
        ) {
            val zaak = createZaak()
            val zaaktypeUuid = zaak.zaaktype.extractUuid()
            val zaakType = createZaakType()
            val matchingEigenschapWithBlankDefinitie = createEigenschap(
                naam = "fakeDatumkenmerk",
                definitie = " ",
                zaaktype = zaakType.url
            )
            val resultType = createResultaatType(
                omschrijving = "fakeResultaatTypeOmschrijving",
                brondatumArchiefprocedure = createBrondatumArchiefprocedure(
                    afleidingswijze = AfleidingswijzeEnum.EIGENSCHAP,
                    datumkenmerk = "fakeDatumkenmerk"
                )
            )

            every { ztcClientService.readZaaktype(zaaktypeUuid) } returns zaakType
            every { ztcClientService.readResultaattypen(zaakType.url) } returns listOf(resultType)
            every {
                ztcClientService.readEigenschappen(zaakType.url)
            } returns listOf(matchingEigenschapWithBlankDefinitie)

            `when`("list of zaak result types is requested") {
                val resultTypeData = zaakService.listResultTypes(zaaktypeUuid)

                then("no datumkenmerk omschrijving is set") {
                    resultTypeData shouldHaveSize 1
                    resultTypeData.first().datumKenmerkOmschrijving shouldBe null
                }
            }
        }
    }
})
