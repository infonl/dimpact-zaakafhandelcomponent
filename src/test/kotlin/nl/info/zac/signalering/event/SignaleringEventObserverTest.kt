/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.event.SignaleringEventId
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService.Companion.ROLTYPE_OMSCHRIJVING_BEHANDELAAR
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolListParameters
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringInstellingen
import nl.info.test.org.flowable.task.api.createTestTask
import java.net.URI

class SignaleringEventObserverTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val identityService = mockk<IdentityService>()
    val signaleringService = mockk<SignaleringService>()
    val signaleringEventObserver = SignaleringEventObserver(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        flowableTaskService = flowableTaskService,
        identityService = identityService,
        signaleringService = signaleringService
    )

    afterEach { checkUnnecessaryStub() }

    context("Handling a zaak op naam event") {
        given("a zaak whose behandelaar is a medewerker who wants a mail") {
            val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
            val zaak = createZaak()
            val rolMedewerker = createRolMedewerker(zaakURI = zaak.url, rolType = rolType)
            val rolURI = URI("https://example.com/fakeRol")
            val user = createUser(id = "fakeIdentificatie")
            val signalering = createSignalering()
            val event = SignaleringEvent(
                SignaleringType.Type.ZAAK_OP_NAAM,
                SignaleringEventId(rolURI, null),
                null
            )
            var wasSystemUserWhileSending: Boolean? = null
            every { zrcClientService.readRol(rolURI) } returns rolMedewerker
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every { signaleringService.signaleringInstance(SignaleringType.Type.ZAAK_OP_NAAM) } returns signalering
            every { identityService.readUser("fakeIdentificatie") } returns user
            every { signaleringService.isNecessary(signalering, null) } returns true
            every {
                signaleringService.readInstellingen(signalering)
            } returns createSignaleringInstellingen(isDashboard = false, isMail = true)
            every { signaleringService.sendSignalering(signalering) } answers {
                wasSystemUserWhileSending = LoggedInUserProvider.systemUser.get()
            }

            `when`("the event is observed") {
                signaleringEventObserver.onFire(event)

                then("a mail is sent to the behandelaar") {
                    signalering.target shouldBe "fakeIdentificatie"
                    verify(exactly = 1) { signaleringService.sendSignalering(signalering) }
                }

                and("it is sent as the system user, which is no longer set once the event has been handled") {
                    wasSystemUserWhileSending shouldBe true
                    LoggedInUserProvider.systemUser.get() shouldBe false
                }
            }
        }
    }

    context("Handling a zaak document toegevoegd event") {
        given("a zaak without a behandelaar medewerker") {
            val rolType = createRolType(omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR)
            val zaak = createZaak()
            val zaakinformatieobjectURI = URI("https://example.com/zaakinformatieobjecten/${java.util.UUID.randomUUID()}")
            val event = SignaleringEvent(
                SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD,
                SignaleringEventId(zaak.url, zaakinformatieobjectURI),
                null
            )
            every { zrcClientService.readZaak(zaak.url) } returns zaak
            every { zrcClientService.readZaakinformatieobject(any()) } returns mockk()
            every {
                ztcClientService.readRoltype(
                    zaaktypeURI = zaak.zaaktype,
                    omschrijvingGeneriekEnum = OmschrijvingGeneriekEnum.BEHANDELAAR,
                    omschrijving = ROLTYPE_OMSCHRIJVING_BEHANDELAAR
                )
            } returns rolType
            every { zrcClientService.listRollen(any<RolListParameters>()) } returns Results(emptyList<Rol<*>>(), 0)

            `when`("the event is observed") {
                signaleringEventObserver.onFire(event)

                then("no signalering is stored or sent") {
                    verify(exactly = 0) {
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }
    }

    context("Handling a taak op naam event") {
        given("a task without an assignee") {
            val task = createTestTask(assignee = null)
            val event = SignaleringEvent(
                SignaleringType.Type.TAAK_OP_NAAM,
                SignaleringEventId("fakeTaskId", null),
                createUser()
            )
            every { flowableTaskService.readOpenTask("fakeTaskId") } returns task

            `when`("the event is observed") {
                signaleringEventObserver.onFire(event)

                then("no signalering is stored or sent") {
                    verify(exactly = 0) {
                        signaleringService.storeSignalering(any())
                        signaleringService.sendSignalering(any())
                    }
                }
            }
        }
    }
})
