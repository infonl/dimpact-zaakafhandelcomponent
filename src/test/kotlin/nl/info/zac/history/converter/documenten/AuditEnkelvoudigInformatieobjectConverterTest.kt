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
import nl.info.client.zgw.shared.model.audit.documenten.createEnkelvoudigInformatieobjectWijziging
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import java.net.URI

class AuditEnkelvoudigInformatieobjectConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val converter = AuditEnkelvoudigInformatieobjectConverter(ztcClientService)

    afterEach { checkUnnecessaryStub() }

    context("convert") {
        given("Old and new EnkelvoudigInformatieObject with identical field values") {
            val fakeObject = createEnkelvoudigInformatieObject(
                title = "fakeTitel",
                informatieObjectType = URI("https://example.com/informatieobjecttype/fakeDefault"),
                identificatie = "fakeIdentificatie1",
                bestandsnaam = "fakeBestandsnaam.pdf",
                taal = "nld",
                auteur = "fakeAuteur",
                bronorganisatie = "fakeBronorganisatie"
            )
            val wijziging = createEnkelvoudigInformatieobjectWijziging(oud = fakeObject, nieuw = fakeObject)

            `when`("convert is called") {
                val result = converter.convert(wijziging)

                then("an empty list is returned") {
                    result.shouldBeEmpty()
                }
            }
        }

        given("Old object with titel 'fakeOudTitel' and new object with titel 'fakeNieuwTitel'") {
            val fakeSharedInformatieObjectType = URI("https://example.com/informatieobjecttype/fakeDefault")
            val oudObject = createEnkelvoudigInformatieObject(
                title = "fakeOudTitel",
                informatieObjectType = fakeSharedInformatieObjectType,
                identificatie = "fakeIdentificatie1",
                bestandsnaam = "fakeBestandsnaam.pdf",
                taal = "nld",
                auteur = "fakeAuteur",
                bronorganisatie = "fakeBronorganisatie"
            )
            val nieuwObject = createEnkelvoudigInformatieObject(
                title = "fakeNieuwTitel",
                informatieObjectType = fakeSharedInformatieObjectType,
                identificatie = "fakeIdentificatie1",
                bestandsnaam = "fakeBestandsnaam.pdf",
                taal = "nld",
                auteur = "fakeAuteur",
                bronorganisatie = "fakeBronorganisatie"
            )
            val wijziging = createEnkelvoudigInformatieobjectWijziging(oud = oudObject, nieuw = nieuwObject)

            `when`("convert is called") {
                val result = converter.convert(wijziging)

                then("one HistoryLine with label 'titel' is returned") {
                    result.any { it.attributeLabel == "titel" } shouldBe true
                }
            }
        }

        given("Old and new objects with different informatieobjecttype URIs") {
            val fakeOudUri = URI("https://example.com/informatieobjecttype/fakeOud")
            val fakeNieuwUri = URI("https://example.com/informatieobjecttype/fakeNieuw")
            val fakeOudType = mockk<InformatieObjectType> { every { omschrijving } returns "fakeOudOmschrijving" }
            val fakeNieuwType = mockk<InformatieObjectType> { every { omschrijving } returns "fakeNieuwOmschrijving" }
            val oudObject = createEnkelvoudigInformatieObject(
                title = "fakeTitel",
                informatieObjectType = fakeOudUri,
                identificatie = "fakeIdentificatie1",
                bestandsnaam = "fakeBestandsnaam.pdf",
                taal = "nld",
                auteur = "fakeAuteur",
                bronorganisatie = "fakeBronorganisatie"
            )
            val nieuwObject = createEnkelvoudigInformatieObject(
                title = "fakeTitel",
                informatieObjectType = fakeNieuwUri,
                identificatie = "fakeIdentificatie1",
                bestandsnaam = "fakeBestandsnaam.pdf",
                taal = "nld",
                auteur = "fakeAuteur",
                bronorganisatie = "fakeBronorganisatie"
            )
            val wijziging = createEnkelvoudigInformatieobjectWijziging(oud = oudObject, nieuw = nieuwObject)

            every { ztcClientService.readInformatieobjecttype(fakeOudUri) } returns fakeOudType
            every { ztcClientService.readInformatieobjecttype(fakeNieuwUri) } returns fakeNieuwType

            `when`("convert is called") {
                val result = converter.convert(wijziging)

                then("a HistoryLine with label 'documentType' is returned") {
                    result.any { it.attributeLabel == "documentType" } shouldBe true
                }
            }
        }

        given("A wijziging with oud = null and a non-null nieuw") {
            val wijziging = createEnkelvoudigInformatieobjectWijziging(
                oud = null,
                nieuw = createEnkelvoudigInformatieObject()
            )

            `when`("convert is called") {
                val result = converter.convert(wijziging)

                then("a single HistoryLine with label 'informatieobject' is returned") {
                    result.size shouldBe 1
                    result[0].attributeLabel shouldBe "informatieobject"
                }
            }
        }

        given("A wijziging with a non-null oud and nieuw = null") {
            val wijziging = createEnkelvoudigInformatieobjectWijziging(
                oud = createEnkelvoudigInformatieObject(),
                nieuw = null
            )

            `when`("convert is called") {
                val result = converter.convert(wijziging)

                then("a single HistoryLine with label 'informatieobject' is returned") {
                    result.size shouldBe 1
                    result[0].attributeLabel shouldBe "informatieobject"
                }
            }
        }
    }
})
