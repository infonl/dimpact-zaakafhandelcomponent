/*
 * SPDX-FileCopyrightText: 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.model.RestDecisionWithdrawalData
import nl.info.zac.app.zaak.model.createRestDecision
import nl.info.zac.app.zaak.model.createRestDecisionChangeData
import nl.info.zac.app.zaak.model.createRestDecisionCreateData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.zaak.ZaakService
import java.net.URI
import java.util.UUID

@Suppress("LongMethod")
class ZaakBesluitRestServiceTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val decisionService = mockk<DecisionService>()
    val eventingService = mockk<EventingService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val restDecisionConverter = mockk<RestDecisionConverter>()
    val zaakHistoryLineConverter = mockk<ZaakHistoryLineConverter>()
    val zaakService = mockk<ZaakService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()

    val zaakBesluitRestService = ZaakBesluitRestService(
        brcClientService = brcClientService,
        decisionService = decisionService,
        eventingService = eventingService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        restDecisionConverter = restDecisionConverter,
        zaakHistoryLineConverter = zaakHistoryLineConverter,
        zaakService = zaakService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Context("List besluiten for zaak UUID") {
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val zaakType = createZaakType()
        val loggedInUser = createLoggedInUser()
        val besluit = createBesluit(zaakUri = zaak.url)
        val restDecision = createRestDecision()

        Given("user has lezen permission") {
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { zrcClientService.readZaak(zaakUUID) } returns zaak
            every { brcClientService.listBesluiten(zaak) } returns listOf(besluit)
            every { restDecisionConverter.convertToRestDecision(besluit) } returns restDecision

            When("besluiten are requested") {
                val result = zaakBesluitRestService.listBesluitenForZaakUUID(zaakUUID)

                Then("the list of rest decisions is returned") {
                    result shouldHaveSize 1
                    result.first() shouldBe restDecision
                }
            }
        }

        Given("user has no lezen permission") {
            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny()

            When("besluiten are requested") {
                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        zaakBesluitRestService.listBesluitenForZaakUUID(zaakUUID)
                    }
                }
            }
        }
    }

    Context("Create besluit") {
        val zaak = createZaak()
        val zaakType = createZaakType(besluittypen = listOf(URI("http://example.com/besluittype/${UUID.randomUUID()}")))
        val loggedInUser = createLoggedInUser()
        val besluit = createBesluit(zaakUri = zaak.url)
        val restDecision = createRestDecision()
        val createData = createRestDecisionCreateData(zaakUuid = zaak.uuid)

        Given("user has vastleggenBesluit permission and zaaktype has besluittypen") {
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { decisionService.createDecision(zaak, createData) } returns besluit
            every { restDecisionConverter.convertToRestDecision(besluit) } returns restDecision
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("besluit is created") {
                val result = zaakBesluitRestService.createBesluit(createData)

                Then("the created rest decision is returned and a screen event is sent") {
                    result shouldBe restDecision
                    verify(exactly = 1) { eventingService.send(any<ScreenEvent>()) }
                }
            }
        }

        Given("user has no vastleggenBesluit permission") {
            every { zaakService.readZaakAndZaakTypeByZaakUUID(zaak.uuid) } returns Pair(zaak, zaakType)
            every { loggedInUserInstance.get() } returns loggedInUser
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny()

            When("besluit creation is attempted") {
                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        zaakBesluitRestService.createBesluit(createData)
                    }
                }
            }
        }
    }

    Context("Update besluit") {
        val zaak = createZaak()
        val besluitUUID = UUID.randomUUID()
        val besluit = createBesluit(
            zaakUri = zaak.url,
            url = URI("http://localhost/besluit/$besluitUUID")
        )
        val loggedInUser = createLoggedInUser()
        val restDecision = createRestDecision()
        val changeData = createRestDecisionChangeData(besluitUUID = besluitUUID)

        Given("user has vastleggenBesluit permission") {
            every { brcClientService.readBesluit(besluitUUID) } returns besluit
            every { zrcClientService.readZaak(besluit.zaak) } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every { decisionService.updateDecision(besluit, changeData) } just runs
            every { restDecisionConverter.convertToRestDecision(besluit) } returns restDecision
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("besluit is updated") {
                val result = zaakBesluitRestService.updateBesluit(changeData)

                Then("the updated rest decision is returned and a screen event is sent") {
                    result shouldBe restDecision
                    verify(exactly = 1) { eventingService.send(any<ScreenEvent>()) }
                }
            }
        }

        Given("user has no vastleggenBesluit permission") {
            every { brcClientService.readBesluit(besluitUUID) } returns besluit
            every { zrcClientService.readZaak(besluit.zaak) } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

            When("besluit update is attempted") {
                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        zaakBesluitRestService.updateBesluit(changeData)
                    }
                }
            }
        }
    }

    Context("Withdraw besluit (intrekken)") {
        val zaak = createZaak()
        val besluitUUID = UUID.randomUUID()
        val besluit = createBesluit(
            zaakUri = zaak.url,
            url = URI("http://localhost/besluit/$besluitUUID")
        )
        val loggedInUser = createLoggedInUser()
        val restDecision = createRestDecision()
        val withdrawalData = RestDecisionWithdrawalData(
            besluitUuid = besluitUUID,
            reden = "fakeReden",
            vervalreden = "fakeVervalreden"
        )

        Given("user has behandelen permission and zaak is open") {
            every { decisionService.readDecision(withdrawalData) } returns besluit
            every { zrcClientService.readZaak(besluit.zaak) } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
            every { decisionService.withdrawDecision(besluit, withdrawalData.reden) } returns besluit
            every { restDecisionConverter.convertToRestDecision(besluit) } returns restDecision
            every { eventingService.send(any<ScreenEvent>()) } just runs

            When("besluit is withdrawn") {
                val result = zaakBesluitRestService.intrekkenBesluit(withdrawalData)

                Then("the withdrawn rest decision is returned and a screen event is sent") {
                    result shouldBe restDecision
                    verify(exactly = 1) { eventingService.send(any<ScreenEvent>()) }
                }
            }
        }

        Given("user has no behandelen permission") {
            every { decisionService.readDecision(withdrawalData) } returns besluit
            every { zrcClientService.readZaak(besluit.zaak) } returns zaak
            every { loggedInUserInstance.get() } returns loggedInUser
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

            When("besluit withdrawal is attempted") {
                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        zaakBesluitRestService.intrekkenBesluit(withdrawalData)
                    }
                }
            }
        }
    }

    Context("List besluit history") {
        val besluitUUID = UUID.randomUUID()
        val historyLines = listOf(HistoryLine("fakeLabel", "fakeOld", "fakeNew"))

        Given("a valid besluit UUID and a user with lezen permission") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val besluit = createBesluit(zaakUri = zaak.url)
            every { brcClientService.readBesluit(besluitUUID) } returns besluit
            every { zrcClientService.readZaak(besluit.zaak) } returns zaak
            every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
            every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten()
            every { loggedInUserInstance.get() } returns loggedInUser
            every { brcClientService.listAuditTrail(besluitUUID) } returns emptyList()
            every { zaakHistoryLineConverter.convert(emptyList()) } returns historyLines

            When("besluit history is requested") {
                val result = zaakBesluitRestService.listBesluitHistorie(besluitUUID)

                Then("the converted history lines are returned") {
                    result shouldBe historyLines
                }
            }
        }
    }

    Context("List besluit types for zaaktype") {
        val zaaktypeUUID = UUID.randomUUID()
        val zaakType = createZaakType()

        Given("user has zakenTaken permission") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
            every { ztcClientService.readZaaktype(zaaktypeUUID) } returns zaakType
            every { ztcClientService.readBesluittypen(zaakType.url) } returns emptyList()

            When("besluit types are requested") {
                val result = zaakBesluitRestService.listBesluittypes(zaaktypeUUID)

                Then("the list of besluit types is returned") {
                    result shouldHaveSize 0
                }
            }
        }

        Given("user has no zakenTaken permission") {
            every { policyService.readWerklijstRechten() } returns createWerklijstRechtenAllDeny()

            When("besluit types are requested") {
                Then("a PolicyException is thrown") {
                    shouldThrow<PolicyException> {
                        zaakBesluitRestService.listBesluittypes(zaaktypeUUID)
                    }
                }
            }
        }
    }
})
