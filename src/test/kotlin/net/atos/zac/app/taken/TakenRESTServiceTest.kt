/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.converter.RESTTaakHistorieConverter
import net.atos.zac.app.taken.model.TaakStatus
import net.atos.zac.app.taken.model.createRESTTaakToekennenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.TakenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createTaakRechten
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.zoeken.IndexeerService
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.Task


class TakenRESTServiceTest : BehaviorSpec() {
    val drcClientService = mockk<DRCClientService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val eventingService = mockk<EventingService>()
    val httpSession = mockk<Instance<HttpSession>>()
    val indexeerService = mockk<IndexeerService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val opschortenZaakHelper = mockk<OpschortenZaakHelper>()
    val policyService = mockk<PolicyService>()
    val restInformatieobjectConverter = mockk<RESTInformatieobjectConverter>()
    val signaleringenService = mockk<SignaleringenService>()
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val restTaakConverter = mockk<RESTTaakConverter>()
    val taakHistorieConverter = mockk<RESTTaakHistorieConverter>()
    val takenService = mockk<TakenService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZRCClientService>()

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not properly support constructor injection for JAX-RS REST services.
    @InjectMockKs
    lateinit var takenRESTService: TakenRESTService

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
    }

    init {
        given("a task is not yet assigned") {
            When("'toekennen' is called") {
                then(
                    "the task is assigned to the provided user and group, " +
                            "an signalering event and a screen event are send,  " +
                            "and the indexeerService is called to index the updated task data"
                ) {
                    val restTaakToekennenGegevens = createRESTTaakToekennenGegevens()
                    val task = mockk<Task>()
                    val identityLinkInfo = mockk<IdentityLinkInfo>()
                    val identityLinks = listOf(identityLinkInfo)
                    val loggedInUser = createLoggedInUser()

                    every { takenService.readOpenTask(restTaakToekennenGegevens.taakId) } returns task
                    every { takenService.getTaakStatus(task) } returns TaakStatus.NIET_TOEGEKEND
                    every {
                        takenService.assignTaskToUser(
                            restTaakToekennenGegevens.taakId,
                            restTaakToekennenGegevens.behandelaarId,
                            restTaakToekennenGegevens.reden
                        )
                    } returns task
                    every {
                        takenService.assignTaskToGroup(
                            task,
                            restTaakToekennenGegevens.groepId,
                            restTaakToekennenGegevens.reden
                        )
                    } returns task
                    every { policyService.readTaakRechten(task) } returns createTaakRechten()
                    every { task.assignee } returns ""
                    every { task.identityLinks } returns identityLinks
                    every { task.id } returns restTaakToekennenGegevens.taakId
                    every { restTaakConverter.extractGroupId(identityLinks) } returns "dummyGroupId"
                    every { loggedInUserInstance.get() } returns loggedInUser
                    every { eventingService.send(any<SignaleringEvent<*>>()) } just runs
                    every { eventingService.send(any<ScreenEvent>()) } just runs

                    takenRESTService.toekennen(restTaakToekennenGegevens)

                    verify(exactly = 1) {
                        takenService.assignTaskToUser(
                            restTaakToekennenGegevens.taakId,
                            restTaakToekennenGegevens.behandelaarId,
                            restTaakToekennenGegevens.reden
                        )
                        takenService.assignTaskToGroup(
                            task,
                            restTaakToekennenGegevens.groepId,
                            restTaakToekennenGegevens.reden
                        )
                    }
                    verify {
                        eventingService.send(any<SignaleringEvent<*>>())
                        eventingService.send(any<ScreenEvent>())
                    }
                }
            }
        }
    }
}
