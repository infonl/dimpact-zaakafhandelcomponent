/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.StreamingOutput
import net.atos.zac.document.DetachedDocumentService
import net.atos.zac.document.InboxDocumentService
import net.atos.zac.document.model.DetachedDocument
import net.atos.zac.document.model.InboxDocument
import net.atos.zac.event.EventingService
import net.atos.zac.webdav.WebdavHelper
import net.atos.zac.websocket.event.ScreenEvent
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.exception.RestExceptionMapper
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import nl.info.zac.app.informatieobjecten.converter.RestInformatieobjecttypeConverter
import nl.info.zac.app.informatieobjecten.exception.EnkelvoudigInformatieObjectConversionException
import nl.info.zac.app.informatieobjecten.model.RestDocumentVerplaatsGegevens
import nl.info.zac.app.informatieobjecten.model.RestDocumentVerwijderenGegevens
import nl.info.zac.app.informatieobjecten.model.RestOndertekening
import nl.info.zac.app.informatieobjecten.model.createRestDocumentVerzendGegevens
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieObjectVersieGegevens
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import nl.info.zac.app.informatieobjecten.model.createRestFileUpload
import nl.info.zac.app.informatieobjecten.model.createRestInformatieobjectZoekParameters
import nl.info.zac.app.informatieobjecten.model.createRestInformatieobjecttype
import nl.info.zac.app.zaak.converter.RestGerelateerdeZaakConverter
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createDocumentRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.search.model.DocumentIndicatie
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LargeClass")
class EnkelvoudigInformatieObjectRestServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectDownloadService = mockk<EnkelvoudigInformatieObjectDownloadService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val enkelvoudigInformatieObjectConvertService = mockk<EnkelvoudigInformatieObjectConvertService>()
    val eventingService = mockk<EventingService>()
    val inboxDocumentService = mockk<InboxDocumentService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val detachedDocumentService = mockk<DetachedDocumentService>()
    val policyService = mockk<PolicyService>()
    val restGerelateerdeZaakConverter = mockk<RestGerelateerdeZaakConverter>()
    val zaakHistoryLineConverter = mockk<ZaakHistoryLineConverter>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val restInformatieobjecttypeConverter = mockk<RestInformatieobjecttypeConverter>()
    val webdavHelper = mockk<WebdavHelper>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val enkelvoudigInformatieObjectRestService = EnkelvoudigInformatieObjectRestService(
        drcClientService = drcClientService,
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        zgwApiService = zgwApiService,
        detachedDocumentService = detachedDocumentService,
        inboxDocumentService = inboxDocumentService,
        enkelvoudigInformatieObjectLockService = enkelvoudigInformatieObjectLockService,
        eventingService = eventingService,
        restInformatieobjectConverter = restInformatieobjectConverter,
        restInformatieobjecttypeConverter = restInformatieobjecttypeConverter,
        zaakHistoryLineConverter = zaakHistoryLineConverter,
        restGerelateerdeZaakConverter = restGerelateerdeZaakConverter,
        loggedInUserInstance = loggedInUserInstance,
        webdavHelper = webdavHelper,
        policyService = policyService,
        enkelvoudigInformatieObjectDownloadService = enkelvoudigInformatieObjectDownloadService,
        enkelvoudigInformatieObjectUpdateService = enkelvoudigInformatieObjectUpdateService,
        enkelvoudigInformatieObjectConvertService = enkelvoudigInformatieObjectConvertService
    )

    isolationMode = IsolationMode.InstancePerTest

    Given("an enkelvoudig informatieobject has been uploaded, and the zaak is open") {
        val zaak = createZaak()
        val documentReferentieId = "fakeDocumentReferentieId"
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val responseRestEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val restFileUpload = createRestFileUpload()
        val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every {
            restInformatieobjectConverter.convertEnkelvoudigInformatieObject(restEnkelvoudigInformatieobject)
        } returns enkelvoudigInformatieObjectData
        every {
            restInformatieobjectConverter.convertToREST(zaakInformatieobject)
        } returns responseRestEnkelvoudigInformatieobject
        every {
            enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                zaak,
                enkelvoudigInformatieObjectData
            )
        } returns zaakInformatieobject
        every { loggedInUserInstance.get() } returns loggedInUser

        When(
            "the enkelvoudig informatieobject update is done by a role that is allowed to change the zaak"
        ) {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                toevoegenDocument = true
            )

            val returnedRESTEnkelvoudigInformatieobject =
                enkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile(
                    zaak.uuid,
                    documentReferentieId,
                    false,
                    restEnkelvoudigInformatieobject
                )

            Then("the enkelvoudig informatieobject is added to the zaak") {
                returnedRESTEnkelvoudigInformatieobject shouldBe responseRestEnkelvoudigInformatieobject
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                        zaak,
                        enkelvoudigInformatieObjectData
                    )
                }
            }
        }

        When(
            "the enkelvoudig informatieobject update is triggered but the ZGW client service throws an exception"
        ) {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                toevoegenDocument = true
            )
            every {
                enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                    zaak,
                    enkelvoudigInformatieObjectData
                )
            } throws RuntimeException("fake exception")

            shouldThrow<RuntimeException> {
                enkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile(
                    zaak.uuid,
                    documentReferentieId,
                    false,
                    restEnkelvoudigInformatieobject
                )
            }

            Then("the enkelvoudig informatieobject is not added to the zaak but is removed from the HTTP session") {
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                        zaak,
                        enkelvoudigInformatieObjectData
                    )
                }
            }
        }

        When("the enkelvoudig informatieobject is updated") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny(
                toevoegenDocument = true
            )
            restEnkelvoudigInformatieobject.file = restFileUpload.file
            restEnkelvoudigInformatieobject.formaat = restFileUpload.type

            val returnedRESTEnkelvoudigInformatieobject =
                enkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile(
                    zaak.uuid,
                    documentReferentieId,
                    false,
                    restEnkelvoudigInformatieobject,
                )

            Then("the enkelvoudig informatieobject is added to the zaak") {
                returnedRESTEnkelvoudigInformatieobject shouldBe responseRestEnkelvoudigInformatieobject
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                        zaak,
                        enkelvoudigInformatieObjectData
                    )
                }
            }
        }

        When("enkelvoudig informatieobject is updated by a user that has no access") {
            every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                enkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile(
                    zaak.uuid,
                    documentReferentieId,
                    false,
                    restEnkelvoudigInformatieobject,
                )
            }

            Then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }
    }

    Given("an enkelvoudig informatieobject has been uploaded, and the zaak is closed") {
        val closedZaak = createZaak(
            archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
        )
        val documentReferentieId = "fakeDocumentReferentieId"
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val responseRestEnkelvoudigInformatieobject =
            createRestEnkelvoudigInformatieobject()
        val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.readZaak(closedZaak.uuid) } returns closedZaak
        every {
            restInformatieobjectConverter.convertEnkelvoudigInformatieObject(restEnkelvoudigInformatieobject)
        } returns enkelvoudigInformatieObjectData
        every {
            enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                closedZaak,
                enkelvoudigInformatieObjectData
            )
        } returns zaakInformatieobject
        every {
            restInformatieobjectConverter.convertToREST(zaakInformatieobject)
        } returns responseRestEnkelvoudigInformatieobject
        every { loggedInUserInstance.get() } returns loggedInUser

        When(
            "the enkelvoudig informatieobject is updated by a role that is allowed to change the zaak"
        ) {
            every { policyService.readZaakRechten(closedZaak, loggedInUser) } returns createZaakRechtenAllDeny(
                toevoegenDocument = true
            )

            val returnedRESTEnkelvoudigInformatieobject =
                enkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile(
                    closedZaak.uuid,
                    documentReferentieId,
                    false,
                    restEnkelvoudigInformatieobject
                )

            Then("the enkelvoudig informatieobject is added to the zaak") {
                returnedRESTEnkelvoudigInformatieobject shouldBe responseRestEnkelvoudigInformatieobject
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
                        closedZaak,
                        enkelvoudigInformatieObjectData
                    )
                }
            }
        }
    }

    Given("enkelvoudig informatieobject has been uploaded, and the zaak is open") {
        val zaak = createZaak()
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val enkelvoudigInformatieObjectWithLockData = createEnkelvoudigInformatieObjectWithLockRequest()
        val restEnkelvoudigInformatieObjectVersieGegevens =
            createRestEnkelvoudigInformatieObjectVersieGegevens(zaakUuid = zaak.uuid)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every {
            drcClientService.readEnkelvoudigInformatieobject(restEnkelvoudigInformatieObjectVersieGegevens.uuid!!)
        } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every {
            restInformatieobjectConverter.convert(restEnkelvoudigInformatieObjectVersieGegevens)
        } returns enkelvoudigInformatieObjectWithLockData
        every {
            enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                enkelvoudigInformatieObject.url.extractUuid(),
                enkelvoudigInformatieObjectWithLockData,
                null
            )
        } returns enkelvoudigInformatieObject
        every {
            restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject)
        } returns restEnkelvoudigInformatieobject

        When("the enkelvoudig informatieobject is updated from user with access") {
            every {
                policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
            } returns createDocumentRechtenAllDeny(toevoegenNieuweVersie = true)

            val returnedRESTEnkelvoudigInformatieobject =
                enkelvoudigInformatieObjectRestService.updateEnkelvoudigInformatieobjectAndUploadFile(
                    restEnkelvoudigInformatieObjectVersieGegevens
                )

            Then("the changes are stored in the backing services") {
                returnedRESTEnkelvoudigInformatieobject shouldBe restEnkelvoudigInformatieobject
                verify(exactly = 1) {
                    drcClientService.readEnkelvoudigInformatieobject(
                        restEnkelvoudigInformatieObjectVersieGegevens.uuid!!
                    )
                    enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                        enkelvoudigInformatieObject.url.extractUuid(),
                        enkelvoudigInformatieObjectWithLockData,
                        null
                    )
                }
            }
        }

        When("the enkelvoudig informatieobject is updated by a user that has no access") {
            every {
                policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
            } returns createDocumentRechtenAllDeny()

            val exception = shouldThrow<PolicyException> {
                enkelvoudigInformatieObjectRestService.updateEnkelvoudigInformatieobjectAndUploadFile(
                    restEnkelvoudigInformatieObjectVersieGegevens
                )
            }

            Then("it throws exception with no message") {
                exception.message shouldBe null
            }
        }
    }

    Given(
        """
            REST informatieobject zoek parameters with an enkelvoudig informatieobject 
            containing informatie object uuids
            """
    ) {
        val zaakUuid = UUID.randomUUID()
        val informatieobjectUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
        val restInformatieobjectZoekParameters = createRestInformatieobjectZoekParameters(
            informatieobjectUUIDs = informatieobjectUUIDs,
            zaakUuid = zaakUuid
        )
        val zaak = createZaak()
        val restEnkelvoudigInformatieobjecten = listOf(
            createRestEnkelvoudigInformatieobject(),
            createRestEnkelvoudigInformatieobject()
        )

        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every {
            restInformatieobjectConverter.convertUUIDsToREST(informatieobjectUUIDs, zaak)
        } returns restEnkelvoudigInformatieobjecten

        When("the list of enkelvoudig informatieobject is requested") {
            val returnedRestEnkelvoudigInformatieobjecten =
                enkelvoudigInformatieObjectRestService.listEnkelvoudigInformatieobjecten(
                    restInformatieobjectZoekParameters
                )

            Then("the returned enkelvoudige informatie objecten are as expected") {
                with(returnedRestEnkelvoudigInformatieobjecten) {
                    size shouldBe 2
                    this[0] shouldBe restEnkelvoudigInformatieobjecten[0]
                    this[1] shouldBe restEnkelvoudigInformatieobjecten[1]
                }
            }
        }
    }

    Given(
        """
            REST informatieobject zoek parameters with an enkelvoudig informatieobject 
            not containing informatie object uuids, and creating a deel with a deelzaak
            """
    ) {
        val zaakUuid = UUID.randomUUID()
        val besluittypeUuid = UUID.randomUUID()
        val informatieobjectUUID = UUID.randomUUID()
        val restInformatieobjectZoekParameters = createRestInformatieobjectZoekParameters(
            besluittypeUuid = besluittypeUuid,
            gekoppeldeZaakDocumenten = true,
            informatieobjectUUIDs = null,
            zaakUuid = zaakUuid
        )
        val deelzaak = createZaak()
        val zaak = createZaak(
            deelzaken = listOf(deelzaak.url)
        )
        val restEnkelvoudigInformatieobjecten = listOf(
            createRestEnkelvoudigInformatieobject(
                informatieobjectTypeUUID = informatieobjectUUID
            )
        )
        val zaakInformatieobjecten = listOf(
            createZaakInformatieobjectForCreatesAndUpdates()
        )
        val besluitType = createBesluitType(
            url = URI("https://example.com/$besluittypeUuid"),
            informatieobjecttypen = listOf(
                URI("https://example.com/$informatieobjectUUID"),
            )
        )
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
        every { zrcClientService.listZaakinformatieobjecten(zaak) } returns zaakInformatieobjecten
        every {
            restInformatieobjectConverter.convertToREST(zaakInformatieobjecten[0])
        } returns restEnkelvoudigInformatieobjecten[0]
        every { zrcClientService.readZaak(deelzaak.url) } returns deelzaak
        every { zrcClientService.listZaakinformatieobjecten(deelzaak) } returns emptyList()
        every {
            ztcClientService.readBesluittype(restInformatieobjectZoekParameters.besluittypeUUID!!)
        } returns besluitType
        every { loggedInUserInstance.get() } returns loggedInUser

        When("the list of enkelvoudig informatieobject is requested") {
            val returnedRestEnkelvoudigInformatieobjecten =
                enkelvoudigInformatieObjectRestService.listEnkelvoudigInformatieobjecten(
                    restInformatieobjectZoekParameters
                )

            Then("the returned enkelvoudige informatie objecten are as expected") {
                with(returnedRestEnkelvoudigInformatieobjecten) {
                    size shouldBe 1
                    this[0] shouldBe restEnkelvoudigInformatieobjecten[0]
                }
            }
        }
    }
    Given("An enkelvoudig informatieobject") {
        val informatieobjectUUID = UUID.randomUUID()
        val zaak = createZaak()
        val enkelvoudiginformatieobject = createEnkelvoudigInformatieObject()
        val restEnkelvoudigInformatieObjectVersieGegevens =
            createRestEnkelvoudigInformatieObjectVersieGegevens(uuid = informatieobjectUUID)
        every {
            drcClientService.readEnkelvoudigInformatieobject(informatieobjectUUID)
        } returns enkelvoudiginformatieobject
        every { policyService.readDocumentRechten(enkelvoudiginformatieobject) } returns createDocumentRechten()
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every { policyService.readDocumentRechten(enkelvoudiginformatieobject, zaak) } returns createDocumentRechten()
        every {
            restInformatieobjectConverter.convertToRestEnkelvoudigInformatieObjectVersieGegevens(enkelvoudiginformatieobject)
        } returns restEnkelvoudigInformatieObjectVersieGegevens

        When("the current version of the enkelvoudig informatieobject is requested") {
            val returnedEnkelvoudigInformatieObjectVersieGegevens =
                enkelvoudigInformatieObjectRestService.readHuidigeVersieInformatieObject(informatieobjectUUID)

            Then("the current version of the enkelvoudig informatieobject is returned") {
                returnedEnkelvoudigInformatieObjectVersieGegevens shouldBe restEnkelvoudigInformatieObjectVersieGegevens
            }
        }
        When("the enkelvoudig informatieobject is converted") {
            every {
                enkelvoudigInformatieObjectConvertService.convertEnkelvoudigInformatieObjectToPDF(
                    any(), any()
                )
            } just Runs

            val response = enkelvoudigInformatieObjectRestService.convertInformatieObjectToPDF(
                informatieobjectUUID,
                zaak.uuid
            )

            Then("the response should be no content") {
                response.status shouldBe 204
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectConvertService.convertEnkelvoudigInformatieObjectToPDF(
                        enkelvoudiginformatieobject,
                        informatieobjectUUID
                    )
                }
            }
        }
        When("the enkelvoudig informatieobject is converted but an exception was thrown") {
            every {
                enkelvoudigInformatieObjectConvertService.convertEnkelvoudigInformatieObjectToPDF(
                    any(),
                    any()
                )
            } throws EnkelvoudigInformatieObjectConversionException()

            val exception = shouldThrow<EnkelvoudigInformatieObjectConversionException> {
                enkelvoudigInformatieObjectRestService.convertInformatieObjectToPDF(
                    informatieobjectUUID,
                    zaak.uuid
                )
            }

            Then("the response should be an error message") {
                val response = RestExceptionMapper().toResponse(exception)
                response.status shouldBe 400
                val entity = response.entity as String
                entity shouldContain """"message":"msg.error.enkelvoudiginformatieobject.conversion.failed""""
            }
        }
    }
    Given("A zaak with two informatieobjecttypes") {
        val zaak = createZaak()
        val informatieObjectTypeUUID1 = UUID.randomUUID()
        val informatieObjectTypeUUID2 = UUID.randomUUID()
        val informatieObjectTypes = listOf(
            createInformatieObjectType(
                uri = URI("http://example.com/catalogus/$informatieObjectTypeUUID1"),
                omschrijving = "fakeOmschrijving1",
                vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR,
                concept = true
            ),
            createInformatieObjectType(
                uri = URI("http://example.com/catalogus/$informatieObjectTypeUUID2"),
                omschrijving = "fakeOmschrijving2",
                vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.BEPERKT_OPENBAAR,
                concept = false
            )
        )
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        with(ztcClientService) {
            every { readZaaktype(zaak.zaaktype).informatieobjecttypen } returns informatieObjectTypes.map { it.url }
            informatieObjectTypes.forEachIndexed { index, informatieObjectType, ->
                every { readInformatieobjecttype(informatieObjectType.url) } returns informatieObjectTypes[index]
            }
        }

        When("the informatieobjecttypes for the zaak are requested") {
            val returnedRestInformatieobjecttypes = enkelvoudigInformatieObjectRestService.listInformatieobjecttypesForZaak(
                zaak.uuid
            )

            Then("the information object types are returned") {
                returnedRestInformatieobjecttypes.size shouldBe 2
                with(returnedRestInformatieobjecttypes) {
                    with(this[0]) {
                        uuid shouldBe informatieObjectTypeUUID1
                        omschrijving shouldBe "fakeOmschrijving1"
                        vertrouwelijkheidaanduiding shouldBe "OPENBAAR"
                        concept shouldBe true
                    }
                    with(this[1]) {
                        uuid shouldBe informatieObjectTypeUUID2
                        omschrijving shouldBe "fakeOmschrijving2"
                        vertrouwelijkheidaanduiding shouldBe "BEPERKT_OPENBAAR"
                        concept shouldBe false
                    }
                }
            }
        }
    }

    Given(
        """
            Valid REST document verzend gegevens with multiple informatieobjecten each with
            a status 'DEFINITIEF' and a vertrouwelijkheidaanduiding not equal to 'CONFIDENTIEEL', 
            'GEHEIM' or 'ZEER_GEHEIM' and without an ontvangstdatum and with formaat PDF           
        """
    ) {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val enkelvoudigInformatieObjectUuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        val restDocumentVerzendGegevens = createRestDocumentVerzendGegevens(
            zaakUuid = zaakUuid,
            informatieobjecten = enkelvoudigInformatieObjectUuids
        )
        val enkelvoudigeInformatieobjecten = listOf(
            createEnkelvoudigInformatieObject(
                status = StatusEnum.DEFINITIEF,
                vertrouwelijkheidaanduiding = nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum.OPENBAAR,
                ontvangstdatum = null,
                formaat = "application/pdf"
            ),
            createEnkelvoudigInformatieObject(
                status = StatusEnum.DEFINITIEF,
                vertrouwelijkheidaanduiding = nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum.ZAAKVERTROUWELIJK,
                ontvangstdatum = null,
                formaat = "application/pdf"
            )
        )
        val loggedInUser = createLoggedInUser()

        enkelvoudigInformatieObjectUuids.forEachIndexed { index, informatieObjectUuid ->
            every {
                drcClientService.readEnkelvoudigInformatieobject(informatieObjectUuid)
            } returns enkelvoudigeInformatieobjecten[index]
        }
        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser).wijzigen } returns true
        every {
            enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(any(), any(), any())
        } just Runs
        every { loggedInUserInstance.get() } returns loggedInUser

        When("sendDocument is called") {
            enkelvoudigInformatieObjectRestService.sendDocument(restDocumentVerzendGegevens)

            Then("all informatieobjecten are sent") {
                verify(exactly = 2) {
                    enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(any(), any(), any())
                }
            }
        }
    }

    Given("Valid gegevens with an informatieobject that cannot be sent") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val enkelvoudigInformatieObjectUuids = listOf(UUID.randomUUID())
        val restDocumentVerzendGegevens = createRestDocumentVerzendGegevens(
            zaakUuid = zaakUuid,
            informatieobjecten = enkelvoudigInformatieObjectUuids
        )
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val loggedInUser = createLoggedInUser()
        every {
            drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieObjectUuids[0])
        } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser).wijzigen } returns false
        every { loggedInUserInstance.get() } returns loggedInUser

        When("sendDocument is called") {
            Then("an exception is thrown") {
                shouldThrow<PolicyException> {
                    enkelvoudigInformatieObjectRestService.sendDocument(restDocumentVerzendGegevens)
                }
            }
        }
    }

    Given("An existing document and the user has permission to download the document") {
        val uuid = UUID.randomUUID()
        val byteArrayInputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject).downloaden } returns true
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns byteArrayInputStream

        When("readFile is called") {
            val response = enkelvoudigInformatieObjectRestService.readFile(uuid)

            Then("it should return the document content as a response") {
                with(response) {
                    status shouldBe 200
                    headers["Content-Disposition"]!!.first() shouldBe
                        """attachment; filename="${enkelvoudigInformatieObject.bestandsnaam}""""
                    entity shouldBe byteArrayInputStream
                }
            }
        }
    }

    Given("The user does not have permission to download the document") {
        val uuid = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject).downloaden } returns false

        When("readFile is called") {
            val exception = shouldThrow<PolicyException> {
                enkelvoudigInformatieObjectRestService.readFile(uuid)
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }

    Given("An IOException occurs while retrieving the document content") {
        val uuid = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject).downloaden } returns true
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } throws IOException("Failed to retrieve content")

        When("readFile is called") {
            val exception = shouldThrow<RuntimeException> {
                enkelvoudigInformatieObjectRestService.readFile(uuid)
            }

            Then("it should throw a exception") {
                exception.cause.shouldBeInstanceOf<IOException>()
                exception.cause?.message shouldBe "Failed to retrieve content"
            }
        }
    }

    Given("An enkelvoudig informatieobject and a zaak UUID") {
        val uuid = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every {
            restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, zaak)
        } returns restEnkelvoudigInformatieobject

        When("readEnkelvoudigInformatieobject is called with a zaak UUID") {
            val result = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobject(uuid, zaakUUID)

            Then("the REST representation is returned with zaak context") {
                result shouldBe restEnkelvoudigInformatieobject
            }
        }
    }

    Given("An enkelvoudig informatieobject and no zaak UUID") {
        val uuid = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every {
            restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, null)
        } returns restEnkelvoudigInformatieobject

        When("readEnkelvoudigInformatieobject is called without a zaak UUID") {
            val result = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobject(uuid, null)

            Then("the REST representation is returned without zaak context") {
                result shouldBe restEnkelvoudigInformatieobject
            }
        }
    }

    Given("An enkelvoudig informatieobject at current version 1234 and a requested older version 1") {
        val uuid = UUID.randomUUID()
        val currentVersionEio = createEnkelvoudigInformatieObject(uuid = uuid)
        val olderVersionEio = createEnkelvoudigInformatieObject(uuid = uuid)
        val restOlderEio = createRestEnkelvoudigInformatieobject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns currentVersionEio
        every { drcClientService.readEnkelvoudigInformatieobjectVersie(uuid, 1) } returns olderVersionEio
        every { restInformatieobjectConverter.convertToREST(olderVersionEio) } returns restOlderEio

        When("readEnkelvoudigInformatieobjectVersion is called with an older version") {
            val result = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobjectVersion(uuid, 1)

            Then("the older version is fetched and returned") {
                result shouldBe restOlderEio
                verify(exactly = 1) { drcClientService.readEnkelvoudigInformatieobjectVersie(uuid, 1) }
            }
        }
    }

    Given("An enkelvoudig informatieobject at current version 1234 and a requested version 1234") {
        val uuid = UUID.randomUUID()
        val currentVersionEio = createEnkelvoudigInformatieObject(uuid = uuid)
        val restEio = createRestEnkelvoudigInformatieobject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns currentVersionEio
        every { restInformatieobjectConverter.convertToREST(currentVersionEio) } returns restEio

        When("readEnkelvoudigInformatieobjectVersion is called with the current version") {
            val result = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobjectVersion(uuid, 1234)

            Then("the current version is returned directly without fetching an older version") {
                result shouldBe restEio
                verify(exactly = 0) { drcClientService.readEnkelvoudigInformatieobjectVersie(any(), any()) }
            }
        }
    }

    Given("A zaak with a DEFINITIEF informatieobject eligible for verzending") {
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val eioUuid = UUID.randomUUID()
        val eioUri = URI("https://example.com/$eioUuid")
        val eio = createEnkelvoudigInformatieObject(
            uuid = eioUuid,
            status = StatusEnum.DEFINITIEF,
            vertrouwelijkheidaanduiding = nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum.OPENBAAR,
            ontvangstdatum = null,
            formaat = "application/pdf"
        )
        val zaakInformatieobjectList = listOf(
            createZaakInformatieobjectForCreatesAndUpdates(informatieobjectUUID = eioUuid)
        )
        val restEio = createRestEnkelvoudigInformatieobject()
        val loggedInUser = createLoggedInUser()

        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak, loggedInUser) } returns createZaakRechten()
        every { zrcClientService.listZaakinformatieobjecten(zaak) } returns zaakInformatieobjectList
        every { drcClientService.readEnkelvoudigInformatieobject(eioUri) } returns eio
        every { restInformatieobjectConverter.convertToREST(eio, zaak) } returns restEio
        every { loggedInUserInstance.get() } returns loggedInUser

        When("listEnkelvoudigInformatieobjectenVoorVerzenden is called") {
            val result = enkelvoudigInformatieObjectRestService.listEnkelvoudigInformatieobjectenVoorVerzenden(zaakUuid)

            Then("the eligible informatieobjecten are returned") {
                result shouldBe listOf(restEio)
            }
        }
    }

    Given("A zaaktype with informatieobjecttypen") {
        val zaakTypeUuid = UUID.randomUUID()
        val informatieobjecttypeUri = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
        val zaakType = createZaakType(informatieObjectTypen = listOf(informatieobjecttypeUri))
        val restInformatieobjecttype = createRestInformatieobjecttype()

        every { ztcClientService.readZaaktype(zaakTypeUuid) } returns zaakType
        every {
            restInformatieobjecttypeConverter.convertFromUris(listOf(informatieobjecttypeUri))
        } returns listOf(restInformatieobjecttype)

        When("listInformatieobjecttypes is called") {
            val result = enkelvoudigInformatieObjectRestService.listInformatieobjecttypes(zaakTypeUuid)

            Then("the informatieobjecttypen are returned") {
                result shouldBe listOf(restInformatieobjecttype)
            }
        }
    }

    Given("A zaak informatieobject UUID linking to an enkelvoudig informatieobject") {
        val zaakInformatieobjectUUID = UUID.randomUUID()
        val eioUuid = UUID.randomUUID()
        val eioUri = URI("https://example.com/$eioUuid")
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(informatieobjectUUID = eioUuid)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val restEio = createRestEnkelvoudigInformatieobject()

        every { zrcClientService.readZaakinformatieobject(zaakInformatieobjectUUID) } returns zaakInformatieobject
        every { drcClientService.readEnkelvoudigInformatieobject(eioUri) } returns enkelvoudigInformatieObject
        every { restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject) } returns restEio

        When("readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID is called") {
            val result = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(
                zaakInformatieobjectUUID
            )

            Then("the enkelvoudig informatieobject is returned") {
                result shouldBe restEio
            }
        }
    }

    Given("An enkelvoudig informatieobject linked to a zaak, and the user has permission to delete it") {
        val uuid = UUID.randomUUID()
        val zaakUuid = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUuid)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val reden = "fakeReden"

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak) } returns createDocumentRechten()
        every {
            zgwApiService.removeEnkelvoudigInformatieObjectFromZaak(enkelvoudigInformatieObject, zaakUuid, reden)
        } just Runs

        When("deleteEnkelvoudigInformatieObject is called with a zaak UUID") {
            enkelvoudigInformatieObjectRestService.deleteEnkelvoudigInformatieObject(
                uuid,
                RestDocumentVerwijderenGegevens(zaakUuid = zaakUuid, reden = reden)
            )

            Then("the informatieobject is removed from the zaak via the API") {
                verify(exactly = 1) {
                    zgwApiService.removeEnkelvoudigInformatieObjectFromZaak(
                        enkelvoudigInformatieObject,
                        zaakUuid,
                        reden
                    )
                }
            }
        }
    }

    Given("An ontkoppeld document and the user has permission to delete it") {
        val uuid = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject, null) } returns createDocumentRechten()
        every { detachedDocumentService.delete(uuid) } just Runs

        When("deleteEnkelvoudigInformatieObject is called without a zaak UUID") {
            enkelvoudigInformatieObjectRestService.deleteEnkelvoudigInformatieObject(
                uuid,
                RestDocumentVerwijderenGegevens(zaakUuid = null, reden = null)
            )

            Then("the ontkoppeld document is deleted") {
                verify(exactly = 1) { detachedDocumentService.delete(uuid) }
            }
        }
    }

    Given("An enkelvoudig informatieobject that the user can download, with a specific version") {
        val uuid = UUID.randomUUID()
        val version = 2
        val byteArrayInputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject).downloaden } returns true
        every { drcClientService.downloadEnkelvoudigInformatieobjectVersie(uuid, version) } returns byteArrayInputStream

        When("readFileWithVersion is called") {
            val response = enkelvoudigInformatieObjectRestService.readFileWithVersion(uuid, version)

            Then("the specific version is downloaded and returned") {
                response.status shouldBe 200
                response.headers["Content-Disposition"]!!.first() shouldBe
                    """attachment; filename="${enkelvoudigInformatieObject.bestandsnaam}""""
                response.entity shouldBe byteArrayInputStream
            }
        }
    }

    Given("An enkelvoudig informatieobject that the user can preview with a specific version") {
        val uuid = UUID.randomUUID()
        val version = 3
        val byteArrayInputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
        every { drcClientService.downloadEnkelvoudigInformatieobjectVersie(uuid, version) } returns byteArrayInputStream

        When("preview is called with a version") {
            val response = enkelvoudigInformatieObjectRestService.preview(uuid, version)

            Then("the specific version is returned inline") {
                response.status shouldBe 200
                response.headers["Content-Disposition"]!!.first() shouldBe
                    """inline; filename="${enkelvoudigInformatieObject.bestandsnaam}""""
                response.headers["Content-Type"]!!.first() shouldBe enkelvoudigInformatieObject.formaat
                verify(exactly = 1) { drcClientService.downloadEnkelvoudigInformatieobjectVersie(uuid, version) }
            }
        }
    }

    Given("An enkelvoudig informatieobject that the user can preview without a version") {
        val uuid = UUID.randomUUID()
        val byteArrayInputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns byteArrayInputStream

        When("preview is called without a version") {
            val response = enkelvoudigInformatieObjectRestService.preview(uuid, null)

            Then("the current version is returned inline") {
                response.status shouldBe 200
                verify(exactly = 0) { drcClientService.downloadEnkelvoudigInformatieobjectVersie(any(), any()) }
            }
        }
    }

    Given("Multiple enkelvoudig informatieobjecten that the user can download as a zip") {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val eio1 = createEnkelvoudigInformatieObject(uuid = uuid1)
        val eio2 = createEnkelvoudigInformatieObject(uuid = uuid2)
        val streamingOutput = mockk<StreamingOutput>()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid1) } returns eio1
        every { drcClientService.readEnkelvoudigInformatieobject(uuid2) } returns eio2
        every { policyService.readDocumentRechten(eio1) } returns createDocumentRechten()
        every { policyService.readDocumentRechten(eio2) } returns createDocumentRechten()
        every { enkelvoudigInformatieObjectDownloadService.getZipStreamOutput(listOf(eio1, eio2)) } returns streamingOutput

        When("readFilesAsZip is called") {
            val response = enkelvoudigInformatieObjectRestService.readFilesAsZip(
                listOf(uuid1.toString(), uuid2.toString())
            )

            Then("a zip response is returned") {
                response.status shouldBe 200
                response.entity shouldBe streamingOutput
            }
        }
    }

    Given("An unlocked enkelvoudig informatieobject and the user has permission to lock it") {
        val uuid = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(locked = false)
        val loggedInUser = createLoggedInUser()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every {
            policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
        } returns createDocumentRechten(vergrendelen = true)
        every { loggedInUserInstance.get() } returns loggedInUser
        every { enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUser.id) } returns mockk()
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        When("lockDocument is called") {
            val response = enkelvoudigInformatieObjectRestService.lockDocument(uuid, zaakUUID)

            Then("the document is locked and a 200 response is returned") {
                response.status shouldBe 200
                verify(exactly = 1) { enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUser.id) }
            }
        }
    }

    Given("A locked enkelvoudig informatieobject and the user has permission to unlock it") {
        val uuid = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(locked = true)

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every {
            policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
        } returns createDocumentRechten(ontgrendelen = true)
        every { enkelvoudigInformatieObjectLockService.deleteLock(uuid) } returns Unit
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        When("unlockDocument is called") {
            val response = enkelvoudigInformatieObjectRestService.unlockDocument(uuid, zaakUUID)

            Then("the document is unlocked and a 200 response is returned") {
                response.status shouldBe 200
                verify(exactly = 1) { enkelvoudigInformatieObjectLockService.deleteLock(uuid) }
            }
        }
    }

    Given("An enkelvoudig informatieobject that the user can view its history") {
        val uuid = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val historyLines = listOf<HistoryLine>()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
        every { drcClientService.listAuditTrail(uuid) } returns emptyList()
        every { zaakHistoryLineConverter.convert(any()) } returns historyLines

        When("listInformatieobjectHistory is called") {
            val result = enkelvoudigInformatieObjectRestService.listInformatieobjectHistory(uuid)

            Then("the audit trail is converted and returned") {
                result shouldBe historyLines
                verify(exactly = 1) { drcClientService.listAuditTrail(uuid) }
            }
        }
    }

    Given("An enkelvoudig informatieobject linked to a zaak") {
        val informatieobjectUuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri)
        val zaak = createZaak(identificatie = "ZAAK-2024-999")

        every { drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(zaakUri) } returns zaak

        When("listZaakIdentificatiesForInformatieobject is called") {
            val result = enkelvoudigInformatieObjectRestService.listZaakIdentificatiesForInformatieobject(
                informatieobjectUuid
            )

            Then("the zaak identificaties are returned") {
                result shouldBe listOf("ZAAK-2024-999")
            }
        }
    }

    Given("A document in ontkoppelde-documenten and the user has permission to move it") {
        val documentUUID = UUID.randomUUID()
        val nieuweZaakID = "ZAAK-TARGET-001"
        val informatieobject = createEnkelvoudigInformatieObject()
        val targetZaak = createZaak(identificatie = nieuweZaakID)
        val ontkoppeldDoc = mockk<DetachedDocument>()
        val loggedInUser = createLoggedInUser()

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns informatieobject
        every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
        every {
            policyService.readDocumentRechten(informatieobject, targetZaak)
        } returns createDocumentRechten(verplaatsen = true)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(targetZaak, loggedInUser)
        } returns createZaakRechten(wijzigen = true)
        every { ontkoppeldDoc.id } returns 42L
        every { detachedDocumentService.read(documentUUID) } returns ontkoppeldDoc
        val expectedToelichting = "Verplaatst: ${RestDocumentVerplaatsGegevens.ONTKOPPELDE_DOCUMENTEN} -> $nieuweZaakID"
        every { zrcClientService.koppelInformatieobject(informatieobject, targetZaak, expectedToelichting) } just Runs
        every { detachedDocumentService.delete(42L) } just Runs

        When("verplaatsEnkelvoudigInformatieobject is called with bron ontkoppelde-documenten") {
            enkelvoudigInformatieObjectRestService.verplaatsEnkelvoudigInformatieobject(
                RestDocumentVerplaatsGegevens(
                    documentUUID = documentUUID,
                    bron = RestDocumentVerplaatsGegevens.ONTKOPPELDE_DOCUMENTEN,
                    nieuweZaakID = nieuweZaakID
                )
            )

            Then("the document is linked to the target zaak and removed from ontkoppelde-documenten") {
                verify(exactly = 1) {
                    zrcClientService.koppelInformatieobject(informatieobject, targetZaak, expectedToelichting)
                }
                verify(exactly = 1) { detachedDocumentService.delete(42L) }
            }
        }
    }

    Given("A document in inbox-documenten and the user has permission to move it") {
        val documentUUID = UUID.randomUUID()
        val nieuweZaakID = "ZAAK-TARGET-002"
        val informatieobject = createEnkelvoudigInformatieObject()
        val targetZaak = createZaak(identificatie = nieuweZaakID)
        val inboxDoc = mockk<InboxDocument>()
        val loggedInUser = createLoggedInUser()

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns informatieobject
        every { zrcClientService.readZaakByID(nieuweZaakID) } returns targetZaak
        every {
            policyService.readDocumentRechten(informatieobject, targetZaak)
        } returns createDocumentRechten(verplaatsen = true)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(targetZaak, loggedInUser)
        } returns createZaakRechten(wijzigen = true)
        every { inboxDoc.id } returns 99L
        every { inboxDocumentService.read(documentUUID) } returns inboxDoc
        val expectedToelichting = "Verplaatst: ${RestDocumentVerplaatsGegevens.INBOX_DOCUMENTEN} -> $nieuweZaakID"
        every { zrcClientService.koppelInformatieobject(informatieobject, targetZaak, expectedToelichting) } just Runs
        every { inboxDocumentService.delete(99L) } just Runs

        When("verplaatsEnkelvoudigInformatieobject is called with bron inbox-documenten") {
            enkelvoudigInformatieObjectRestService.verplaatsEnkelvoudigInformatieobject(
                RestDocumentVerplaatsGegevens(
                    documentUUID = documentUUID,
                    bron = RestDocumentVerplaatsGegevens.INBOX_DOCUMENTEN,
                    nieuweZaakID = nieuweZaakID
                )
            )

            Then("the document is linked to the target zaak and removed from inbox-documenten") {
                verify(exactly = 1) {
                    zrcClientService.koppelInformatieobject(informatieobject, targetZaak, expectedToelichting)
                }
                verify(exactly = 1) { inboxDocumentService.delete(99L) }
            }
        }
    }

    Given("A document linked to a source zaak and the user has permission to move it to another zaak") {
        val documentUUID = UUID.randomUUID()
        val sourceZaakID = "fakeSourceZaakID"
        val targetZaakID = "fakeTargetZaakID"
        val informatieobject = createEnkelvoudigInformatieObject()
        val sourceZaak = createZaak(identificatie = sourceZaakID)
        val targetZaak = createZaak(identificatie = targetZaakID)
        val loggedInUser = createLoggedInUser()

        every { drcClientService.readEnkelvoudigInformatieobject(documentUUID) } returns informatieobject
        every { zrcClientService.readZaakByID(targetZaakID) } returns targetZaak
        every {
            policyService.readDocumentRechten(informatieobject, targetZaak)
        } returns createDocumentRechten(verplaatsen = true)
        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readZaakRechten(targetZaak, loggedInUser)
        } returns createZaakRechten(wijzigen = true)
        every { zrcClientService.readZaakByID(sourceZaakID) } returns sourceZaak
        every { zrcClientService.verplaatsInformatieobject(informatieobject, sourceZaak, targetZaak) } just Runs

        When("verplaatsEnkelvoudigInformatieobject is called with bron being a zaak ID") {
            enkelvoudigInformatieObjectRestService.verplaatsEnkelvoudigInformatieobject(
                RestDocumentVerplaatsGegevens(
                    documentUUID = documentUUID,
                    bron = sourceZaakID,
                    nieuweZaakID = targetZaakID
                )
            )

            Then("the document is moved from the source zaak to the target zaak") {
                verify(exactly = 1) {
                    zrcClientService.verplaatsInformatieobject(informatieobject, sourceZaak, targetZaak)
                }
            }
        }
    }

    Given("An enkelvoudig informatieobject linked to a zaak where the user has read access") {
        val uuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri)
        val startDate = LocalDate.of(2024, 1, 1)
        val plannedEndDate = LocalDate.of(2024, 12, 31)
        val zaak = createZaak(
            zaaktypeUri = zaakTypeUri,
            startDate = startDate,
            einddatumGepland = plannedEndDate,
            identificatie = "faakZaakID"
        )
        val zaakType = createZaakType()
        val loggedInUser = createLoggedInUser()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(zaakUri) } returns zaak
        every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechten(lezen = true)

        When("listZaakInformatieobjecten is called") {
            val restZaakInformatieobjects = enkelvoudigInformatieObjectRestService.listZaakInformatieobjecten(uuid)

            Then("the result contains enriched zaak data because the user can read the zaak") {
                restZaakInformatieobjects shouldHaveSize 1
                with(restZaakInformatieobjects.first()) {
                    zaakIdentificatie shouldBe "faakZaakID"
                    zaakStartDatum shouldBe startDate
                    zaakEinddatumGepland shouldBe plannedEndDate
                    zaaktypeOmschrijving shouldBe zaakType.getOmschrijving()
                    zaakRechten.lezen shouldBe true
                    zaakStatus shouldBe null // createZaak() has no status by default
                }
            }
        }
    }

    Given("An enkelvoudig informatieobject linked to a zaak where the user does not have read access") {
        val uuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri)
        val zaak = createZaak(
            zaaktypeUri = zaakTypeUri,
            startDate = LocalDate.of(2024, 6, 1),
            identificatie = "ZAAK-2024-NOACCESS"
        )
        val zaakType = createZaakType()
        val loggedInUser = createLoggedInUser()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { policyService.readDocumentRechten(enkelvoudigInformatieObject) } returns createDocumentRechten()
        every { zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject) } returns listOf(zaakInformatieobject)
        every { zrcClientService.readZaak(zaakUri) } returns zaak
        every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType
        every { loggedInUserInstance.get() } returns loggedInUser
        every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny()

        When("listZaakInformatieobjecten is called") {
            val restZaakInformatieobjects = enkelvoudigInformatieObjectRestService.listZaakInformatieobjecten(uuid)

            Then("sensitive zaak fields are null because the user cannot read the zaak") {
                restZaakInformatieobjects shouldHaveSize 1
                with(restZaakInformatieobjects.first()) {
                    zaakIdentificatie shouldBe "ZAAK-2024-NOACCESS"
                    zaakStartDatum shouldBe null
                    zaakEinddatumGepland shouldBe null
                    zaaktypeOmschrijving shouldBe null
                    zaakRechten.lezen shouldBe false
                    zaakStatus shouldBe null
                }
            }
        }
    }

    Given("An unsigned enkelvoudig informatieobject and the user has permission to sign it") {
        val uuid = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaak = createZaak(uuid = zaakUUID)
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every {
            policyService.readDocumentRechten(enkelvoudigInformatieObject, zaak)
        } returns createDocumentRechten(ondertekenen = true)
        every { enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid) } just Runs
        every { eventingService.send(any<ScreenEvent>()) } just Runs

        When("ondertekenInformatieObject is called") {
            val response = enkelvoudigInformatieObjectRestService.ondertekenInformatieObject(uuid, zaakUUID)

            Then("the document is signed and a 200 response is returned") {
                response.status shouldBe 200
                verify(exactly = 1) {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid)
                }
            }
        }
    }

    Given("A zaak with one currently valid and one expired informatieobjecttype") {
        val zaakUUID = UUID.randomUUID()
        val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
        val validTypeUri = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
        val invalidTypeUri = URI("https://example.com/informatieobjecttype/${UUID.randomUUID()}")
        val zaak = createZaak(zaaktypeUri = zaakTypeUri)
        val zaakType = createZaakType(informatieObjectTypen = listOf(validTypeUri, invalidTypeUri))
        val validType = createInformatieObjectType(uri = validTypeUri, omschrijving = "validType")
        val invalidType = createInformatieObjectType(uri = invalidTypeUri, omschrijving = "invalidType")
            .apply { this.eindeGeldigheid = LocalDate.now().minusDays(1) }

        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(validTypeUri) } returns validType
        every { ztcClientService.readInformatieobjecttype(invalidTypeUri) } returns invalidType

        When("listInformatieobjecttypesForZaak is called") {
            val restInformatieobjecttypes = enkelvoudigInformatieObjectRestService.listInformatieobjecttypesForZaak(
                zaakUUID
            )

            Then("only the currently valid informatieobjecttype is returned") {
                restInformatieobjecttypes shouldHaveSize 1
                restInformatieobjecttypes.first().omschrijving shouldBe "validType"
            }
        }
    }

    Given("A zaak with no informatieobjecttypen") {
        val zaakUUID = UUID.randomUUID()
        val zaakTypeUri = URI("https://example.com/zaaktype/${UUID.randomUUID()}")
        val zaak = createZaak(zaaktypeUri = zaakTypeUri)
        val zaakType = createZaakType(informatieObjectTypen = emptyList())

        every { zrcClientService.readZaak(zaakUUID) } returns zaak
        every { ztcClientService.readZaaktype(zaakTypeUri) } returns zaakType

        When("listInformatieobjecttypesForZaak is called") {
            val restInformatieobjecttypes = enkelvoudigInformatieObjectRestService.listInformatieobjecttypesForZaak(
                zaakUUID
            )

            Then("an empty list is returned") {
                restInformatieobjecttypes shouldBe emptyList()
            }
        }
    }

    Given("An enkelvoudig informatieobject returned by readEnkelvoudigInformatieobject with various indicator flags") {
        data class TestCase(
            val gelockedDoor: RestUser? = null,
            val ondertekening: RestOndertekening? = null,
            val indicatieGebruiksrecht: Boolean = false,
            val isBesluitDocument: Boolean = false,
            val verzenddatum: LocalDate? = null,
            val expectedIndicaties: Set<DocumentIndicatie>
        )

        val uuid = UUID.randomUUID()
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject()

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject

        withData(
            nameFn = { "expected indicaties: ${it.expectedIndicaties}" },
            listOf(
                TestCase(
                    expectedIndicaties = emptySet()
                ),
                TestCase(
                    gelockedDoor = RestUser(id = "fakeId", naam = "fakeName"),
                    expectedIndicaties = setOf(DocumentIndicatie.VERGRENDELD)
                ),
                TestCase(
                    ondertekening = RestOndertekening(soort = "fakeSoort", datum = LocalDate.of(2026, 1, 1)),
                    expectedIndicaties = setOf(DocumentIndicatie.ONDERTEKEND)
                ),
                TestCase(
                    indicatieGebruiksrecht = true,
                    expectedIndicaties = setOf(DocumentIndicatie.GEBRUIKSRECHT)
                ),
                TestCase(
                    isBesluitDocument = true,
                    expectedIndicaties = setOf(DocumentIndicatie.BESLUIT)
                ),
                TestCase(
                    verzenddatum = LocalDate.of(2026, 1, 1),
                    expectedIndicaties = setOf(DocumentIndicatie.VERZONDEN)
                ),
                TestCase(
                    gelockedDoor = RestUser(id = "fakeId", naam = "fakeName"),
                    ondertekening = RestOndertekening(soort = "fakeSoort", datum = LocalDate.of(2026, 1, 1)),
                    indicatieGebruiksrecht = true,
                    isBesluitDocument = true,
                    verzenddatum = LocalDate.of(2026, 1, 1),
                    expectedIndicaties = setOf(
                        DocumentIndicatie.VERGRENDELD,
                        DocumentIndicatie.ONDERTEKEND,
                        DocumentIndicatie.GEBRUIKSRECHT,
                        DocumentIndicatie.BESLUIT,
                        DocumentIndicatie.VERZONDEN
                    )
                )
            )
        ) { testCase ->
            // Each test case re-stubs the converter with a real RestEnkelvoudigInformatieobject
            // built from the fixture, so getIndicaties() executes against real field values.
            val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject(
                gelockedDoor = testCase.gelockedDoor,
                ondertekening = testCase.ondertekening,
                indicatieGebruiksrecht = testCase.indicatieGebruiksrecht,
                isBesluitDocument = testCase.isBesluitDocument,
                verzenddatum = testCase.verzenddatum
            )
            every {
                restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, null)
            } returns restEnkelvoudigInformatieobject

            When("readEnkelvoudigInformatieobject is called without a zaak UUID") {
                val enkelvoudigInformatieobject = enkelvoudigInformatieObjectRestService.readEnkelvoudigInformatieobject(
                    uuid,
                    null
                )

                Then("getIndicaties() reflects the document's indicator flags") {
                    enkelvoudigInformatieobject.getIndicaties().toSet() shouldBe testCase.expectedIndicaties
                }
            }
        }
    }
})
