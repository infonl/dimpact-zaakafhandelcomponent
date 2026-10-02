/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.event.SignaleringEventId
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createOrganisatorischeEenheidIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolOrganisatorischeEenheid
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForReads
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolListParameters
import nl.info.client.zgw.ztc.model.createBehandelaarRolType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakspecifiekGeautoriseerdeMedewerkerRolType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringInstellingen
import nl.info.zac.signalering.model.createSignaleringType
import nl.info.test.org.flowable.task.api.createTestTask
import java.net.URI
import java.util.UUID

class SignaleringEventObserverTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerTest
    afterEach { checkUnnecessaryStub() }

    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val identityService = mockk<IdentityService>()
    val signaleringService = mockk<SignaleringService>()
    val signaleringEventObserver = SignaleringEventObserver(
        zgwApiService,
        zrcClientService,
        flowableTaskService,
        identityService,
        signaleringService
    )

    context("Receiving a zaak op naam event for a newly created rol") {
        val zaak = createZaak()
        val rolURI = URI("https://example.com/rol/fakeRolUuid")
        val signaleringEvent = SignaleringEvent(
            SignaleringType.Type.ZAAK_OP_NAAM,
            SignaleringEventId(rolURI, null),
            null
        )

        given("a behandelaar rol for a medewerker") {
            val user = createUser(id = "fakeBehandelaarId")
            val rolMedewerker = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype),
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = user.id)
            )
            val storedSignalering = slot<Signalering>()
            every { zrcClientService.readRol(rolURI) } returns rolMedewerker
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every {
                signaleringService.signaleringInstance(SignaleringType.Type.ZAAK_OP_NAAM)
            } returns createSignalering(zaak = null)
            every { identityService.readUser(user.id) } returns user
            every { signaleringService.isNecessary(any(), null) } returns true
            every { signaleringService.readInstellingen(any()) } returns createSignaleringInstellingen(
                isDashboard = true,
                isMail = false
            )
            every { signaleringService.storeSignalering(capture(storedSignalering)) } answers { firstArg() }

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("the medewerker is signalled that the zaak is on their name") {
                    with(storedSignalering.captured) {
                        targettype shouldBe SignaleringTarget.USER
                        target shouldBe user.id
                        subject shouldBe zaak.uuid.toString()
                    }
                }
            }
        }

        given("a behandelaar rol for an organisatorische eenheid, on a zaak without a behandelaar medewerker") {
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val group = createGroup(id = "fakeGroupId")
            val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                organisatorischeEenheidIdentificatie = createOrganisatorischeEenheidIdentificatie(
                    identificatie = group.name
                )
            )
            val storedSignalering = slot<Signalering>()
            every { zrcClientService.readRol(rolURI) } returns rolOrganisatorischeEenheid
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(emptyList<Rol<*>>(), 0)
            every {
                signaleringService.signaleringInstance(SignaleringType.Type.ZAAK_OP_NAAM)
            } returns createSignalering(zaak = null)
            every { identityService.readGroup(group.name) } returns group
            every { signaleringService.isNecessary(any(), null) } returns true
            every { signaleringService.readInstellingen(any()) } returns createSignaleringInstellingen(
                isDashboard = true,
                isMail = false
            )
            every { signaleringService.storeSignalering(capture(storedSignalering)) } answers { firstArg() }

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("the group is signalled that the zaak is on its name") {
                    with(storedSignalering.captured) {
                        targettype shouldBe SignaleringTarget.GROUP
                        target shouldBe group.name
                        subject shouldBe zaak.uuid.toString()
                    }
                }
            }
        }

        given("a behandelaar rol for an organisatorische eenheid, on a zaak that also has a behandelaar medewerker") {
            val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            val rolOrganisatorischeEenheid = createRolOrganisatorischeEenheid(
                zaakURI = zaak.url,
                rolType = behandelaarRolType
            )
            val rolMedewerker = createRolMedewerker(zaakURI = zaak.url, rolType = behandelaarRolType)
            every { zrcClientService.readRol(rolURI) } returns rolOrganisatorischeEenheid
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every {
                zrcClientService.listRollen(any<RolListParameters>())
            } returns Results(listOf<Rol<*>>(rolMedewerker), 1)

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("no signalering is created, because the zaak is on the name of the medewerker") {
                    verify(exactly = 0) {
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde medewerker rol, whose omschrijving generiek is also behandelaar") {
            val rolMedewerker = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = createZaakspecifiekGeautoriseerdeMedewerkerRolType(zaakTypeUri = zaak.zaaktype)
            )
            every { zrcClientService.readRol(rolURI) } returns rolMedewerker

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("no signalering is created, so the previous behandelaar is not told the zaak is on their name") {
                    verify(exactly = 0) {
                        zrcClientService.readZaak(any<URI>())
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }

        given("an initiator rol") {
            val rolMedewerker = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.INITIATOR)
            )
            every { zrcClientService.readRol(rolURI) } returns rolMedewerker

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("no signalering is created, because only a behandelaar gets a zaak on their name") {
                    verify(exactly = 0) {
                        zrcClientService.readZaak(any<URI>())
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }

        given("a behandelaar rol for a medewerker who caused the event themselves") {
            val user = createUser(id = "fakeBehandelaarId")
            val rolMedewerker = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype),
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = user.id)
            )
            val signaleringEventByBehandelaar = SignaleringEvent(
                SignaleringType.Type.ZAAK_OP_NAAM,
                SignaleringEventId(rolURI, null),
                user
            )
            every { zrcClientService.readRol(rolURI) } returns rolMedewerker
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every {
                signaleringService.signaleringInstance(SignaleringType.Type.ZAAK_OP_NAAM)
            } returns createSignalering(zaak = null)
            every { identityService.readUser(user.id) } returns user
            every { signaleringService.isNecessary(any(), user.id) } returns false

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEventByBehandelaar)

                then("no signalering is stored or sent, because it is not necessary") {
                    verify(exactly = 0) {
                        signaleringService.readInstellingen(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }

        given("a behandelaar rol for a natuurlijk persoon") {
            val rolNatuurlijkPersoon = createRolNatuurlijkPersoon(
                zaakURI = zaak.url,
                rolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)
            )
            every { zrcClientService.readRol(rolURI) } returns rolNatuurlijkPersoon
            every { zrcClientService.readZaak(zaak.url) } returns zaak

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("no signalering is created, because only a medewerker or a group can be signalled") {
                    verify(exactly = 0) {
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }

        given("reading the rol fails with an error") {
            every { zrcClientService.readRol(rolURI) } throws NotImplementedError("fakeError")

            `when`("the event is handled") {
                val notImplementedError = shouldThrow<NotImplementedError> {
                    signaleringEventObserver.onFire(signaleringEvent)
                }

                then("the error is passed on to the caller, so that the failure of the async event is logged") {
                    notImplementedError.message shouldBe "fakeError"
                }
            }
        }
    }

    context("Receiving a zaak verlopend event") {
        given("an event for a zaak") {
            val signaleringEvent = SignaleringEvent(
                SignaleringType.Type.ZAAK_VERLOPEND,
                SignaleringEventId(createZaak().url, null),
                null
            )

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("it is ignored, because verlopen signaleringen are created by a scheduled job instead") {
                    verify(exactly = 0) {
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }
    }

    context("Receiving a zaak document toegevoegd event") {
        val zaak = createZaak()
        val informatieobjectUUID = UUID.randomUUID()
        val zaakInformatieobject = createZaakInformatieobjectForReads(
            informatieobject = URI("https://example.com/enkelvoudiginformatieobjecten/$informatieobjectUUID")
        )
        val zaakInformatieobjectUUID = UUID.randomUUID()
        val signaleringEvent = SignaleringEvent(
            SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD,
            SignaleringEventId(zaak.url, URI("https://example.com/zaakinformatieobjecten/$zaakInformatieobjectUUID")),
            null
        )
        val behandelaarRolType = createBehandelaarRolType(zaakTypeUri = zaak.zaaktype)

        given("a zaak with a behandelaar medewerker") {
            val user = createUser(id = "fakeBehandelaarId")
            val rolMedewerker = createRolMedewerker(
                zaakURI = zaak.url,
                rolType = behandelaarRolType,
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = user.id)
            )
            val storedSignalering = slot<Signalering>()
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every { zrcClientService.readZaakinformatieobject(zaakInformatieobjectUUID) } returns zaakInformatieobject
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every {
                zrcClientService.listRollen(any<RolListParameters>())
            } returns Results(listOf<Rol<*>>(rolMedewerker), 1)
            every {
                signaleringService.signaleringInstance(SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD)
            } returns createSignalering(
                type = createSignaleringType(
                    type = SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD,
                    subjecttype = SignaleringSubject.ZAAK
                ),
                zaak = null
            )
            every { identityService.readUser(user.id) } returns user
            every { signaleringService.isNecessary(any(), null) } returns true
            every { signaleringService.readInstellingen(any()) } returns createSignaleringInstellingen(
                isDashboard = true,
                isMail = false
            )
            every { signaleringService.storeSignalering(capture(storedSignalering)) } answers { firstArg() }

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("the behandelaar is signalled about the added document") {
                    with(storedSignalering.captured) {
                        targettype shouldBe SignaleringTarget.USER
                        target shouldBe user.id
                        subject shouldBe zaak.uuid.toString()
                        detail shouldBe informatieobjectUUID.toString()
                    }
                }
            }
        }

        given("a zaak without a behandelaar medewerker") {
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every { zrcClientService.readZaakinformatieobject(zaakInformatieobjectUUID) } returns zaakInformatieobject
            every { zgwApiService.readBehandelaarRoltype(zaak.zaaktype) } returns behandelaarRolType
            every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(emptyList<Rol<*>>(), 0)

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("no signalering is created") {
                    verify(exactly = 0) {
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }
    }

    context("Receiving a taak op naam event") {
        given("an event without an actor, for a task with an owner and an assignee") {
            val owner = createUser(id = "fakeOwnerId")
            val assignee = createUser(id = "fakeAssigneeId")
            val task = createTestTask(id = "fakeTaskId", owner = owner.id, assignee = assignee.id)
            val signaleringEvent = SignaleringEvent(
                SignaleringType.Type.TAAK_OP_NAAM,
                SignaleringEventId(task.id, null),
                null
            )
            val sentSignalering = slot<Signalering>()
            every { flowableTaskService.readOpenTask(task.id) } returns task
            every { identityService.readUser(owner.id) } returns owner
            every { identityService.readUser(assignee.id) } returns assignee
            every {
                signaleringService.signaleringInstance(SignaleringType.Type.TAAK_OP_NAAM)
            } returns createSignalering(
                type = createSignaleringType(
                    type = SignaleringType.Type.TAAK_OP_NAAM,
                    subjecttype = SignaleringSubject.TAAK
                ),
                zaak = null
            )
            every { signaleringService.isNecessary(any(), null) } returns true
            every { signaleringService.readInstellingen(any()) } returns createSignaleringInstellingen(
                isDashboard = false,
                isMail = true
            )
            every { signaleringService.sendSignalering(capture(sentSignalering)) } returns Unit

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("the assignee is mailed") {
                    with(sentSignalering.captured) {
                        targettype shouldBe SignaleringTarget.USER
                        target shouldBe assignee.id
                        subject shouldBe task.id
                    }
                }
            }
        }

        given("an event without an actor, for a task without an owner") {
            val assignee = createUser(id = "fakeAssigneeId")
            val task = createTestTask(id = "fakeTaskId", owner = null, assignee = assignee.id)
            val signaleringEvent = SignaleringEvent(
                SignaleringType.Type.TAAK_OP_NAAM,
                SignaleringEventId(task.id, null),
                null
            )
            val sentSignalering = slot<Signalering>()
            every { flowableTaskService.readOpenTask(task.id) } returns task
            every { identityService.readUser(assignee.id) } returns assignee
            every {
                signaleringService.signaleringInstance(SignaleringType.Type.TAAK_OP_NAAM)
            } returns createSignalering(
                type = createSignaleringType(
                    type = SignaleringType.Type.TAAK_OP_NAAM,
                    subjecttype = SignaleringSubject.TAAK
                ),
                zaak = null
            )
            every { signaleringService.isNecessary(any(), null) } returns true
            every { signaleringService.readInstellingen(any()) } returns createSignaleringInstellingen(
                isDashboard = false,
                isMail = true
            )
            every { signaleringService.sendSignalering(capture(sentSignalering)) } returns Unit

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("the assignee is mailed") {
                    sentSignalering.captured.target shouldBe assignee.id
                }
            }
        }

        given("a task without an assignee") {
            val task = createTestTask(id = "fakeTaskId", assignee = null)
            val signaleringEvent = SignaleringEvent(
                SignaleringType.Type.TAAK_OP_NAAM,
                SignaleringEventId(task.id, null),
                createUser()
            )
            every { flowableTaskService.readOpenTask(task.id) } returns task

            `when`("the event is handled") {
                signaleringEventObserver.onFire(signaleringEvent)

                then("no signalering is created") {
                    verify(exactly = 0) {
                        signaleringService.signaleringInstance(any())
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }
    }
})
