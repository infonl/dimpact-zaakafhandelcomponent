/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
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
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectDownloadService
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjecttypeConverter
import net.atos.zac.app.informatieobjecten.converter.RestZaakInformatieobjectConverter
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.webdav.WebdavHelper
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createBesluitType
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.exception.RestExceptionMapper
import nl.info.zac.app.informatieobjecten.exception.EnkelvoudigInformatieObjectConversionException
import nl.info.zac.app.informatieobjecten.model.createRESTFileUpload
import nl.info.zac.app.informatieobjecten.model.createRESTInformatieobjectZoekParameters
import nl.info.zac.app.informatieobjecten.model.createRestDocumentVerzendGegevens
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieObjectVersieGegevens
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import nl.info.zac.app.zaak.converter.RestGerelateerdeZaakConverter
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createDocumentRechten
import nl.info.zac.policy.output.createDocumentRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.util.UUID

class EnkelvoudigInformatieObjectRestServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectDownloadService = mockk<EnkelvoudigInformatieObjectDownloadService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val enkelvoudigInformatieObjectConvertService = mockk<EnkelvoudigInformatieObjectConvertService>()
    val eventingService = mockk<EventingService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val ontkoppeldeDocumentenService = mockk<OntkoppeldeDocumentenService>()
    val policyService = mockk<PolicyService>()
    val restGerelateerdeZaakConverter = mockk<RestGerelateerdeZaakConverter>()
    val zaakHistoryLineConverter = mockk<ZaakHistoryLineConverter>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val restInformatieobjecttypeConverter = mockk<RestInformatieobjecttypeConverter>()
    val webdavHelper = mockk<WebdavHelper>()
    val zaakInformatieobjectConverter = mockk<RestZaakInformatieobjectConverter>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val enkelvoudigInformatieObjectRestService = EnkelvoudigInformatieObjectRestService(
        drcClientService = drcClientService,
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService,
        zgwApiService = zgwApiService,
        ontkoppeldeDocumentenService = ontkoppeldeDocumentenService,
        inboxDocumentenService = inboxDocumentenService,
        enkelvoudigInformatieObjectLockService = enkelvoudigInformatieObjectLockService,
        eventingService = eventingService,
        zaakInformatieobjectConverter = zaakInformatieobjectConverter,
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
        val restFileUpload = createRESTFileUpload()
        val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()

        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        every {
            restInformatieobjectConverter.convertZaakObject(restEnkelvoudigInformatieobject)
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

        When(
            "the enkelvoudig informatieobject update is done by a role that is allowed to change the zaak"
        ) {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
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
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
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
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
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
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

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

        every { zrcClientService.readZaak(closedZaak.uuid) } returns closedZaak
        every {
            restInformatieobjectConverter.convertZaakObject(restEnkelvoudigInformatieobject)
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

        When(
            "the enkelvoudig informatieobject is updated by a role that is allowed to change the zaak"
        ) {
            every { policyService.readZaakRechten(closedZaak) } returns createZaakRechtenAllDeny(
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
            drcClientService.readEnkelvoudigInformatieobject(restEnkelvoudigInformatieObjectVersieGegevens.uuid)
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
                        restEnkelvoudigInformatieObjectVersieGegevens.uuid
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
        val restInformatieobjectZoekParameters = createRESTInformatieobjectZoekParameters(
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
        val restInformatieobjectZoekParameters = createRESTInformatieobjectZoekParameters(
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
            url = URI("http://example.com/$besluittypeUuid"),
            informatieobjecttypen = listOf(
                URI("http://example.com/$informatieobjectUUID"),
            )
        )

        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
        every { zrcClientService.listZaakinformatieobjecten(zaak) } returns zaakInformatieobjecten
        every {
            restInformatieobjectConverter.convertToREST(zaakInformatieobjecten[0])
        } returns restEnkelvoudigInformatieobjecten[0]
        every { zrcClientService.readZaak(deelzaak.url) } returns deelzaak
        every { zrcClientService.listZaakinformatieobjecten(deelzaak) } returns emptyList()
        every {
            ztcClientService.readBesluittype(restInformatieobjectZoekParameters.besluittypeUUID)
        } returns besluitType

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
        When("the enkelvoudig informatieobject is trying to be converted with status definitief") {
            every {
                enkelvoudigInformatieObjectConvertService.convertEnkelvoudigInformatieObject(
                    any(), any()
                )
            } just Runs

            enkelvoudiginformatieobject.status = StatusEnum.DEFINITIEF
            val resp = enkelvoudigInformatieObjectRestService.convertInformatieObjectToPDF(
                informatieobjectUUID,
                zaak.uuid
            )
            Then("the response should be ok") {
                resp.status shouldBe 200
            }
        }
        When("the enkelvoudig informatieobject is trying to be converted with status in bewerking") {
            every {
                enkelvoudigInformatieObjectConvertService.convertEnkelvoudigInformatieObject(
                    any(),
                    any()
                )
            } throws EnkelvoudigInformatieObjectConversionException()

            val resp = try {
                enkelvoudigInformatieObjectRestService.convertInformatieObjectToPDF(informatieobjectUUID, zaak.uuid)
            } catch (e: Exception) {
                RestExceptionMapper().toResponse(e)
            }

            Then("the response should be an error message") {
                resp.status shouldBe 400
                val entity = resp.entity as String
                entity shouldContain """"message":"msg.error.convert.not.possible""""
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
        enkelvoudigInformatieObjectUuids.forEachIndexed { index, informatieObjectUuid ->
            every {
                drcClientService.readEnkelvoudigInformatieobject(informatieObjectUuid)
            } returns enkelvoudigeInformatieobjecten[index]
        }
        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak).wijzigen } returns true
        every {
            enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(any(), any(), any())
        } just Runs

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
        every {
            drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieObjectUuids[0])
        } returns enkelvoudigInformatieObject
        every { zrcClientService.readZaak(zaakUuid) } returns zaak
        every { policyService.readZaakRechten(zaak).wijzigen } returns false

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
})
