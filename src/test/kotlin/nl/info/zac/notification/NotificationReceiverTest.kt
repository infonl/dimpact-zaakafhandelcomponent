/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.model.InboxDocument
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.createTestTask
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringVerzondenZoekParameters
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.zac.admin.ZaakafhandelParameterBeheerService
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.task.TaskService
import java.net.URI
import java.util.UUID

const val SECRET = "fakeSecret"

class NotificationReceiverTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val signaleringService = mockk<SignaleringService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val cmmnService = mockk<CMMNService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val taskService = mockk<TaskService>()
    val httpHeaders = mockk<HttpHeaders>()
    val httpSession = mockk<HttpSession>(relaxed = true)
    val httpSessionInstance = mockk<Instance<HttpSession>>()
    val notificationReceiver = NotificationReceiver(
        eventingService = eventingService,
        productaanvraagService = productaanvraagService,
        indexingService = indexingService,
        inboxDocumentenService = inboxDocumentenService,
        zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService,
        cmmnService = cmmnService,
        zaakVariabelenService = zaakVariabelenService,
        signaleringService = signaleringService,
        taskService = taskService,
        secret = SECRET,
        httpSession = httpSessionInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
            a request containing a authorization header and
            a productaanvraag notificatie with a object type UUID for the productaanvraag object type
            """
    ) {
        val productaanvraagObjectUUID = UUID.randomUUID()
        val productTypeUUID = UUID.randomUUID()
        val notificatie = createNotificatie(
            resourceUrl = URI("http://example.com/fakeproductaanvraag/$productaanvraagObjectUUID"),
            properties = mutableMapOf("objectType" to "http://example.com/fakeproducttype/$productTypeUUID")
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { productaanvraagService.handleProductaanvraag(productaanvraagObjectUUID) } just runs

        When("notificatieReceive is called") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the 'functional user' is added to the HTTP sessionm the productaanvraag service is invoked " +
                    "and a 'no content' response is returned"
            ) {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    productaanvraagService.handleProductaanvraag(productaanvraagObjectUUID)
                }
            }
        }
    }

    Given(
        "a request containing a authorization header and a zaaktype create notificatie"
    ) {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("http://example.com/fakezaaktype/$zaaktypeUUID")
        val notificatie = createNotificatie(
            resource = Resource.ZAAKTYPE,
            resourceUrl = zaaktypeUri
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri) } just runs

        When("notificatieReceive is called with the zaaktype create notificatie") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the zaaktype aanvraag service is invoked and a 'no content' response is returned"
            ) {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri)
                }
            }
        }
    }

    Given(
        "a request containing a authorization header and a zaaktype update notificatie"
    ) {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("http://example.com/fakezaaktype/$zaaktypeUUID")
        val notificatie = createNotificatie(
            resource = Resource.ZAAKTYPE,
            resourceUrl = zaaktypeUri,
            action = Action.UPDATE
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri) } just runs

        When("notificatieReceive is called with the zaaktype create notificatie") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the zaaktype aanvraag service is invoked and a 'no content' response is returned"
            ) {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri)
                }
            }
        }
    }
    Given(
        "A request without a authorization header and a zaaktype update notificatie"
    ) {
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("http://example.com/fakezaaktype/$zaaktypeUUID")
        val notificatie = createNotificatie(
            resource = Resource.ZAAKTYPE,
            resourceUrl = zaaktypeUri,
            action = Action.UPDATE
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns null

        When("notificatieReceive is called with the zaaktype create notificatie") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "a 'forbidden' response is returned"
            ) {
                response.status shouldBe Response.Status.FORBIDDEN.statusCode
                verify(exactly = 0) {
                    zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(zaaktypeUri)
                }
            }
        }
    }
    Given(
        """
            A CMMN case with a related task and a request containing an authorization header
             and a 'zaak destroy' notificatie
        """.trimIndent()
    ) {
        val zaakUUID = UUID.randomUUID()
        val zaakUri = URI("http://example.com/fakezaak/$zaakUUID")
        val notificatie = createNotificatie(
            channel = Channel.ZAKEN,
            resource = Resource.ZAAK,
            resourceUrl = zaakUri,
            action = Action.DELETE
        )
        val taskId = "fakeTaskId"
        val tasks = listOf(createTestTask(id = taskId))
        val signaleringZoekParametersSlot = mutableListOf<SignaleringZoekParameters>()
        val signaleringVerzondenZoekParameters = mutableListOf<SignaleringVerzondenZoekParameters>()
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { cmmnService.deleteCase(zaakUUID) } returns Unit
        every { zaakVariabelenService.deleteAllCaseVariables(zaakUUID) } just Runs
        every { indexingService.removeZaak(zaakUUID) } just Runs
        every { indexingService.removeTaak(taskId) } just Runs
        every { signaleringService.deleteSignaleringen(capture(signaleringZoekParametersSlot)) } returns 2
        every { signaleringService.deleteSignaleringVerzonden(capture(signaleringVerzondenZoekParameters)) } returns true
        every { taskService.listTasksForZaak(zaakUUID) } returns tasks
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        When("notificatieReceive is called with the zaak destroy notificatie") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                """
                   the CMMN case is successfully deleted, the zaak is removed from the search index,
                   any signaleringen related to the zaak are deleted and a screen event is sent
               """
            ) {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    cmmnService.deleteCase(zaakUUID)
                    zaakVariabelenService.deleteAllCaseVariables(zaakUUID)
                    indexingService.removeZaak(zaakUUID)
                    indexingService.removeTaak(taskId)
                    eventingService.send(any<ScreenEvent>())
                }
                // signaleringen and signalering verzonden records should be deleted
                // both for the zaak and the related task
                verify(exactly = 2) {
                    signaleringService.deleteSignaleringen(any())
                    signaleringService.deleteSignaleringVerzonden(any())
                }
                signaleringZoekParametersSlot[0].run {
                    subjecttype shouldBe SignaleringSubject.ZAAK
                    subject shouldBe zaakUUID.toString()
                }
                signaleringZoekParametersSlot[1].run {
                    subjecttype shouldBe SignaleringSubject.TAAK
                    subject shouldBe tasks[0].id
                }
                signaleringVerzondenZoekParameters[0].run {
                    subjecttype shouldBe SignaleringSubject.ZAAK
                    subject shouldBe zaakUUID.toString()
                }
                signaleringVerzondenZoekParameters[1].run {
                    subjecttype shouldBe SignaleringSubject.TAAK
                    subject shouldBe tasks[0].id
                }
            }
        }
    }
    Given("A 'create informatieobject' notification") {
        val informatieobjectUUID = UUID.randomUUID()
        val informatieobjectURI = URI("http://example.com/fakezaak/$informatieobjectUUID")
        val notificatie = createNotificatie(
            channel = Channel.INFORMATIEOBJECTEN,
            resource = Resource.INFORMATIEOBJECT,
            resourceUrl = informatieobjectURI,
            action = Action.CREATE
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { indexingService.addOrUpdateInformatieobject(informatieobjectUUID) } just Runs
        every { inboxDocumentenService.create(informatieobjectUUID) } returns mockk<InboxDocument>()

        When("the notification is handled") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "an inbox document is created, the informatieobject is added to the search index, and no screen event is sent"
            ) {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    indexingService.addOrUpdateInformatieobject(informatieobjectUUID)
                }
                verify(exactly = 0) {
                    // no screen event should be sent because it concerns a 'create' action
                    // (no websocket can be listening for events on a resource that does not yet exis)
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }
    Given("A 'destroy informatieobject' notification") {
        val informatieobjectUUID = UUID.randomUUID()
        val informatieobjectURI = URI("http://example.com/fakezaak/$informatieobjectUUID")
        val notificatie = createNotificatie(
            channel = Channel.INFORMATIEOBJECTEN,
            resource = Resource.INFORMATIEOBJECT,
            resourceUrl = informatieobjectURI,
            action = Action.DELETE
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { indexingService.removeInformatieobject(informatieobjectUUID) } just Runs
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        When("the notification is handled") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then("the informatieobject is added to the search index and a screen event is sent") {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    indexingService.removeInformatieobject(informatieobjectUUID)
                    eventingService.send(any<ScreenEvent>())
                }
            }
        }
    }

    Given("A test callback url notification") {
        val notification = createNotificatie(
            channel = Channel.TEST,
            resource = Resource.TEST
        )

        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession

        When("the notification is handled") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notification)

            Then("a response is returned") {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
            }

            And("no processing happens") {
                verify(exactly = 0) {
                    indexingService.removeInformatieobject(any())
                    eventingService.send(any<ScreenEvent>())
                    cmmnService.deleteCase(any())
                    zaakVariabelenService.deleteAllCaseVariables(any())
                    indexingService.removeZaak(any())
                    indexingService.removeTaak(any())
                    eventingService.send(any<ScreenEvent>())
                    signaleringService.deleteSignaleringen(any())
                    signaleringService.deleteSignaleringVerzonden(any())
                }
            }
        }
    }
})
