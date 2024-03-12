/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.servlet.http.HttpSession
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectData
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockData
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.zac.app.informatieobjecten.InformatieObjectenRESTService.FILE_SESSION_ATTRIBUTE_PREFIX
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter.convertToEnkelvoudigInformatieObject
import net.atos.zac.app.informatieobjecten.model.RESTDocumentCreatieGegevens
import net.atos.zac.app.informatieobjecten.model.createRESTEnkelvoudigInformatieObjectVersieGegevens
import net.atos.zac.app.informatieobjecten.model.createRESTEnkelvoudigInformatieobject
import net.atos.zac.app.informatieobjecten.model.createRESTFileUpload
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.documentcreatie.model.DocumentCreatieGegevens
import net.atos.zac.documentcreatie.model.createDocumentCreatieResponse
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.ZaakRechten
import net.atos.zac.policy.output.createDocumentRechten

class InformatieObjectenRESTServiceTest : BehaviorSpec() {
    private val documentCreatieService = mockk<DocumentCreatieService>()
    private val drcClientService = mockk<DRCClientService>()
    private val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    private val httpSessionInstance = mockk<Instance<HttpSession>>()
    private val httpSession = mockk<HttpSession>()
    private val policyService = mockk<PolicyService>()
    private val restInformatieobjectConverter = mockk<RESTInformatieobjectConverter>()
    private val zgwApiService = mockk<ZGWApiService>()
    private val zrcClientService = mockk<ZRCClientService>()
    private val ztcClientService = mockk<ZTCClientService>()

    private val zaakRechtenWijzigen =
        ZaakRechten(false, true, false, false, false, false, false, false)

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not support constructor injection for JAX-RS REST services completely.
    @InjectMockKs
    lateinit var informatieObjectenRESTService: InformatieObjectenRESTService

    override suspend fun beforeContainer(testCase: TestCase) {
        super.beforeContainer(testCase)

        // Only run before Given
        if (testCase.parent != null) return

        MockKAnnotations.init(this)
        clearAllMocks()
    }

