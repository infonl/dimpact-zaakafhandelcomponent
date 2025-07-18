/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.converter

import com.google.common.collect.ImmutableList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldHaveSameDayAs
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.exception.ZgwErrorException
import net.atos.client.zgw.shared.model.ZgwError
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.zac.app.informatieobjecten.model.createRESTFileUpload
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieObjectVersieGegevens
import nl.info.zac.app.informatieobjecten.model.createRestEnkelvoudigInformatieobject
import nl.info.zac.app.task.model.createRestTaskDocumentData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.policy.output.createDocumentRechtenAllDeny
import org.eclipse.jetty.http.HttpStatus
import java.net.URI
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

class RestInformatieobjectConverterTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val drcClientService = mockk<DrcClientService>()
    val enkelvoudigInformatieObjectLockService = mockk<EnkelvoudigInformatieObjectLockService>()
    val identityService = mockk<IdentityService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val loggedInUser = createLoggedInUser()
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()

    val restInformatieobjectConverter = RestInformatieobjectConverter(
        brcClientService,
        configuratieService,
        drcClientService,
        enkelvoudigInformatieObjectLockService,
        identityService,
        loggedInUserInstance,
        policyService,
        zrcClientService,
        ztcClientService
    )

    Given("REST taak document data and REST file upload are provided") {
        val restTaakDocumentData = createRestTaskDocumentData()
        val restFileUpload = createRESTFileUpload()
        val providedInformatieObjectType = createInformatieObjectType()

        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            ztcClientService.readInformatieobjecttype(restTaakDocumentData.documentType.uuid)
        } returns providedInformatieObjectType
        every { configuratieService.readBronOrganisatie() } returns "123443210"

        When("convert is invoked") {
            val enkelvoudigInformatieObjectData = restInformatieobjectConverter.convert(
                restTaakDocumentData,
                restFileUpload
            )
            Then("the provided data is converted correctly") {
                with(enkelvoudigInformatieObjectData) {
                    bronorganisatie shouldBe "123443210"
                    creatiedatum shouldHaveSameDayAs LocalDate.now()
                    titel shouldBe restTaakDocumentData.documentTitel
                    auteur shouldBe loggedInUser.getFullName()
                    // currently hardcoded
                    taal shouldBe "dut"
                    informatieobjecttype shouldBe providedInformatieObjectType.url
                    inhoud shouldBe Base64.getEncoder().encodeToString(restFileUpload.file)
                    formaat shouldBe restFileUpload.type
                    bestandsnaam shouldBe restFileUpload.filename
                    // status should always be DEFINITIEF
                    status shouldBe StatusEnum.DEFINITIEF
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.valueOf(
                        restTaakDocumentData.documentType.vertrouwelijkheidaanduiding
                    )
                }
            }
        }
    }

    Given("REST enkelvoudig informatie object data and REST file upload are provided for a taak") {
        val restFileUpload = createRESTFileUpload()
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject()
        val providedInformatieObjectType = createInformatieObjectType()

        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID)
        } returns providedInformatieObjectType
        every { configuratieService.readBronOrganisatie() } returns "123443210"

        When("convert taak object is invoked") {
            val enkelvoudigInformatieObjectData = restInformatieobjectConverter.convertTaakObject(
                restEnkelvoudigInformatieobject
            )
            Then("the provided data is converted correctly") {
                with(enkelvoudigInformatieObjectData) {
                    bronorganisatie shouldBe "123443210"
                    creatiedatum shouldHaveSameDayAs LocalDate.now()
                    titel shouldBe restEnkelvoudigInformatieobject.titel
                    auteur shouldBe loggedInUser.getFullName()
                    // currently hardcoded
                    taal shouldBe "dut"
                    informatieobjecttype shouldBe providedInformatieObjectType.url
                    inhoud shouldBe Base64.getEncoder().encodeToString(restFileUpload.file)
                    formaat shouldBe restFileUpload.type
                    bestandsnaam shouldBe restFileUpload.filename
                    // status should always be DEFINITIEF
                    status shouldBe StatusEnum.DEFINITIEF
                    // vertrouwelijkheidaanduiding should always be OPENBAAR
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
                }
            }
        }
    }

    Given("REST enkelvoudig informatie object data and REST file upload are provided for a zaak") {
        // when converting a zaak more fields in the RESTEnkelvoudigInformatieobject are used in the
        // conversion compared to when converting a taak
        val restEnkelvoudigInformatieobject = createRestEnkelvoudigInformatieobject(
            vertrouwelijkheidaanduiding = "vertrouwelijk",
            creatieDatum = LocalDate.now(),
            auteur = "fakeAuteur",
            taal = "fakeTaal",
            bestandsNaam = "fakeBestandsNaam"
        )
        val restFileUpload = createRESTFileUpload()
        val providedInformatieObjectType = createInformatieObjectType()

        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID)
        } returns providedInformatieObjectType
        every { configuratieService.readBronOrganisatie() } returns "123443210"

        When("convert zaak object is invoked") {
            val enkelvoudigInformatieObjectData = restInformatieobjectConverter.convertZaakObject(
                restEnkelvoudigInformatieobject
            )
            Then("the provided data is converted correctly") {
                with(enkelvoudigInformatieObjectData) {
                    bronorganisatie shouldBe "123443210"
                    creatiedatum shouldHaveSameDayAs LocalDate.now()
                    titel shouldBe restEnkelvoudigInformatieobject.titel
                    auteur shouldBe restEnkelvoudigInformatieobject.auteur
                    taal shouldBe restEnkelvoudigInformatieobject.taal
                    informatieobjecttype shouldBe providedInformatieObjectType.url
                    inhoud shouldBe Base64.getEncoder().encodeToString(restFileUpload.file)
                    formaat shouldBe restFileUpload.type
                    bestandsnaam shouldBe restEnkelvoudigInformatieobject.bestandsnaam
                    status.name shouldBe restEnkelvoudigInformatieobject.status.name
                    vertrouwelijkheidaanduiding.name shouldBe restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding.uppercase()
                }
            }
        }
    }

    Given("Enkelvoudig informatie object") {
        val expectedUUID = UUID.randomUUID()
        val uri = URI("https://example.com/informatieobjecten/$expectedUUID")
        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject(url = uri).apply {
            informatieobjecttype = uri
            vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.ZEER_GEHEIM
        }
        val documentRechten = createDocumentRechtenAllDeny(lezen = true)

        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            policyService.readDocumentRechten(enkelvoudigInformatieObject, null, null)
        } returns documentRechten
        every {
            brcClientService.isInformatieObjectGekoppeldAanBesluit(enkelvoudigInformatieObject.url)
        } returns true
        every {
            configuratieService.findTaal(any())
        } returns null
        every {
            ztcClientService.readInformatieobjecttype(any<URI>())
        } returns createInformatieObjectType()

        When("converted to REST Enkelvoudig Informatie Object") {
            val restEnkelvoudigInformatieObject = restInformatieobjectConverter.convertToREST(
                enkelvoudigInformatieObject
            )

            Then("the provided data is converted correctly") {
                with(restEnkelvoudigInformatieObject) {
                    uuid shouldBe expectedUUID
                    informatieobjectTypeOmschrijving shouldBe "fakeOmschrijving"
                    informatieobjectTypeUUID shouldBe expectedUUID
                    informatieobjectTypeUUID shouldBe expectedUUID
                    versie shouldBe 1234
                    bestandsomvang shouldBe 1234
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.ZEER_GEHEIM.name
                }
            }
        }
    }

    Given("A uuid that's found in open zaak") {
        val rechten = DocumentRechten(false, false, false, false, false, false, false, false, false, false)
        val uuid = UUID.randomUUID()
        val document = createEnkelvoudigInformatieObject(
            url = URI("http://example.com/$uuid")
        )

        every { loggedInUserInstance.get() } returns loggedInUser
        every {
            drcClientService.readEnkelvoudigInformatieobject(uuid)
        } returns document
        every {
            policyService.readDocumentRechten(document, null, null)
        } returns rechten
        every {
            brcClientService.isInformatieObjectGekoppeldAanBesluit(document.url)
        } returns false
        When("We try to convert a list with that uuid") {
            val result = restInformatieobjectConverter.convertUUIDsToREST(ImmutableList.of(uuid), null)
            Then("An list is returned containing the expected object") {
                with(result) {
                    shouldHaveSize(1)
                    with(get(0).uuid) {
                        shouldBe(uuid)
                    }
                }
            }
        }
    }

    Given("A uuid that's not found in open zaak") {
        val uuid = UUID.randomUUID()
        every {
            drcClientService.readEnkelvoudigInformatieobject(uuid)
        } throws ZgwErrorException(ZgwError(null, null, null, HttpStatus.NOT_FOUND_404, null, null))
        When("We try to convert a list with that uuid") {
            val result = restInformatieobjectConverter.convertUUIDsToREST(ImmutableList.of(uuid), null)
            Then("An empty list is returned") {
                result.shouldBeEmpty()
            }
        }
    }

    Given("A uuid that causes an error response other than 404 in open zaak") {
        val uuid = UUID.randomUUID()
        val expectedException = ZgwErrorException(ZgwError(null, null, null, 500, null, null))
        every {
            drcClientService.readEnkelvoudigInformatieobject(uuid)
        } throws expectedException
        When("We try to convert a list with that uuid") {
            Then("The exception bubbles up") {
                val exception = shouldThrow<ZgwErrorException> {
                    restInformatieobjectConverter.convertUUIDsToREST(ImmutableList.of(uuid), null)
                }
                exception.shouldBe(expectedException)
            }
        }
    }

    Given("A 'REST enkelvoudiginformatieobject versie gegevens' object containing a file, bestandsnaam and formaat") {
        val informatieobjectType = createInformatieObjectType()
        val restEnkelvoudigInformatieobjectVersieGegevens = createRestEnkelvoudigInformatieObjectVersieGegevens(
            vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR.name.lowercase()
        )

        every {
            ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobjectVersieGegevens.informatieobjectTypeUUID)
        } returns informatieobjectType

        When("this object is converted") {
            val enkelvoudigInformatieObjectWithLockRequest =
                restInformatieobjectConverter.convert(restEnkelvoudigInformatieobjectVersieGegevens)

            Then("the obejct is correctly converted to a 'enkelvoudiginformatieobject with lock request'") {
                with(enkelvoudigInformatieObjectWithLockRequest) {
                    bestandsnaam shouldBe restEnkelvoudigInformatieobjectVersieGegevens.bestandsnaam
                    bestandsomvang shouldBe restEnkelvoudigInformatieobjectVersieGegevens.file.size
                    inhoud shouldBe Base64.getEncoder().encodeToString(restEnkelvoudigInformatieobjectVersieGegevens.file)
                    informatieobjecttype shouldBe informatieobjectType.url
                    vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
                }
            }
        }
    }
})
