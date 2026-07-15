/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter
import net.atos.zac.app.mail.model.RESTMailGegevens
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.mail.MailService
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.zaak.ZaakService
import java.util.UUID

class MailRestServiceTest : BehaviorSpec({
    val zaakService = mockk<ZaakService>()
    val mailService = mockk<MailService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val restMailGegevensConverter = mockk<RESTMailGegevensConverter>()

    @Suppress("UNCHECKED_CAST")
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val mailRestService = MailRestService(
        zaakService,
        mailService,
        zaakVariabelenService,
        policyService,
        zrcClientService,
        restMailGegevensConverter,
        loggedInUserInstance
    )

    afterEach { checkUnnecessaryStub() }

    context("sendMail") {
        given("A zaak UUID, mail gegevens, and policy permits versturenEmail") {
            val fakeZaakUuid = UUID.randomUUID()
            val fakeZaak = createZaak(uuid = fakeZaakUuid)
            val fakeRestMailGegevens = mockk<RESTMailGegevens>()
            val fakeMailGegevens = mockk<MailGegevens>()
            val fakeLoggedInUser = createLoggedInUser()

            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { zrcClientService.readZaak(fakeZaakUuid) } returns fakeZaak
            every { policyService.readZaakRechten(fakeZaak, fakeLoggedInUser) } returns createZaakRechten(versturenEmail = true)
            every { restMailGegevensConverter.convert(fakeRestMailGegevens) } returns fakeMailGegevens
            every { mailService.sendMail(fakeMailGegevens, any()) } returns null

            `when`("sendMail is called") {
                mailRestService.sendMail(fakeZaakUuid, fakeRestMailGegevens)

                then("ZrcClientService reads the zaak, policy is checked, and MailService sends the mail") {
                    verify { zrcClientService.readZaak(fakeZaakUuid) }
                    verify { policyService.readZaakRechten(fakeZaak, fakeLoggedInUser) }
                    verify { mailService.sendMail(fakeMailGegevens, any()) }
                }
            }
        }
    }

    context("sendAcknowledgmentReceiptMail when permitted and not yet sent") {
        given("Policy permits and ontvangstbevestiging not yet sent") {
            val fakeZaakUuid = UUID.randomUUID()
            val fakeZaak = createZaak(uuid = fakeZaakUuid)
            val fakeRestMailGegevens = mockk<RESTMailGegevens>()
            val fakeMailGegevens = mockk<MailGegevens>()
            val fakeLoggedInUser = createLoggedInUser()

            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { zrcClientService.readZaak(fakeZaakUuid) } returns fakeZaak
            every { zaakVariabelenService.findOntvangstbevestigingVerstuurd(fakeZaakUuid) } returns false
            every {
                policyService.readZaakRechten(fakeZaak, fakeLoggedInUser)
            } returns createZaakRechten(versturenOntvangstbevestiging = true)
            every { restMailGegevensConverter.convert(fakeRestMailGegevens) } returns fakeMailGegevens
            every { mailService.sendMail(fakeMailGegevens, any()) } returns null
            every { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(fakeZaak) } just runs

            `when`("sendAcknowledgmentReceiptMail is called") {
                mailRestService.sendAcknowledgmentReceiptMail(fakeZaakUuid, fakeRestMailGegevens)

                then("MailService sends the mail and zaak is marked as ontvangstbevestiging verstuurd") {
                    verify { mailService.sendMail(fakeMailGegevens, any()) }
                    verify { zaakService.setOntvangstbevestigingVerstuurdIfNotHeropend(fakeZaak) }
                }
            }
        }
    }

    context("sendAcknowledgmentReceiptMail when policy denies") {
        given("Policy denies versturenOntvangstbevestiging") {
            val fakeZaakUuid = UUID.randomUUID()
            val fakeZaak = createZaak(uuid = fakeZaakUuid)
            val fakeRestMailGegevens = mockk<RESTMailGegevens>()
            val fakeLoggedInUser = createLoggedInUser()

            every { loggedInUserInstance.get() } returns fakeLoggedInUser
            every { zrcClientService.readZaak(fakeZaakUuid) } returns fakeZaak
            every { zaakVariabelenService.findOntvangstbevestigingVerstuurd(fakeZaakUuid) } returns false
            every {
                policyService.readZaakRechten(fakeZaak, fakeLoggedInUser)
            } returns createZaakRechten(versturenOntvangstbevestiging = false)

            `when`("sendAcknowledgmentReceiptMail is called") {
                shouldThrow<PolicyException> {
                    mailRestService.sendAcknowledgmentReceiptMail(fakeZaakUuid, fakeRestMailGegevens)
                }

                then("MailService is not called") {
                    verify(exactly = 0) { mailService.sendMail(any(), any()) }
                }
            }
        }
    }
})
