/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.taken

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.inspectors.forExactly
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
import net.atos.zac.event.Opcode
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.TakenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createTaakRechten
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.event.SignaleringEvent
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.task.api.Task
import org.junit.jupiter.api.Assertions.assertEquals

@MockKExtension.CheckUnnecessaryStub
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
                        "an signalering event and two screen events are sent,  " +
                        "and the indexed task data is updated"
                ) {
                    val restTaakToekennenGegevens = createRESTTaakToekennenGegevens()
                    val task = mockk<Task>()
                    val identityLinkInfo = mockk<IdentityLinkInfo>()
                    val identityLinks = listOf(identityLinkInfo)
                    val loggedInUser = createLoggedInUser()
                    val signaleringEventSlot = slot<SignaleringEvent<*>>()
                    val screenEventSlots = mutableListOf<ScreenEvent>()

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
                    every { eventingService.send(capture(signaleringEventSlot)) } just runs
                    every { eventingService.send(capture(screenEventSlots)) } just runs
                    every { indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK) } just runs

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
                        eventingService.send(any<SignaleringEvent<*>>())
                        indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK)
                    }
                    // we expect two screen events to be sent
                    verify(exactly = 2) {
                        eventingService.send(any<ScreenEvent>())
                    }
                    with(signaleringEventSlot.captured) {
                        assertEquals(this.objectType, SignaleringType.Type.TAAK_OP_NAAM)
                        assertEquals(this.actor, loggedInUser.id)
                        assertEquals(this.objectId.resource, restTaakToekennenGegevens.taakId)
                    }
                    // we expect both a taak screen event and a zaak_taken screen event to be sent
                    screenEventSlots.forExactly(1) { screenEvent ->
                        assertEquals(screenEvent.opcode, Opcode.UPDATED)
                        assertEquals(screenEvent.objectType, ScreenEventType.TAAK)
                        assertEquals(screenEvent.objectId.resource, restTaakToekennenGegevens.taakId)
                    }
                    screenEventSlots.forExactly(1) { screenEvent ->
                        assertEquals(screenEvent.opcode, Opcode.UPDATED)
                        assertEquals(screenEvent.objectType, ScreenEventType.ZAAK_TAKEN)
                        assertEquals(screenEvent.objectId.resource, restTaakToekennenGegevens.zaakUuid.toString())
                    }
                }
            }
        }
    }
}
