/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.officeconverter.OfficeConverterClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.drc.model.createEnkelvoudigInformatieObjectWithLockRequest
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.model.Archiefnominatie
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createBesluitType
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjecttypeConverter
import net.atos.zac.app.informatieobjecten.converter.RestZaakInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.createRESTFileUpload
import net.atos.zac.app.informatieobjecten.model.createRESTInformatieobjectZoekParameters
import net.atos.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieObjectVersieGegevens
import net.atos.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import net.atos.zac.app.informatieobjecten.model.createRestInformatieobjecttype
import net.atos.zac.app.zaak.converter.RestGerelateerdeZaakConverter
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.event.EventingService
import net.atos.zac.history.converter.ZaakHistoryLineConverter
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createDocumentRechten
import net.atos.zac.policy.output.createDocumentRechtenAllDeny
import net.atos.zac.policy.output.createZaakRechten
import net.atos.zac.policy.output.createZaakRechtenAllDeny
import net.atos.zac.util.extractUuid
import net.atos.zac.webdav.WebdavHelper
import java.net.URI
import java.util.UUID

class EnkelvoudigInformatieObjectRestServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectDownloadService = mockk<EnkelvoudigInformatieObjectDownloadService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val enkelvoudigInformatieObjectUpdateService = mockk<EnkelvoudigInformatieObjectUpdateService>()
    val eventingService = mockk<EventingService>()
    val inboxDocumentenService = mockk<InboxDocumentenService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val officeConverterClientService = mockk<OfficeConverterClientService>()
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
        officeConverterClientService = officeConverterClientService
    )

    isolationMode = IsolationMode.InstancePerTest

    Given("an enkelvoudig informatieobject has been uploaded, and the zaak is open") {
        val zaak = createZaak()
        val documentReferentieId = "dummyDocumentReferentieId"
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val responseRestEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val restFileUpload = createRESTFileUpload()
        val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobject()

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
            } throws RuntimeException("dummy exception")

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
            archiefnominatie = Archiefnominatie.VERNIETIGEN
        )
        val documentReferentieId = "dummyDocumentReferentieId"
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val responseRestEnkelvoudigInformatieobject =
            createRestEnkelvoudigInformatieobject()
        val enkelvoudigInformatieObjectData = createEnkelvoudigInformatieObjectCreateLockRequest()
        val zaakInformatieobject = createZaakInformatieobject()

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
            deelzaken = setOf(deelzaak.url)
        )
        val restEnkelvoudigInformatieobjecten = listOf(
            createRestEnkelvoudigInformatieobject(
                informatieobjectTypeUUID = informatieobjectUUID
            )
        )
        val zaakInformatieobjecten = listOf(
            createZaakInformatieobject()
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
        val enkelvoudiginformatieobject = createEnkelvoudigInformatieObject()
        val restEnkelvoudigInformatieObjectVersieGegevens =
            createRestEnkelvoudigInformatieObjectVersieGegevens(uuid = informatieobjectUUID)
        every {
            drcClientService.readEnkelvoudigInformatieobject(informatieobjectUUID)
        } returns enkelvoudiginformatieobject
        every { policyService.readDocumentRechten(enkelvoudiginformatieobject) } returns createDocumentRechten()
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
    }
    Given("A zaak with two informatieobjecttypes") {
        val zaak = createZaak()
        val informatieObjectTypes = listOf(createInformatieObjectType(), createInformatieObjectType())
        val restInformatieobjecttypes = listOf(createRestInformatieobjecttype(), createRestInformatieobjecttype())
        every { zrcClientService.readZaak(zaak.uuid) } returns zaak
        with(ztcClientService) {
            every { readZaaktype(zaak.zaaktype).informatieobjecttypen } returns informatieObjectTypes.map { it.url }
            informatieObjectTypes.forEachIndexed { index, informatieObjectType, ->
                every { readInformatieobjecttype(informatieObjectType.url) } returns informatieObjectTypes[index]
            }
        }
        every { restInformatieobjecttypeConverter.convert(informatieObjectTypes) } returns restInformatieobjecttypes

        When("the informatieobjecttypes for the zaak are requested") {
            val returnedRestInformatieobjecttypes = enkelvoudigInformatieObjectRestService.listInformatieobjecttypesForZaak(
                zaak.uuid
            )

            Then("the information object types are returned") {
                returnedRestInformatieobjecttypes shouldBe restInformatieobjecttypes
            }
        }
    }
})
