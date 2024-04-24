package net.atos.zac.zaken

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createRolType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.event.EventingService
import net.atos.zac.event.Opcode
import net.atos.zac.identity.model.createGroup
import net.atos.zac.identity.model.createUser
import net.atos.zac.websocket.event.ScreenEvent
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType

class ZakenServiceTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val indexeerService = mockk<IndexeerService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val tracer = mockk<Tracer>()
    val spanBuilder = mockk<SpanBuilder>()
    val span = mockk<Span>()
    val scope = mockk<Scope>()
    val context = mockk<Context>()
    val zakenService = ZakenService(
        eventingService = eventingService,
        indexeerService = indexeerService,
        zrcClientService = zrcClientService,
        ztcClientService = ztcClientService,
        tracer = tracer
    )
    val explanation = "dummyExplanation"
    val screenEventResourceId = "dummyResourceId"
    val zaken = listOf(
        createZaak(),
        createZaak()
    )
    val group = createGroup()
    val user = createUser()
    val rolTypeBehandelaar = createRolType(
        omschrijvingGeneriek = RolType.OmschrijvingGeneriekEnum.BEHANDELAAR
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("A list of zaken") {
        val screenEventSlot = slot<ScreenEvent>()
        every { tracer.spanBuilder("ZakenService.assignZakenAsync") } returns spanBuilder
        every { spanBuilder.startSpan() } returns span
        every { span.storeInContext(any()) } returns context
        every { context.makeCurrent() } returns scope
        every { span.end() } just Runs
        every { scope.close() } just Runs
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every {
                ztcClientService.readRoltype(
                    RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                    it.zaaktype
                )
            } returns rolTypeBehandelaar
            every { zrcClientService.updateRol(it, any(), explanation) } just Runs
            every { indexeerService.indexeerDirect(it.uuid.toString(), ZoekObjectType.ZAAK, false) } just Runs
            every { eventingService.send(capture(screenEventSlot)) } just Runs
        }
        When(
            """the assign zaken async function is called with a group, a user
                and a screen event resource id"""
        ) {
            zakenService.assignZakenAsync(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                group = group,
                user = user,
                screenEventResourceId = screenEventResourceId
            )

            Then(
                """for both zaken the group and user roles 
                    and the search index should be updated and
                    a screen event of type 'zaken verdelen' should sent"""
            ) {
                zaken.map {
                    // for every zaak both the user and group roles should have been updated
                    verify(exactly = 2) {
                        zrcClientService.updateRol(it, any(), explanation)
                    }
                }
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VERDELEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }
    Given("A list of zaken") {
        clearAllMocks()
        val screenEventSlot = slot<ScreenEvent>()
        every { tracer.spanBuilder("ZakenService.releaseZakenAsync") } returns spanBuilder
        every { spanBuilder.startSpan() } returns span
        every { span.storeInContext(any()) } returns context
        every { context.makeCurrent() } returns scope
        every { span.end() } just Runs
        every { scope.close() } just Runs
        zaken.map {
            every { zrcClientService.readZaak(it.uuid) } returns it
            every { zrcClientService.deleteRol(it, any(), explanation) } just Runs
            every { indexeerService.indexeerDirect(it.uuid.toString(), ZoekObjectType.ZAAK, false) } just Runs
        }
        every { eventingService.send(capture(screenEventSlot)) } just Runs
        When(
            """the release zaken async function is called with
                 a screen event resource id"""
        ) {
            zakenService.releaseZakenAsync(
                zaakUUIDs = zaken.map { it.uuid },
                explanation = explanation,
                screenEventResourceId = screenEventResourceId
            )
            Then(
                """both zaken should no longer have a user assigned
                     but the group should still be assigned
                    and the search index should be updated and
                    a screen event of type 'zaken vrijgeven' should sent"""
            ) {
                zaken.map {
                    verify(exactly = 1) {
                        zrcClientService.deleteRol(it, BetrokkeneType.MEDEWERKER, explanation)
                    }
                }
                with(screenEventSlot.captured) {
                    opcode shouldBe Opcode.UPDATED
                    objectType shouldBe ScreenEventType.ZAKEN_VRIJGEVEN
                    objectId.resource shouldBe screenEventResourceId
                }
            }
        }
    }
    Given("A list of zaken and a failing ZRC client service when retrieving the second zaak ") {
        clearAllMocks()
        every { tracer.spanBuilder("ZakenService.assignZakenAsync") } returns spanBuilder
        every { spanBuilder.startSpan() } returns span
        every { span.storeInContext(any()) } returns context
        every { context.makeCurrent() } returns scope
        every { span.setStatus(StatusCode.ERROR) } returns span
        every { span.recordException(any()) } returns span
        every { span.end() } just Runs
        every { scope.close() } just Runs
        every { zrcClientService.readZaak(zaken[0].uuid) } returns zaken[0]
        every { zrcClientService.readZaak(zaken[1].uuid) } throws RuntimeException("dummyRuntimeException")
        When(
            """the assign zaken async function is called with a group
                and a screen event resource id"""
        ) {
            shouldThrow<RuntimeException> {
                zakenService.assignZakenAsync(
                    zaakUUIDs = zaken.map { it.uuid },
                    explanation = explanation,
                    group = group,
                    screenEventResourceId = screenEventResourceId
                )
            }
            Then(
                """the exception should be re-thrown and for neither of the zaken 
                    the group and user role nor the search index should be updated
                    and no screen event of type 'zaken verdelen' should be sent"""
            ) {
                verify(exactly = 0) {
                    zrcClientService.updateRol(any(), any(), explanation)
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }
})
