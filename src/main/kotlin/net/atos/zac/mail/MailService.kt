/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail

import com.fasterxml.uuid.impl.UUIDUtil
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.exceptions.PdfException
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.IBlockElement
import com.itextpdf.layout.element.IElement
import com.itextpdf.layout.element.Paragraph
import jakarta.annotation.Resource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Transport
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.mail.model.Attachment
import net.atos.zac.mail.model.Bronnen
import net.atos.zac.mail.model.MailAdres
import net.atos.zac.mailtemplates.MailTemplateHelper
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.util.MediaTypes
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import nl.lifely.zac.util.toBase64String
import org.apache.commons.lang3.StringUtils
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class MailService @Inject constructor(
    private var configuratieService: ConfiguratieService,
    private var zgwApiService: ZGWApiService,
    private var ztcClientService: ZtcClientService,
    private var drcClientService: DrcClientService,
    private var mailTemplateHelper: MailTemplateHelper,
    private var loggedInUserInstance: Instance<LoggedInUser>
) {

    companion object {
        private val LOG = Logger.getLogger(MailService::class.java.name)

        @Resource(mappedName = "java:jboss/mail/zac")
        lateinit var mailSession: Session

        // http://www.faqs.org/rfcs/rfc2822.html
        private const val SUBJECT_MAXWIDTH = 78

        private const val FONT_SIZE = 16f
        private const val MAIL_VERZENDER = "Afzender"
        private const val MAIL_ONTVANGER = "Ontvanger"
        private const val MAIL_BIJLAGE = "Bijlage"
        private const val MAIL_ONDERWERP = "Onderwerp"
        private const val MAIL_BERICHT = "Bericht"
    }

    val gemeenteMailAdres
        get() = MailAdres(configuratieService.readGemeenteMail(), configuratieService.readGemeenteNaam())

    fun sendMail(mailGegevens: MailGegevens, bronnen: Bronnen): String {
        val subject = StringUtils.abbreviate(
            resolveVariabelen(mailGegevens.subject, bronnen),
            SUBJECT_MAXWIDTH
        )
        val body = resolveVariabelen(mailGegevens.body, bronnen)
        val attachments = getAttachments(mailGegevens.attachments)
        val message = MailMessageBuilder(
            fromAddress = mailGegevens.from.toAddress(),
            toAddress = mailGegevens.to.toAddress(),
            replyToAddress = mailGegevens.replyTo?.toAddress(),
            mailSubject = subject,
            body = body,
            attachments = attachments
        ).build(mailSession)

        try {
            Transport.send(message)
            LOG.fine("Sent mail to ${mailGegevens.to} with subject '$subject'.")
            if (mailGegevens.isCreateDocumentFromMail && bronnen.zaak != null) {
                createZaakDocumentFromMail(
                    mailGegevens.from.email,
                    mailGegevens.to.email,
                    subject,
                    body,
                    attachments,
                    bronnen.zaak
                )
            }
        } catch (messagingException: MessagingException) {
            LOG.log(Level.SEVERE, "Failed to send mail with subject '$subject'.", messagingException)
        }

        return body
    }

    @Suppress("LongParameterList")
    private fun createZaakDocumentFromMail(
        verzender: String,
        ontvanger: String,
        subject: String,
        body: String,
        attachments: List<Attachment>,
        zaak: Zaak
    ) {
        val eMailObjectType = getEmailInformatieObjectType(zaak)
        val pdfDocument = createPdfDocument(verzender, ontvanger, subject, body, attachments)
        val enkelvoudigInformatieobjectWithInhoud = EnkelvoudigInformatieObjectCreateLockRequest().apply {
            bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
            creatiedatum = LocalDate.now()
            titel = subject
            auteur = loggedInUserInstance.get().getFullName()
            taal = ConfiguratieService.TAAL_NEDERLANDS
            informatieobjecttype = eMailObjectType.url
            inhoud = pdfDocument.toBase64String()
            vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
            formaat = MediaTypes.Application.PDF.mediaType
            bestandsnaam = "$subject.pdf"
            status = StatusEnum.DEFINITIEF
            vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
            verzenddatum = LocalDate.now()
        }

        zgwApiService.createZaakInformatieobjectForZaak(
            zaak,
            enkelvoudigInformatieobjectWithInhoud,
            subject,
            subject,
            ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
        )
    }

    @Suppress("NestedBlockDepth")
    private fun createPdfDocument(
        verzender: String,
        ontvanger: String,
        subject: String,
        body: String,
        attachments: List<Attachment>
    ): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            PdfWriter(byteArrayOutputStream).use { pdfWriter ->
                PdfDocument(pdfWriter).use { pdfDoc ->
                    Document(pdfDoc).use { document ->
                        val font = PdfFontFactory.createFont(StandardFonts.COURIER)

                        document.add(
                            Paragraph().apply {
                                setFont(font).setFontSize(FONT_SIZE).setFontColor(ColorConstants.BLACK)
                                add("$MAIL_VERZENDER: $verzender \n\n")
                                add("$MAIL_ONTVANGER: $ontvanger \n\n")
                                if (attachments.isNotEmpty()) {
                                    val content = attachments.joinToString(",") { attachment: Attachment ->
                                        attachment.filename
                                    }
                                    add("$MAIL_BIJLAGE: $content \n\n")
                                }
                                add("$MAIL_ONDERWERP: $subject \n\n")
                                add("$MAIL_BERICHT \n")
                            }
                        )

                        val cleaner = HtmlCleaner()
                        val rootTagNode = cleaner.clean(body)
                        val cleanerProperties = cleaner.properties.apply {
                            isOmitXmlDeclaration = true
                        }
                        val html = PrettyXmlSerializer(cleanerProperties).getAsString(rootTagNode)

                        val emailBodyParagraph = Paragraph()
                        HtmlConverter.convertToElements(html).forEach(
                            Consumer { element: IElement? ->
                                emailBodyParagraph.add(element as IBlockElement?)
                                // the individual (HTML paragraph) block elements are not separated
                                // with new lines, so we add them explicitly here
                                emailBodyParagraph.add("\n")
                            }
                        )
                        document.add(emailBodyParagraph)
                    }
                }
            }
        } catch (e: PdfException) {
            LOG.log(Level.SEVERE, "Failed to create pdf document", e)
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, "Failed to create pdf document", e)
        }

        return byteArrayOutputStream.toByteArray()
    }

    private fun getEmailInformatieObjectType(zaak: Zaak): InformatieObjectType =
        ztcClientService.readZaaktype(zaak.zaaktype).informatieobjecttypen
            .map { ztcClientService.readInformatieobjecttype(it) }
            .first { it.omschrijving == ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL }

    private fun getAttachments(bijlagenString: Array<String>): List<Attachment> =
        bijlagenString.map { UUIDUtil.uuid(it) }.map { uuid: UUID ->
            val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid)
            val byteArrayInputStream = drcClientService.downloadEnkelvoudigInformatieobject(uuid)
            Attachment(
                enkelvoudigInformatieobject.formaat,
                enkelvoudigInformatieobject.bestandsnaam,
                String(Base64.getEncoder().encode(byteArrayInputStream.readAllBytes()))
            )
        }

    private fun resolveVariabelen(tekst: String, bronnen: Bronnen): String =
        mailTemplateHelper.resolveVariabelen(tekst).let {
            mailTemplateHelper.resolveVariabelen(it, bronnen.zaak)
        }.let {
            mailTemplateHelper.resolveVariabelen(it, bronnen.document)
        }.let {
            mailTemplateHelper.resolveVariabelen(it, bronnen.taskInfo)
        }
}
