/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import net.atos.zac.admin.ZaakafhandelParameterBeheerService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.zoeken.IndexingService
import java.net.URI
import java.util.UUID

const val SECRET = "dummySecret"

class NotificationReceiverTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val cmmnService = mockk<CMMNService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
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
            resourceUrl = URI("http://example.com/dummyproductaanvraag/$productaanvraagObjectUUID"),
            properties = mutableMapOf("objectType" to "http://example.com/dummyproducttype/$productTypeUUID")
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
        val zaaktypeUri = URI("http://example.com/dummyzaaktype/$zaaktypeUUID")
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
        val zaaktypeUri = URI("http://example.com/dummyzaaktype/$zaaktypeUUID")
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
        val zaaktypeUri = URI("http://example.com/dummyzaaktype/$zaaktypeUUID")
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
    Given("A CMMN case and a request containing an authorization header and a 'zaak destroy' notificatie") {
        val zaakUUID = UUID.randomUUID()
        val zaakUri = URI("http://example.com/dummyzaak/$zaakUUID")
        val notificatie = createNotificatie(
            channel = Channel.ZAKEN,
            resource = Resource.ZAAK,
            resourceUrl = zaakUri,
            action = Action.DELETE
        )
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        every { cmmnService.deleteCase(zaakUUID) } returns Unit
        every { zaakVariabelenService.deleteAllCaseVariables(zaakUUID) } just Runs
        every { indexingService.removeZaak(zaakUUID) } just Runs

        When("notificatieReceive is called with the zaak destroy notificatie") {
            val response = notificationReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the CMMN case is successfully deleted and the zaak is removed from the search index"
            ) {
                response.status shouldBe Response.Status.NO_CONTENT.statusCode
                verify(exactly = 1) {
                    cmmnService.deleteCase(zaakUUID)
                    zaakVariabelenService.deleteAllCaseVariables(zaakUUID)
                    indexingService.removeZaak(zaakUUID)
                }
            }
        }
    }
})
