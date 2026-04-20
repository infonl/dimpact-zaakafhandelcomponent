/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.model.RestZaakResumeData
import nl.info.zac.app.zaak.model.RestZaakSuspendData
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.zaak.ZaakService
import java.time.ZonedDateTime
import java.util.UUID

class ZaakSuspendRestServiceTest : BehaviorSpec({
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val suspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val zaakService = mockk<ZaakService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()

    val zaakSuspendRestService = ZaakSuspendRestService(
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        restZaakConverter = restZaakConverter,
        suspensionZaakHelper = suspensionZaakHelper,
        zaakService = zaakService,
        zaakVariabelenService = zaakVariabelenService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Suspending a zaak") {
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val restZaak = createRestZaak()

        Given("a zaak exists and suspension is requested") {
            val suspendData = RestZaakSuspendData(
                reason = "fakeSuspensionReason",
                numberOfDays = 5L
            )
            val suspendedZaak = createZaak(uuid = zaakUUID)

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                suspensionZaakHelper.suspendZaak(
                    zaak = zaak,
                    numberOfDays = suspendData.numberOfDays,
                    suspensionReason = suspendData.reason
                )
            } returns suspendedZaak
            every { restZaakConverter.toRestZaak(suspendedZaak, zaakType, zaakRechten, loggedInUser) } returns restZaak

            When("suspendZaak is called") {
                val result = zaakSuspendRestService.suspendZaak(zaakUUID, suspendData)

                Then("the suspended zaak is returned") {
                    result shouldBe restZaak
                }
            }
        }
    }

    Context("Resuming a zaak") {
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten()
        val loggedInUser = createLoggedInUser()
        val restZaak = createRestZaak()

        Given("a zaak exists and resuming is requested") {
            val resumeData = RestZaakResumeData(
                reason = "fakeResumeReason"
            )
            val resumedZaak = createZaak(uuid = zaakUUID)

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every {
                suspensionZaakHelper.resumeZaak(any(), any(), any())
            } returns resumedZaak
            every { restZaakConverter.toRestZaak(resumedZaak, zaakType, zaakRechten, loggedInUser) } returns restZaak

            When("resumeZaak is called") {
                val result = zaakSuspendRestService.resumeZaak(zaakUUID, resumeData)

                Then("the resumed zaak is returned") {
                    result shouldBe restZaak
                }
            }
        }
    }

    Context("Reading opschorting of a zaak") {
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val zaakType = createZaakType()
        val zaakRechten = createZaakRechten(lezen = true)
        val loggedInUser = createLoggedInUser()

        Given("a suspended zaak exists") {
            val suspensionDateTime = ZonedDateTime.now().minusDays(3)
            val expectedDays = 5

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { zaakVariabelenService.findDatumtijdOpgeschort(zaakUUID) } returns suspensionDateTime
            every { zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUUID) } returns expectedDays

            When("readOpschortingZaak is called") {
                val result = zaakSuspendRestService.readOpschortingZaak(zaakUUID)

                Then("the suspension details are returned") {
                    result.vanafDatumTijd shouldBe suspensionDateTime
                    result.duurDagen shouldBe expectedDays
                }
            }
        }

        Given("a zaak exists that has not been suspended") {
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns zaakRechten
            every { zaakVariabelenService.findDatumtijdOpgeschort(zaakUUID) } returns null
            every { zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUUID) } returns null

            When("readOpschortingZaak is called") {
                val result = zaakSuspendRestService.readOpschortingZaak(zaakUUID)

                Then("the suspension details show no suspension") {
                    result.vanafDatumTijd shouldBe null
                    result.duurDagen shouldBe 0
                }
            }
        }
    }
})
