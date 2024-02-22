/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectData
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RESTDocumentCreatieGegevens
import net.atos.zac.app.informatieobjecten.model.createRESTEnkelvoudigInformatieobject
import net.atos.zac.app.informatieobjecten.model.createRESTFileUpload
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.documentcreatie.model.DocumentCreatieGegevens
import net.atos.zac.documentcreatie.model.createDocumentCreatieResponse
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.ZaakRechten

class InformatieObjectenRESTServiceTest : BehaviorSpec() {
    private val documentCreatieService = mockk<DocumentCreatieService>()
    private val restInformatieobjectConverter = mockk<RESTInformatieobjectConverter>()
    private val policyService = mockk<PolicyService>()
    private val zgwApiService = mockk<ZGWApiService>()
    private val zrcClientService = mockk<ZRCClientService>()
    private val ztcClientService = mockk<ZTCClientService>()
    private val httpSessionInstance = mockk<Instance<HttpSession>>()
    private val httpSession = mockk<HttpSession>()

    private val zaakRechtenWijzigen =
        ZaakRechten(false, true, false, false, false, false, false, false)

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not support constructor injection for JAX-RS REST services completely.
    @InjectMockKs
    lateinit var informatieObjectenRESTService: InformatieObjectenRESTService

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
        clearAllMocks()
    }

    init {
        given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
            When("createDocument is called by a role that is allowed to change the zaak") {
                then("the document creation service is called to create the document") {
                    val zaak = createZaak()
                    val restDocumentCreatieGegevens = RESTDocumentCreatieGegevens().apply {
                        zaakUUID = zaak.uuid
                        taskId = "dummyTaskId"
                    }
                    val documentCreatieResponse = createDocumentCreatieResponse()
                    val documentCreatieGegevens = slot<DocumentCreatieGegevens>()

                    every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                    every { policyService.readZaakRechten(zaak) } returns zaakRechtenWijzigen
                    every { ztcClientService.readInformatieobjecttypen(zaak.zaaktype) } returns listOf(
                        createInformatieObjectType(omschrijving = "bijlage")
                    )
                    every {
                        documentCreatieService.creeerDocumentAttendedSD(
                            capture(
                                documentCreatieGegevens
                            )
                        )
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
        given("an enkelvoudig informatieobject has been uploaded, and the zaak is open") {
            When("createEnkelvoudigInformatieobject is called by a role that is allowed to change the zaak") {
                then("the enkelvoudig informatieobject is added to the zaak") {
                    val zaak = createZaak()
                    val documentReferentieId = "dummyDocumentReferentieId"
                    val restEnkelvoudigInformatieobject = createRESTEnkelvoudigInformatieobject()
                    val responseRestEnkelvoudigInformatieobject =
                        createRESTEnkelvoudigInformatieobject()
                    val restFileUpload = createRESTFileUpload()
                    val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectData()
                    val zaakInformatieobject = createZaakInformatieobject()
                    val httpSessionFileAttribute = "FILE_$documentReferentieId"

                    every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                    every { policyService.readZaakRechten(zaak) } returns zaakRechtenWijzigen
                    every { httpSessionInstance.get() } returns httpSession
                    every { httpSession.getAttribute(httpSessionFileAttribute) } returns restFileUpload
                    every { httpSession.removeAttribute(httpSessionFileAttribute) } just runs
                    every {
                        restInformatieobjectConverter.convertZaakObject(
                            restEnkelvoudigInformatieobject,
                            restFileUpload
                        )
                    } returns enkelvoudigInformatieObjectData
                    every {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            zaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                    } returns zaakInformatieobject
                    every {
                        restInformatieobjectConverter.convertToREST(zaakInformatieobject)
                    } returns responseRestEnkelvoudigInformatieobject

                    val returnedRESTEnkelvoudigInformatieobject =
                        informatieObjectenRESTService.createEnkelvoudigInformatieobject(
                            zaak.uuid,
                            documentReferentieId,
                            false,
                            restEnkelvoudigInformatieobject
                        )

                    returnedRESTEnkelvoudigInformatieobject shouldBe responseRestEnkelvoudigInformatieobject
                    verify(exactly = 1) {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            zaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                        httpSession.removeAttribute(httpSessionFileAttribute)
                    }
                }
            }
        }
        given("an enkelvoudig informatieobject has been uploaded, and the zaak is open") {
            When("createEnkelvoudigInformatieobject is called but the ZGW client service throws an exception") {
                then("the enkelvoudig informatieobject is not added to the zaak but is removed from the HTTP session") {
                    val zaak = createZaak()
                    val documentReferentieId = "dummyDocumentReferentieId"
                    val restEnkelvoudigInformatieobject = createRESTEnkelvoudigInformatieobject()
                    val restFileUpload = createRESTFileUpload()
                    val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectData()
                    val httpSessionFileAttribute = "FILE_$documentReferentieId"

                    every { zrcClientService.readZaak(zaak.uuid) } returns zaak
                    every { policyService.readZaakRechten(zaak) } returns zaakRechtenWijzigen
                    every { httpSessionInstance.get() } returns httpSession
                    every { httpSession.getAttribute(httpSessionFileAttribute) } returns restFileUpload
                    every { httpSession.removeAttribute(httpSessionFileAttribute) } just runs
                    every {
                        restInformatieobjectConverter.convertZaakObject(
                            restEnkelvoudigInformatieobject,
                            restFileUpload
                        )
                    } returns enkelvoudigInformatieObjectData
                    every {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            zaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                    } throws RuntimeException("dummy exception")

                    shouldThrow<RuntimeException> {
                        informatieObjectenRESTService.createEnkelvoudigInformatieobject(
                            zaak.uuid,
                            documentReferentieId,
                            false,
                            restEnkelvoudigInformatieobject
                        )
                    }

                    verify(exactly = 1) {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            zaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                        httpSession.removeAttribute(httpSessionFileAttribute)
                    }
                }
            }
        }
        given("an enkelvoudig informatieobject has been uploaded, and the zaak is closed") {
            When("createEnkelvoudigInformatieobject is called by a role that is allowed to change the zaak") {
                then("the enkelvoudig informatieobject is added to the zaak") {
                    val closedZaak = createZaak(
                        archiefnominatie = Archiefnominatie.VERNIETIGEN
                    )
                    val documentReferentieId = "dummyDocumentReferentieId"
                    val restEnkelvoudigInformatieobject = createRESTEnkelvoudigInformatieobject()
                    val responseRestEnkelvoudigInformatieobject =
                        createRESTEnkelvoudigInformatieobject()
                    val restFileUpload = createRESTFileUpload()
                    val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectData()
                    val zaakInformatieobject = createZaakInformatieobject()
                    val httpSessionFileAttribute = "FILE_$documentReferentieId"

                    every { zrcClientService.readZaak(closedZaak.uuid) } returns closedZaak
                    every { policyService.readZaakRechten(closedZaak) } returns zaakRechtenWijzigen
                    every { httpSessionInstance.get() } returns httpSession
                    every { httpSession.getAttribute(httpSessionFileAttribute) } returns restFileUpload
                    every { httpSession.removeAttribute(httpSessionFileAttribute) } just runs
                    every {
                        restInformatieobjectConverter.convertZaakObject(
                            restEnkelvoudigInformatieobject,
                            restFileUpload
                        )
                    } returns enkelvoudigInformatieObjectData
                    every {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            closedZaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                    } returns zaakInformatieobject
                    every {
                        restInformatieobjectConverter.convertToREST(zaakInformatieobject)
                    } returns responseRestEnkelvoudigInformatieobject

                    val returnedRESTEnkelvoudigInformatieobject =
                        informatieObjectenRESTService.createEnkelvoudigInformatieobject(
                            closedZaak.uuid,
                            documentReferentieId,
                            false,
                            restEnkelvoudigInformatieobject
                        )

                    returnedRESTEnkelvoudigInformatieobject shouldBe responseRestEnkelvoudigInformatieobject
                    verify(exactly = 1) {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            closedZaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                        httpSession.removeAttribute(httpSessionFileAttribute)
                    }
                }
            }
        }
    }
}
