/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.slot
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.zac.app.informatieobjecten.model.RESTDocumentCreatieGegevens
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.documentcreatie.model.DocumentCreatieGegevens
import net.atos.zac.documentcreatie.model.createDocumentCreatieResponse
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.ZaakRechten

class InformatieObjectenRESTServiceTest : BehaviorSpec() {
    val documentCreatieService = mockk<DocumentCreatieService>()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not support constructor injection for JAX-RS REST services completely.
    @InjectMockKs
    lateinit var informatieObjectenRESTService: InformatieObjectenRESTService

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
    }

    init {
        given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
            When("createDocument is called") {
                then("the document creation service is called to create the document") {
                    val zaak = createZaak()
                    val zaakRechten =
                        ZaakRechten(false, true, false, false, false, false, false, false)
                    val restDocumentCreatieGegevens = RESTDocumentCreatieGegevens().apply {
                        zaakUUID = zaak.uuid
                        taskId = "dummyTaskId"
                    }
                    val documentCreatieResponse = createDocumentCreatieResponse()
                    val documentCreatieGegevens = slot<DocumentCreatieGegevens>()

                    every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                    every { policyService.readZaakRechten(zaak) } returns zaakRechten
                    every { ztcClientService.readInformatieobjecttypen(zaak.zaaktype) } returns listOf(
                        createInformatieObjectType(omschrijving = "bijlage")
                    )
                    every {
                        documentCreatieService.creeerDocumentAttendedSD(capture(documentCreatieGegevens))
                    } returns documentCreatieResponse

                    val restDocumentCreatieResponse =
                        informatieObjectenRESTService.createDocument(restDocumentCreatieGegevens)

                    restDocumentCreatieResponse.message shouldBe null
                    restDocumentCreatieResponse.redirectURL shouldBe documentCreatieResponse.redirectUrl
                    with(documentCreatieGegevens.captured) {
                        this.zaak shouldBe zaak
                        this.taskId shouldBe restDocumentCreatieGegevens.taskId
                        this.informatieobjecttype.omschrijving shouldBe "bijlage"
                    }
                }
            }
        }
    }
}
