/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering.event

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.signalering.model.SignaleringType
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringInstellingen
import java.net.URI

class SignaleringEventObserverTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val identityService = mockk<IdentityService>()
    val signaleringService = mockk<SignaleringService>()
    val signaleringEventObserver = SignaleringEventObserver(
        ztcClientService,
        zrcClientService,
        flowableTaskService,
        identityService,
        signaleringService
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
})
