/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.converter

import jakarta.inject.Inject
import net.atos.zac.app.mail.model.RESTMailGegevens
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.shared.toDrcVertrouwelijkheidaanduidingEnum
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mailtemplates.model.MailGegevens

class RESTMailGegevensConverter @Inject constructor(
    private val configurationService: ConfigurationService
) {
    fun convert(restMailGegevens: RESTMailGegevens): MailGegevens {
        // Note that most of the actual conversion happens in the constructor.
        // Please do not move it here, because MailGegevens do not always get constructed here.
        val afzender = configurationService.readGemeenteNaam()
        return MailGegevens(
            from = MailAdres(restMailGegevens.verzender!!, afzender),
            to = MailAdres(restMailGegevens.ontvanger!!, null),
            replyTo = restMailGegevens.replyTo?.let { MailAdres(it, afzender) },
            subject = restMailGegevens.onderwerp!!,
            body = restMailGegevens.body!!,
            attachments = restMailGegevens.bijlagen,
            isCreateDocumentFromMail = restMailGegevens.createDocumentFromMail,
            vertrouwelijkheidaanduiding = restMailGegevens.vertrouwelijkheidaanduiding
                ?.toDrcVertrouwelijkheidaanduidingEnum()
                ?: VertrouwelijkheidaanduidingEnum.OPENBAAR
        )
    }
}