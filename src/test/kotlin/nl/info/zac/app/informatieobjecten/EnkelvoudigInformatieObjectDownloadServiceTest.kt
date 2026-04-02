/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.informatieobjecten.exception.EnkelvoudigInformatieObjectDownloadException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipInputStream

class EnkelvoudigInformatieObjectDownloadServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val service = EnkelvoudigInformatieObjectDownloadService(drcClientService, zrcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    fun readZipEntries(output: ByteArrayOutputStream): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    Given("a single inkomend informatieobject (ontvangstdatum set)") {
        val uuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val informatieobject = createEnkelvoudigInformatieObject(
            uuid = uuid,
            ontvangstdatum = LocalDate.now()
        ).apply {
            identificatie = "DOC-001"
            bestandsnaam = "report.pdf"
        }
        val fileContent = "hello world"

        every { zrcClientService.listZaakinformatieobjecten(informatieobject) } returns
            listOf(createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri))
        every { zrcClientService.readZaak(zaakUri) } returns createZaak(identificatie = "ZAAK-2024-001")
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns
            ByteArrayInputStream(fileContent.toByteArray())

        When("getZipStreamOutput is called") {
            val output = ByteArrayOutputStream()
            service.getZipStreamOutput(listOf(informatieobject)).write(output)
            val entries = readZipEntries(output)

            Then("the zip contains the document in the inkomend subfolder with correct content") {
                entries.keys shouldContainExactlyInAnyOrder listOf(
                    "ZAAK-2024-001/inkomend/report-DOC-001.pdf",
                    "samenvatting.txt"
                )
                entries["ZAAK-2024-001/inkomend/report-DOC-001.pdf"] shouldBe fileContent
            }

            And("the samenvatting.txt references the zaak, richting and bestandsnaam") {
                val samenvatting = entries["samenvatting.txt"]!!
                samenvatting shouldContain "ZAAK-2024-001"
                samenvatting shouldContain "inkomend"
                samenvatting shouldContain "report-DOC-001.pdf"
            }
        }
    }

    Given("a single uitgaand informatieobject (verzenddatum set, ontvangstdatum null)") {
        val uuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val informatieobject = createEnkelvoudigInformatieObject(uuid = uuid, ontvangstdatum = null).apply {
            identificatie = "DOC-002"
            bestandsnaam = "letter.docx"
            verzenddatum = LocalDate.now()
        }

        every { zrcClientService.listZaakinformatieobjecten(informatieobject) } returns
            listOf(createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri))
        every { zrcClientService.readZaak(zaakUri) } returns createZaak(identificatie = "ZAAK-2024-002")
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns
            ByteArrayInputStream(ByteArray(0))

        When("getZipStreamOutput is called") {
            val output = ByteArrayOutputStream()
            service.getZipStreamOutput(listOf(informatieobject)).write(output)
            val entries = readZipEntries(output)

            Then("the document entry is placed in the uitgaand subfolder") {
                entries.keys shouldContainExactlyInAnyOrder listOf(
                    "ZAAK-2024-002/uitgaand/letter-DOC-002.docx",
                    "samenvatting.txt"
                )
            }
        }
    }

    Given("a single intern informatieobject (both ontvangstdatum and verzenddatum are null)") {
        val uuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val informatieobject = createEnkelvoudigInformatieObject(uuid = uuid, ontvangstdatum = null).apply {
            identificatie = "DOC-003"
            bestandsnaam = "memo.txt"
        }

        every { zrcClientService.listZaakinformatieobjecten(informatieobject) } returns
            listOf(createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri))
        every { zrcClientService.readZaak(zaakUri) } returns createZaak(identificatie = "ZAAK-2024-003")
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns
            ByteArrayInputStream(ByteArray(0))

        When("getZipStreamOutput is called") {
            val output = ByteArrayOutputStream()
            service.getZipStreamOutput(listOf(informatieobject)).write(output)
            val entries = readZipEntries(output)

            Then("the document entry is placed in the intern subfolder") {
                entries.keys shouldContainExactlyInAnyOrder listOf(
                    "ZAAK-2024-003/intern/memo-DOC-003.txt",
                    "samenvatting.txt"
                )
            }
        }
    }

    Given("two informatieobjecten belonging to different zaken") {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val zaakUri1 = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val zaakUri2 = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val informatieobject1 = createEnkelvoudigInformatieObject(
            uuid = uuid1,
            ontvangstdatum = LocalDate.now()
        ).apply {
            identificatie = "DOC-A"
            bestandsnaam = "doc-a.pdf"
        }
        val informatieobject2 = createEnkelvoudigInformatieObject(uuid = uuid2, ontvangstdatum = null).apply {
            identificatie = "DOC-B"
            bestandsnaam = "doc-b.pdf"
        }

        every { zrcClientService.listZaakinformatieobjecten(informatieobject1) } returns
            listOf(createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri1))
        every { zrcClientService.listZaakinformatieobjecten(informatieobject2) } returns
            listOf(createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri2))
        every { zrcClientService.readZaak(zaakUri1) } returns createZaak(identificatie = "ZAAK-A")
        every { zrcClientService.readZaak(zaakUri2) } returns createZaak(identificatie = "ZAAK-B")
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid1) } returns ByteArrayInputStream(ByteArray(0))
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid2) } returns ByteArrayInputStream(ByteArray(0))

        When("getZipStreamOutput is called") {
            val output = ByteArrayOutputStream()
            service.getZipStreamOutput(listOf(informatieobject1, informatieobject2)).write(output)
            val entries = readZipEntries(output)

            Then("the zip contains an entry for each document in its respective zaak folder") {
                entries.keys shouldContainExactlyInAnyOrder listOf(
                    "ZAAK-A/inkomend/doc-a-DOC-A.pdf",
                    "ZAAK-B/intern/doc-b-DOC-B.pdf",
                    "samenvatting.txt"
                )
            }

            And("the samenvatting.txt references both zaken") {
                val samenvatting = entries["samenvatting.txt"]!!
                samenvatting shouldContain "ZAAK-A"
                samenvatting shouldContain "ZAAK-B"
            }
        }
    }

    Given("a download that fails with an IOException when reading the file content") {
        val uuid = UUID.randomUUID()
        val zaakUri = URI("https://example.com/zaak/${UUID.randomUUID()}")
        val informatieobject = createEnkelvoudigInformatieObject(
            uuid = uuid,
            ontvangstdatum = LocalDate.now()
        ).apply {
            identificatie = "DOC-FAIL"
            bestandsnaam = "broken.pdf"
        }
        val failingStream = object : ByteArrayInputStream(ByteArray(0)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("simulated I/O failure")
        }

        every { zrcClientService.listZaakinformatieobjecten(informatieobject) } returns
            listOf(createZaakInformatieobjectForCreatesAndUpdates(zaakURL = zaakUri))
        every { zrcClientService.readZaak(zaakUri) } returns createZaak(identificatie = "ZAAK-ERR")
        every { drcClientService.downloadEnkelvoudigInformatieobject(uuid) } returns failingStream

        When("getZipStreamOutput is called and the stream is written") {
            val thrownException = runCatching {
                service.getZipStreamOutput(listOf(informatieobject)).write(ByteArrayOutputStream())
            }.exceptionOrNull()

            Then("an EnkelvoudigInformatieObjectDownloadException is thrown") {
                thrownException.shouldBeInstanceOf<EnkelvoudigInformatieObjectDownloadException>()
            }
        }
    }

    Given("an empty list of informatieobjecten") {
        When("getZipStreamOutput is called") {
            val output = ByteArrayOutputStream()
            service.getZipStreamOutput(emptyList()).write(output)
            val entries = readZipEntries(output)

            Then("the zip contains only a samenvatting.txt with no zaak entries") {
                entries.keys shouldBe setOf("samenvatting.txt")
                entries["samenvatting.txt"] shouldBe ""
            }
        }
    }
})
