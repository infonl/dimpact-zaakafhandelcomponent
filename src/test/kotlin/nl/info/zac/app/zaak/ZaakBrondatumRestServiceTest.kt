/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.model.createRestZaakSetBrondatum
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.zaak.ZaakService
import java.time.LocalDate

class ZaakBrondatumRestServiceTest : BehaviorSpec({
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zaakBrondatumRestService = ZaakBrondatumRestService(
        loggedInUserInstance,
        policyService,
        zaakService,
        zgwApiService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Setting the brondatum of a zaak") {
        given("a user with the 'brondatumZetten' right") {
            val zaakType = createZaakType()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val loggedInUser = createLoggedInUser()
            val restZaakSetBrondatum = createRestZaakSetBrondatum(
                brondatum = LocalDate.of(2023, 12, 1)
            )

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(brondatumZetten = true)
            every { loggedInUserInstance.get() } returns loggedInUser
            every {
                zgwApiService.setBrondatum(zaak, restZaakSetBrondatum.brondatum)
            } just runs

            `when`("the brondatum is set") {
                zaakBrondatumRestService.setBrondatum(zaak.uuid, restZaakSetBrondatum)

                then("the brondatum procedure is processed with the given date") {
                    verify(exactly = 1) {
                        zgwApiService.setBrondatum(zaak, restZaakSetBrondatum.brondatum)
                    }
                }
            }
        }

        given("a user without the 'brondatumZetten' right") {
            val zaakType = createZaakType()
            val zaak = createZaak(zaaktypeUri = zaakType.url)
            val loggedInUser = createLoggedInUser()
            val restZaakSetArchivalDate = createRestZaakSetBrondatum()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny()
            every { loggedInUserInstance.get() } returns loggedInUser

            `when`("the brondatum is set") {
                shouldThrow<PolicyException> {
                    zaakBrondatumRestService.setBrondatum(zaak.uuid, restZaakSetArchivalDate)
                }

                then("the brondatum procedure is not processed") {
                    verify(exactly = 0) {
                        zgwApiService.setBrondatum(any(), any())
                    }
                }
            }
        }
    }
})
