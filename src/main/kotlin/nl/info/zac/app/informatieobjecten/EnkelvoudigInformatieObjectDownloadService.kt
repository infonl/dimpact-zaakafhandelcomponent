/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten

import jakarta.inject.Inject
import jakarta.ws.rs.core.StreamingOutput
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.informatieobjecten.exception.EnkelvoudigInformatieObjectDownloadException
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("TooGenericExceptionThrown")
class EnkelvoudigInformatieObjectDownloadService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService
) {
    companion object {
        private const val RICHTING_INKOMEND = "inkomend"
        private const val RICHTING_UITGAAND = "uitgaand"
        private const val RICHTING_INTERN = "intern"
        private const val SAMENVATTING_BESTANDSNAAM = "samenvatting.txt"
    }

    fun getZipStreamOutput(informatieobjecten: List<EnkelvoudigInformatieObject>): StreamingOutput =
        StreamingOutput { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOutputStream ->
                val samenvatting = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
                informatieobjecten.forEach { informatieobject ->
                    samenvattingAddInformatieObject(
                        pad = addInformatieObjectToZip(
                            informatieobject = informatieobject,
                            zipOutputStream = zipOutputStream
                        ),
                        samenvatting = samenvatting
                    )
                }
                zipAddSamenvatting(samenvatting = samenvatting, zipOutputStream = zipOutputStream)
                zipOutputStream.finish()
            }
            outputStream.flush()
            outputStream.close()
        }

    private fun addInformatieObjectToZip(informatieobject: EnkelvoudigInformatieObject, zipOutputStream: ZipOutputStream): String {
        val pad = getInformatieObjectZipPath(informatieobject)
        val zipEntry = ZipEntry(pad)
        try {
            zipOutputStream.putNextEntry(zipEntry)
            zipOutputStream.write(getInformatieObjectInhoud(informatieobject.url.extractUuid()))
            zipOutputStream.closeEntry()
        } catch (ioException: IOException) {
            throw EnkelvoudigInformatieObjectDownloadException(
                "Failed to add enkelvoudiginformatieobject with identification '${informatieobject.identificatie}' to zip outputStream",
                ioException
            )
        }
        return pad
    }

    private fun getInformatieObjectInhoud(uuid: UUID): ByteArray =
        drcClientService.downloadEnkelvoudigInformatieobject(uuid).readBytes()

    private fun getInformatieObjectZipPath(enkelvoudigInformatieobject: EnkelvoudigInformatieObject): String {
        val zaakInformatieObjectenList = zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieobject)
        val zaakUri = zaakInformatieObjectenList.first().zaak
        val zaakId = zrcClientService.readZaak(zaakUri).identificatie
        val subfolder = when {
            enkelvoudigInformatieobject.ontvangstdatum != null -> RICHTING_INKOMEND
            enkelvoudigInformatieobject.verzenddatum != null -> RICHTING_UITGAAND
            else -> RICHTING_INTERN
        }
        val bestandsnaam = enkelvoudigInformatieobject.bestandsnaam
        val baseName = bestandsnaam.substringBeforeLast(".")
        val extension = bestandsnaam.substringAfterLast(".", "")

        val naamMetIdentificatie = when {
            baseName.isNotEmpty() -> "$baseName-${enkelvoudigInformatieobject.identificatie}"
            else -> "-${enkelvoudigInformatieobject.identificatie}"
        }

        return if (extension.isNotEmpty()) {
            "$zaakId/$subfolder/$naamMetIdentificatie.$extension"
        } else {
            "$zaakId/$subfolder/$naamMetIdentificatie"
        }
    }

    private fun samenvattingAddInformatieObject(
        pad: String,
        samenvatting: MutableMap<String, MutableMap<String, MutableList<String>>>
    ) {
        val (zaakId, richting, bestandsnaam) = pad.split("/")
        samenvatting.getOrPut(zaakId) { mutableMapOf() }
            .getOrPut(richting) { mutableListOf() }
            .add(bestandsnaam)
    }

    private fun zipAddSamenvatting(samenvatting: Map<String, Map<String, List<String>>>, zipOutputStream: ZipOutputStream) {
        val zipEntry = ZipEntry(SAMENVATTING_BESTANDSNAAM)
        val stringBuilder = StringBuilder()

        samenvatting.forEach { (zaak, richtingen) ->
            stringBuilder.append(zaak)
            stringBuilder.append(":\n")
            richtingen.forEach { (richting, bestanden) ->
                stringBuilder.append('\t')
                stringBuilder.append(richting)
                stringBuilder.append(":\n")
                bestanden.forEach { bestand ->
                    stringBuilder.append("\t  - ")
                    stringBuilder.append(bestand)
                    stringBuilder.append("\n")
                }
            }
            stringBuilder.append('\n')
        }

        try {
            zipOutputStream.putNextEntry(zipEntry)
            zipOutputStream.write(stringBuilder.toString().toByteArray())
            zipOutputStream.closeEntry()
        } catch (ioException: IOException) {
            throw RuntimeException(ioException)
        }
    }
}
