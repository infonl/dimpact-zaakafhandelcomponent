/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.documenten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.model.audit.documenten.EnkelvoudigInformatieobjectWijziging
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import java.net.URI

class AuditEnkelvoudigInformatieobjectConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val converter = AuditEnkelvoudigInformatieobjectConverter(ztcClientService)

    afterEach { checkUnnecessaryStub() }

    fun createFullEnkelvoudigInformatieObject(
        title: String = "fakeTitel",
        informatieObjectType: URI = URI("https://example.com/informatieobjecttype/fakeDefault"),
        beginRegistratie: java.time.OffsetDateTime = java.time.OffsetDateTime.now()
    ) = createEnkelvoudigInformatieObject(
        title = title,
        informatieObjectType = informatieObjectType,
        beginRegistratie = beginRegistratie
    ).also {
        it.identificatie = "fakeIdentificatie1"
        it.bestandsnaam = "fakeBestandsnaam.pdf"
        it.taal = "nld"
        it.auteur = "fakeAuteur"
        it.bronorganisatie = "fakeBronorganisatie"
    }

    fun createWijziging(
        oud: EnkelvoudigInformatieObject? = null,
        nieuw: EnkelvoudigInformatieObject? = null
    ) = EnkelvoudigInformatieobjectWijziging().apply {
        this.oud = oud
        this.nieuw = nieuw
    }

    Context("Converting a wijziging with two identical objects") {
        Given("Old and new EnkelvoudigInformatieObject with identical field values") {
            val fakeObject = createFullEnkelvoudigInformatieObject()
            val wijziging = createWijziging(oud = fakeObject, nieuw = fakeObject)

            When("convert is called") {
                val result = converter.convert(wijziging)

                Then("an empty list is returned") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    Context("Converting a wijziging where titel changed") {
        Given("Old object with titel 'fakeOudTitel' and new object with titel 'fakeNieuwTitel'") {
            val sharedRegistratie = java.time.OffsetDateTime.now()
            val oudObject =
                createFullEnkelvoudigInformatieObject(title = "fakeOudTitel", beginRegistratie = sharedRegistratie)
            val nieuwObject =
                createFullEnkelvoudigInformatieObject(title = "fakeNieuwTitel", beginRegistratie = sharedRegistratie)
            val wijziging = createWijziging(oud = oudObject, nieuw = nieuwObject)

            When("convert is called") {
                val result = converter.convert(wijziging)

                Then("one HistoryLine with label 'titel' is returned") {
                    result.any { it.attribuutLabel == "titel" } shouldBe true
                }
            }
        }
    }

    Context("Converting a wijziging where informatieobjecttype URI changed") {
        Given("Old and new objects with different informatieobjecttype URIs") {
            val fakeOudUri = URI("https://example.com/informatieobjecttype/fakeOud")
            val fakeNieuwUri = URI("https://example.com/informatieobjecttype/fakeNieuw")
            val fakeOudType = mockk<InformatieObjectType> { every { omschrijving } returns "fakeOudOmschrijving" }
            val fakeNieuwType = mockk<InformatieObjectType> { every { omschrijving } returns "fakeNieuwOmschrijving" }
            val sharedRegistratie = java.time.OffsetDateTime.now()
            val oudObject =
                createFullEnkelvoudigInformatieObject(
                    informatieObjectType = fakeOudUri,
                    beginRegistratie = sharedRegistratie
                )
            val nieuwObject =
                createFullEnkelvoudigInformatieObject(
                    informatieObjectType = fakeNieuwUri,
                    beginRegistratie = sharedRegistratie
                )
            val wijziging = createWijziging(oud = oudObject, nieuw = nieuwObject)

            every { ztcClientService.readInformatieobjecttype(fakeOudUri) } returns fakeOudType
            every { ztcClientService.readInformatieobjecttype(fakeNieuwUri) } returns fakeNieuwType

            When("convert is called") {
                val result = converter.convert(wijziging)

                Then("a HistoryLine with label 'documentType' is returned") {
                    result.any { it.attribuutLabel == "documentType" } shouldBe true
                }
            }
        }
    }

    Context("Converting a wijziging where oud is null") {
        Given("A wijziging with oud = null and a non-null nieuw") {
            val wijziging = createWijziging(oud = null, nieuw = createFullEnkelvoudigInformatieObject())

            When("convert is called") {
                val result = converter.convert(wijziging)

                Then("a single HistoryLine with label 'informatieobject' is returned") {
                    result.size shouldBe 1
                    result[0].attribuutLabel shouldBe "informatieobject"
                }
            }
        }
    }

    Context("Converting a wijziging where nieuw is null") {
        Given("A wijziging with a non-null oud and nieuw = null") {
            val wijziging = createWijziging(oud = createFullEnkelvoudigInformatieObject(), nieuw = null)

            When("convert is called") {
                val result = converter.convert(wijziging)

                Then("a single HistoryLine with label 'informatieobject' is returned") {
                    result.size shouldBe 1
                    result[0].attribuutLabel shouldBe "informatieobject"
                }
            }
        }
    }
})
