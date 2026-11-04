/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.model.RestArchiefnominatie
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakArchiefnominatieGegevens
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.zaak.ZaakService

class ZaakArchiefRestServiceTest : BehaviorSpec({
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val zaakService = mockk<ZaakService>()
    val zrcClientService = mockk<ZrcClientService>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val zaakArchiefRestService = ZaakArchiefRestService(
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        zaakService = zaakService,
        zrcClientService = zrcClientService,
        restZaakConverter = restZaakConverter
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Changing the archiefnominatie of a zaak") {
        given("an afgehandelde zaak with archiefnominatie VERNIETIGEN and a user with the 'wijzigenArchief' right") {
            val zaakType = createZaakType()
            val zaak = createZaak(zaaktypeUri = zaakType.url, archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN)
            val updatedZaak = createZaak(
                uuid = zaak.uuid,
                zaaktypeUri = zaakType.url,
                archiefnominatie = ArchiefnominatieEnum.BLIJVEND_BEWAREN
            )
            val loggedInUser = createLoggedInUser()
            val zaakRechten = createZaakRechten()
            val updatedZaakRechten = createZaakRechten(wijzigen = false)
            val restZaak = createRestZaak()
            val restZaakArchiefnominatieGegevens = createRestZaakArchiefnominatieGegevens(
                archiefnominatie = RestArchiefnominatie.BLIJVEND_BEWAREN,
                reden = "fakeReden"
            )
            val patchZaakSlot = slot<Zaak>()

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak = zaak, zaaktype = zaakType, loggedInUser = loggedInUser)
            } returns zaakRechten
            every {
                policyService.readZaakRechten(zaak = updatedZaak, zaaktype = zaakType, loggedInUser = loggedInUser)
            } returns updatedZaakRechten
            every { zrcClientService.patchZaak(zaak.uuid, capture(patchZaakSlot), "fakeReden") } returns updatedZaak
            every {
                restZaakConverter.toRestZaak(
                    zaak = updatedZaak,
                    zaakType = zaakType,
                    zaakRechten = updatedZaakRechten,
                    loggedInUser = loggedInUser
                )
            } returns restZaak

            `when`("the archiefnominatie is changed to BLIJVEND_BEWAREN") {
                val returnedRestZaak = zaakArchiefRestService.updateArchiefnominatie(
                    zaak.uuid,
                    restZaakArchiefnominatieGegevens
                )

                then("only the archiefnominatie is patched in Open Zaak, with the reden as audit explanation") {
                    with(patchZaakSlot.captured) {
                        archiefnominatie shouldBe ArchiefnominatieEnum.BLIJVEND_BEWAREN
                        archiefactiedatum shouldBe null
                        startdatumBewaartermijn shouldBe null
                    }
                }

                and("the updated zaak is returned with the rechten evaluated on the updated zaak") {
                    returnedRestZaak shouldBe restZaak
                }
            }
        }

        given("a zaak and a user without the 'wijzigenArchief' right") {
            val zaakType = createZaakType()
            val zaak = createZaak(zaaktypeUri = zaakType.url, archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN)
            val loggedInUser = createLoggedInUser()

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak = zaak, zaaktype = zaakType, loggedInUser = loggedInUser)
            } returns createZaakRechtenAllDeny(lezen = true)

            `when`("the archiefnominatie is changed") {
                shouldThrow<PolicyException> {
                    zaakArchiefRestService.updateArchiefnominatie(
                        zaak.uuid,
                        createRestZaakArchiefnominatieGegevens()
                    )
                }

                then("the zaak is not patched") {
                    verify(exactly = 0) {
                        zrcClientService.patchZaak(any(), any(), any())
                    }
                }
            }
        }
    }
})