    init {
        Given("document creation data is provided and zaaktype can use the 'bijlage' informatieobjecttype") {
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

            When("createDocument is called by a role that is allowed to change the zaak") {
                val restDocumentCreatieResponse =
                    informatieObjectenRESTService.createDocument(restDocumentCreatieGegevens)

                Then("the document creation service is called to create the document") {
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

        Given("an enkelvoudig informatieobject has been uploaded, and the zaak is open") {
            val zaak = createZaak()
            val documentReferentieId: String = "dummyDocumentReferentieId"
            val restEnkelvoudigInformatieobject = createRESTEnkelvoudigInformatieobject()
            val responseRestEnkelvoudigInformatieobject = createRESTEnkelvoudigInformatieobject()
            val restFileUpload = createRESTFileUpload()
            val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectData()
            val zaakInformatieobject = createZaakInformatieobject()
            val httpSessionFileAttribute = "$FILE_SESSION_ATTRIBUTE_PREFIX$documentReferentieId"

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
                restInformatieobjectConverter.convertZaakObject(restEnkelvoudigInformatieobject)
            } returns enkelvoudigInformatieObjectData
            every {
                restInformatieobjectConverter.convertToREST(zaakInformatieobject)
            } returns responseRestEnkelvoudigInformatieobject
            every {
                zgwApiService.createZaakInformatieobjectForZaak(
                    zaak,
                    enkelvoudigInformatieObjectData,
                    enkelvoudigInformatieObjectData.titel,
                    enkelvoudigInformatieObjectData.beschrijving,
                    "geen"
                )
            } returns zaakInformatieobject

            When("createEnkelvoudigInformatieobject is called by a role that is allowed to change the zaak") {
                val returnedRESTEnkelvoudigInformatieobject =
                    informatieObjectenRESTService.createEnkelvoudigInformatieobjectWithUploadedFile(
                        zaak.uuid,
                        documentReferentieId,
                        false,
                        restEnkelvoudigInformatieobject
                    )

                Then("the enkelvoudig informatieobject is added to the zaak") {
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

            When("createEnkelvoudigInformatieobject is called but the ZGW client service throws an exception") {
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
                    informatieObjectenRESTService.createEnkelvoudigInformatieobjectWithUploadedFile(
                        zaak.uuid,
                        documentReferentieId,
                        false,
                        restEnkelvoudigInformatieobject
                    )
                }

                Then("the enkelvoudig informatieobject is not added to the zaak but is removed from the HTTP session") {
                    verify(exactly = 1) {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            zaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                    }
                }
            }

            When("file for upload is provided") {
                restEnkelvoudigInformatieobject.file = restFileUpload.file
                restEnkelvoudigInformatieobject.formaat = restFileUpload.type

                val returnedRESTEnkelvoudigInformatieobject =
                    informatieObjectenRESTService.createEnkelvoudigInformatieobjectAndUploadFile(
                        zaak.uuid,
                        documentReferentieId,
                        false,
                        restEnkelvoudigInformatieobject,
                    )

                Then("the enkelvoudig informatieobject is added to the zaak") {
                    returnedRESTEnkelvoudigInformatieobject shouldBe responseRestEnkelvoudigInformatieobject
                    verify(exactly = 1) {
                        zgwApiService.createZaakInformatieobjectForZaak(
                            zaak,
                            enkelvoudigInformatieObjectData,
                            enkelvoudigInformatieObjectData.titel,
                            enkelvoudigInformatieObjectData.beschrijving,
                            "geen"
                        )
                    }
                }
            }
        }

        Given("an enkelvoudig informatieobject has been uploaded, and the zaak is closed") {
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
            val httpSessionFileAttribute = "$FILE_SESSION_ATTRIBUTE_PREFIX$documentReferentieId"

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

            When("createEnkelvoudigInformatieobject is called by a role that is allowed to change the zaak") {
                val returnedRESTEnkelvoudigInformatieobject =
                    informatieObjectenRESTService.createEnkelvoudigInformatieobjectWithUploadedFile(
                        closedZaak.uuid,
                        documentReferentieId,
                        false,
                        restEnkelvoudigInformatieobject
                    )

                Then("the enkelvoudig informatieobject is added to the zaak") {
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

        Given("enkelvoudig informatieobject has been uploaded, and the zaak is open") {
            val zaak = createZaak()
            val restEnkelvoudigInformatieobject = createRESTEnkelvoudigInformatieobject()
            val enkelvoudigInformatieObjectWithLockData = createEnkelvoudigInformatieObjectWithLockData()
            val restFileUpload = createRESTFileUpload()
            val restEnkelvoudigInformatieObjectVersieGegevens =
                createRESTEnkelvoudigInformatieObjectVersieGegevens(zaakUuid = zaak.uuid)
            val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
            val httpSessionFileAttribute = "$FILE_SESSION_ATTRIBUTE_PREFIX${restEnkelvoudigInformatieObjectVersieGegevens.zaakUuid}"
            val documentRechten = createDocumentRechten()

            every {
                drcClientService.readEnkelvoudigInformatieobject(restEnkelvoudigInformatieObjectVersieGegevens.uuid)
            } returns enkelvoudigInformatieObject
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every { policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak) } returns documentRechten
            every { httpSessionInstance.get() } returns httpSession
            every { httpSession.getAttribute(httpSessionFileAttribute) } returns restFileUpload
            every { httpSession.removeAttribute(httpSessionFileAttribute) } just runs
            every {
                restInformatieobjectConverter.convert(
                    restEnkelvoudigInformatieObjectVersieGegevens,
                    restFileUpload
                )
            } returns enkelvoudigInformatieObjectWithLockData
            every {
                enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                    parseUUIDFromResourceURI(enkelvoudigInformatieObject.url),
                    enkelvoudigInformatieObjectWithLockData,
                    null
                )
            } returns enkelvoudigInformatieObjectWithLockData
            mockkStatic(RESTInformatieobjectConverter::convertToEnkelvoudigInformatieObject)
            every {
                convertToEnkelvoudigInformatieObject(enkelvoudigInformatieObjectWithLockData)
            } returns enkelvoudigInformatieObject
            every {
                restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject)
            } returns restEnkelvoudigInformatieobject

            When("updateEnkelvoudigInformatieobject is called") {
                val returnedRESTEnkelvoudigInformatieobject =
                    informatieObjectenRESTService.updateEnkelvoudigInformatieobject(
                        restEnkelvoudigInformatieObjectVersieGegevens,
                        null
                    )

                Then("the enkelvoudig informatieobject is updated") {
                    returnedRESTEnkelvoudigInformatieobject shouldBe restEnkelvoudigInformatieobject
                    verify(exactly = 1) {
                        drcClientService.readEnkelvoudigInformatieobject(
                            restEnkelvoudigInformatieObjectVersieGegevens.uuid
                        )
                        enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                            parseUUIDFromResourceURI(enkelvoudigInformatieObject.url),
                            enkelvoudigInformatieObjectWithLockData,
                            null
                        )
                        httpSession.removeAttribute(httpSessionFileAttribute)
                    }
                }
            }

            When("updateEnkelvoudigInformatieobject is called with file to upload") {
                val returnedRESTEnkelvoudigInformatieobject =
                    informatieObjectenRESTService.updateEnkelvoudigInformatieobject(
                        restEnkelvoudigInformatieObjectVersieGegevens,
                        restFileUpload
                    )

                Then("the enkelvoudig informatieobject is updated") {
                    returnedRESTEnkelvoudigInformatieobject shouldBe restEnkelvoudigInformatieobject
                    verify(exactly = 1) {
                        drcClientService.readEnkelvoudigInformatieobject(
                            restEnkelvoudigInformatieObjectVersieGegevens.uuid
                        )
                        enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                            parseUUIDFromResourceURI(enkelvoudigInformatieObject.url),
                            enkelvoudigInformatieObjectWithLockData,
                            null
                        )
                    }
                }
            }
        }
    }

    override fun isolationMode() = IsolationMode.InstancePerTest
}
