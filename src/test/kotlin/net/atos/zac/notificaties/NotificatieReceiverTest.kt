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

    Given(
        "a request containing a authorization header, a productaanvraag notificatie with a object type UUID " +
            "for the productaanvraag object type"
    ) {
        When("notificatieReceive is called") {
            Then(
                "the 'functional user' is added to the HTTP sessionm the productaanvraag service is invoked " +
                    "and a 'no content' response is returned"
            ) {
                val secret = "dummySecret"
                val productaanvraagObjectUUID = UUID.randomUUID()
                val productTypeUUID = UUID.randomUUID()
                val httpHeaders = mockk<HttpHeaders>()
                val httpSession = mockk<HttpSession>()
                val notifcatie = createNotificatie(
                    resourceUrl = URI("http://example.com/dummyproductaanvraag/$productaanvraagObjectUUID"),
                    properties = mapOf("objectType" to "http://example.com/dummyproducttype/$productTypeUUID")
                )
                val objectType = createObjecttype(name = "Productaanvraag-Dimpact")
                every { httpHeaders.getHeaderString("Authorization") } returns secret
                every { httpSessionInstance.get() } returns httpSession
                every { httpSession.setAttribute("logged-in-user", any()) } just runs
                every { objecttypesClientService.readObjecttype(productTypeUUID) } returns objectType
                every { productaanvraagService.handleProductaanvraag(productaanvraagObjectUUID) } just runs

                val response = notificatieReceiver.notificatieReceive(httpHeaders, notifcatie)

                response.status shouldBe 204
                verify(exactly = 1) {
                    productaanvraagService.handleProductaanvraag(productaanvraagObjectUUID)
                }
            }
        }
    }
})
