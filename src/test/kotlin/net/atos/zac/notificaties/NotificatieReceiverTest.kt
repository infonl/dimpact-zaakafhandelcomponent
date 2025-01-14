/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.notificaties

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.core.HttpHeaders
import net.atos.client.or.`object`.model.createObjecttype
import net.atos.client.or.objecttype.ObjecttypesClientService
import net.atos.zac.admin.ZaakafhandelParameterBeheerService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.zoeken.IndexingService
import java.net.URI
import java.util.UUID

const val SECRET = "dummySecret"

@MockKExtension.CheckUnnecessaryStub
class NotificatieReceiverTest : BehaviorSpec({
    val eventingService = mockk<EventingService>()
    val productaanvraagService = mockk<ProductaanvraagService>()
    val indexingService = mockk<IndexingService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val objecttypesClientService = mockk<ObjecttypesClientService>()

    val httpHeaders = mockk<HttpHeaders>()
    val httpSession = mockk<HttpSession>(relaxed = true)
    val httpSessionInstance = mockk<Instance<HttpSession>>()

    val notificatieReceiver = NotificatieReceiver(
        eventingService,
        productaanvraagService,
        indexingService,
        inboxDocumentenService,
        zaakafhandelParameterBeheerService,
        objecttypesClientService,
        SECRET,
        httpSessionInstance
    )

    // HTTP session mock

    Given(
        """
            a request containing a authorization header and
            a productaanvraag notificatie with a object type UUID for the productaanvraag object type
            """
    ) {
        val productaanvraagObjectUUID = UUID.randomUUID()
        val productTypeUUID = UUID.randomUUID()
        val objectType = createObjecttype(name = "Productaanvraag-Dimpact")
        every { objecttypesClientService.readObjecttype(productTypeUUID) } returns objectType
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        val notificatie = createNotificatie(
            resourceUrl = URI("http://example.com/dummyproductaanvraag/$productaanvraagObjectUUID"),
            properties = mapOf("objectType" to "http://example.com/dummyproducttype/$productTypeUUID")
        )
        every { productaanvraagService.handleProductaanvraag(productaanvraagObjectUUID) } just runs

        When("notificatieReceive is called") {
            val response = notificatieReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the 'functional user' is added to the HTTP sessionm the productaanvraag service is invoked " +
                    "and a 'no content' response is returned"
            ) {
                response.status shouldBe 204
                verify(exactly = 1) {
                    productaanvraagService.handleProductaanvraag(productaanvraagObjectUUID)
                }
            }
        }
    }

    Given(
        "a request containing a authorization header and a zaaktype create notificatie"
    ) {
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("http://example.com/dummyzaaktype/$zaaktypeUUID")
        val notificatie = createNotificatie(
            resource = Resource.ZAAKTYPE,
            resourceUrl = zaaktypeUri
        )
        every { zaakafhandelParameterBeheerService.upsertZaaktype(zaaktypeUri) } just runs

        When("notificatieReceive is called with the zaaktype create notificatie") {
            val response = notificatieReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the zaaktype aanvraag service is invoked and a 'no content' response is returned"
            ) {
                response.status shouldBe 204
                verify(exactly = 1) {
                    zaakafhandelParameterBeheerService.upsertZaaktype(zaaktypeUri)
                }
            }
        }
    }

    Given(
        "a request containing a authorization header and a zaaktype update notificatie"
    ) {
        every { httpHeaders.getHeaderString(eq(HttpHeaders.AUTHORIZATION)) } returns SECRET
        every { httpSessionInstance.get() } returns httpSession
        val zaaktypeUUID = UUID.randomUUID()
        val zaaktypeUri = URI("http://example.com/dummyzaaktype/$zaaktypeUUID")
        val notificatie = createNotificatie(
            resource = Resource.ZAAKTYPE,
            resourceUrl = zaaktypeUri,
            action = Action.UPDATE
        )
        every { zaakafhandelParameterBeheerService.upsertZaaktype(zaaktypeUri) } just runs

        When("notificatieReceive is called with the zaaktype create notificatie") {
            val response = notificatieReceiver.notificatieReceive(httpHeaders, notificatie)

            Then(
                "the zaaktype aanvraag service is invoked and a 'no content' response is returned"
            ) {
                response.status shouldBe 204
                verify(exactly = 1) {
                    zaakafhandelParameterBeheerService.upsertZaaktype(zaaktypeUri)
                }
            }
        }
    }
})
