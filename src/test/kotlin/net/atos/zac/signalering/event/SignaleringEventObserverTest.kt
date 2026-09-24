/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering.event

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.model.createBehandelaarRolType
import nl.info.client.zgw.ztc.model.createZaakspecifiekGeautoriseerdeMedewerkerRolType
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringInstellingen
import java.net.URI

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
    }
})
