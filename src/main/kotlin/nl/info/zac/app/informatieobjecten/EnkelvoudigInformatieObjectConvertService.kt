/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.officeconverter.OfficeConverterClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.app.informatieobjecten.exception.EnkelvoudigInformatieObjectConversionException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.toBase64String
import org.apache.commons.lang3.StringUtils
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class EnkelvoudigInformatieObjectConvertService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val officeConverterClientService: OfficeConverterClientService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService
) {
    companion object {
        private const val TOELICHTING_PDF = "Geconverteerd naar PDF"
    }

    fun convertEnkelvoudigInformatieObject(document: EnkelvoudigInformatieObject, enkelvoudigInformatieobjectUUID: UUID) {
        if (document.status != StatusEnum.DEFINITIEF) {
            throw EnkelvoudigInformatieObjectConversionException()
        }
        drcClientService.downloadEnkelvoudigInformatieobject(
            enkelvoudigInformatieobjectUUID
        ).use { documentInputStream ->
            officeConverterClientService.convertToPDF(
                documentInputStream,
                document.bestandsnaam
            ).use { pdfInputStream ->
                val pdf = EnkelvoudigInformatieObjectWithLockRequest()
                val inhoud = pdfInputStream.readAllBytes()
                pdf.inhoud = inhoud.toBase64String()
                pdf.formaat = MediaTypes.Application.PDF.mediaType
                pdf.bestandsnaam = StringUtils.substringBeforeLast(document.bestandsnaam, ".") + ".pdf"
                pdf.bestandsomvang = inhoud.size
                enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                    document.url.extractUuid(),
                    pdf,
                    TOELICHTING_PDF
                )
            }
        }
    }
}
