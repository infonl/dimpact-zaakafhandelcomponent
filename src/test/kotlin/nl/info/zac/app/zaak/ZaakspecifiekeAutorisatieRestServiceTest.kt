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
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.model.createRestZaakspecifiekGeautoriseerdeMedewerker
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.identity.model.createUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService

class ZaakspecifiekeAutorisatieRestServiceTest : BehaviorSpec({
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val zaakService = mockk<ZaakService>()
    val zaakspecifiekeAutorisatieService = mockk<ZaakspecifiekeAutorisatieService>()
    val zaakspecifiekeAutorisatieRestService = ZaakspecifiekeAutorisatieRestService(
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        zaakService = zaakService,
        zaakspecifiekeAutorisatieService = zaakspecifiekeAutorisatieService
    )
    val zaakType = createZaakType()
    val zaak = createZaak(zaaktypeUri = zaakType.url)
    val loggedInUser = createLoggedInUser()

    afterEach {
        checkUnnecessaryStub()
    }

    context("Listing the medewerkers who can be added to a zaakspecifiek geautoriseerde zaak") {
        given("a user with the 'wijzigen' right on the zaak") {
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every {
                zaakspecifiekeAutorisatieService.listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
                    zaak,
                    zaakType,
                    "fakeGroepId"
                )
            } returns listOf(createUser(id = "fakeMedewerkerId", fullName = "fakeFullName"))

            `when`("the kandidaten of a groep are listed") {
                val restUsers = zaakspecifiekeAutorisatieRestService.listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
                    zaak.uuid,
                    "fakeGroepId"
                )

                then("the kandidaten are returned") {
                    restUsers.size shouldBe 1
                    with(restUsers.single()) {
                        id shouldBe "fakeMedewerkerId"
                        naam shouldBe "fakeFullName"
                    }
                }
            }
        }

        given("a user without the 'wijzigen' right on the zaak") {
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { loggedInUserInstance.get() } returns loggedInUser
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(wijzigen = false)

            `when`("the kandidaten of a groep are listed") {
                shouldThrow<PolicyException> {
                    zaakspecifiekeAutorisatieRestService.listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
                        zaak.uuid,
                        "fakeGroepId"
                    )
                }

                then("the kandidaten are not read") {
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(
                            any(),
                            any(),
                            any()
                        )
                    }
                }
            }
        }
    }

    context("Adding a medewerker to a zaakspecifiek geautoriseerde zaak") {
        given("a user with the 'wijzigen' right on the zaak") {
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every {
                zaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(
                    zaak = zaak,
                    zaakType = zaakType,
                    groepId = "fakeGroepId",
                    medewerkerId = "fakeMedewerkerId"
                )
            } just runs

            `when`("a medewerker of a groep is added") {
                zaakspecifiekeAutorisatieRestService.addZaakspecifiekGeautoriseerdeMedewerker(
                    zaak.uuid,
                    createRestZaakspecifiekGeautoriseerdeMedewerker(
                        groepId = "fakeGroepId",
                        medewerkerId = "fakeMedewerkerId"
                    )
                )

                then("the medewerker is added to the zaak") {
                    verify(exactly = 1) {
                        zaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(
                            zaak = zaak,
                            zaakType = zaakType,
                            groepId = "fakeGroepId",
                            medewerkerId = "fakeMedewerkerId"
                        )
                    }
                }
            }
        }

        given("a user without the 'wijzigen' right on the zaak") {
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { loggedInUserInstance.get() } returns loggedInUser
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechten(wijzigen = false)

            `when`("a medewerker is added") {
                shouldThrow<PolicyException> {
                    zaakspecifiekeAutorisatieRestService.addZaakspecifiekGeautoriseerdeMedewerker(
                        zaak.uuid,
                        createRestZaakspecifiekGeautoriseerdeMedewerker()
                    )
                }

                then("no medewerker is added") {
                    verify(exactly = 0) {
                        zaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(
                            zaak = any(),
                            zaakType = any(),
                            groepId = any(),
                            medewerkerId = any()
                        )
                    }
                }
            }
        }
    }
})
