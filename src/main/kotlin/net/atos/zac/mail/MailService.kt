/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.errors.MailjetException
import com.mailjet.client.resource.Emailv31
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectData
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.util.InformatieobjectenUtil
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.mail.model.Attachment
import net.atos.zac.mail.model.Bronnen
import net.atos.zac.mail.model.EMail
import net.atos.zac.mail.model.EMails
import net.atos.zac.mail.model.MailAdres
import net.atos.zac.mailtemplates.MailTemplateHelper
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.util.JsonbUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.eclipse.microprofile.config.ConfigProvider
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer
import org.htmlcleaner.XmlSerializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.time.LocalDate
import java.util.Arrays
import java.util.Base64
import java.util.UUID
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class MailService
@Inject
@Suppress("LongParameterList")
constructor(
    private var configuratieService: ConfiguratieService,
    private var zgwApiService: ZGWApiService,
    private var ztcClientService: ZTCClientService,
    private var drcClientService: DRCClientService,
    private var mailTemplateHelper: MailTemplateHelper,
    private var loggedInUserInstance: Instance<LoggedInUser>
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(MailService::class.java.name)

        private val MAILJET_API_KEY: String =
            ConfigProvider.getConfig().getValue("mailjet.api.key", String::class.java)
        private val MAILJET_API_SECRET_KEY: String =
            ConfigProvider.getConfig().getValue("mailjet.api.secret.key", String::class.java)

        private const val HTTP_REDIRECT_RANGE = 300

        // http://www.faqs.org/rfcs/rfc2822.html
        private const val SUBJECT_MAXWIDTH = 78

        private const val FONT_SIZE = 16f

        private const val MEDIA_TYPE_PDF = "application/pdf"

        private const val MAIL_VERZENDER = "Afzender"
        private const val MAIL_ONTVANGER = "Ontvanger"
        private const val MAIL_BIJLAGE = "Bijlage"
        private const val MAIL_ONDERWERP = "Onderwerp"
        private const val MAIL_BERICHT = "Bericht"
    }

    private val mailjetClient: MailjetClient =
        MailjetClientHelper.createMailjetClient(MAILJET_API_KEY, MAILJET_API_SECRET_KEY)

    val gemeenteMailAdres: MailAdres
        get() = MailAdres(configuratieService.readGemeenteMail(), configuratieService.readGemeenteNaam())

    fun sendMail(mailGegevens: MailGegevens, bronnen: Bronnen): String {
        val subject = StringUtils.abbreviate(
            resolveVariabelen(mailGegevens.subject, bronnen),
            SUBJECT_MAXWIDTH
        )
        val body = resolveVariabelen(mailGegevens.body, bronnen)
        val attachments = getAttachments(mailGegevens.attachments)

        val eMail = EMail(
            mailGegevens.from,
            listOf(mailGegevens.to),
            mailGegevens.replyTo,
            subject,
            body,
            attachments
        )
        val request = MailjetRequest(Emailv31.resource)
            .setBody(JsonbUtil.JSONB.toJson(EMails(listOf(eMail))))
        try {
            val status = mailjetClient.post(request).status
            if (status < HTTP_REDIRECT_RANGE) {
                if (mailGegevens.isCreateDocumentFromMail) {
                    createZaakDocumentFromMail(
                        mailGegevens.from.email,
                        mailGegevens.to.email,
                        subject,
                        body,
                        attachments,
                        bronnen.zaak
                    )
                }
            } else {
                LOG.log(Level.WARNING, "Failed to send mail with subject '$subject' (http result $status).")
            }
        } catch (e: MailjetException) {
            LOG.log(Level.SEVERE, "Failed to send mail with subject '$subject'.", e)
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

        val enkelvoudigInformatieobjectWithInhoud = EnkelvoudigInformatieObjectData()
        enkelvoudigInformatieobjectWithInhoud.bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
        enkelvoudigInformatieobjectWithInhoud.creatiedatum = LocalDate.now()
        enkelvoudigInformatieobjectWithInhoud.titel = subject
        enkelvoudigInformatieobjectWithInhoud.auteur = loggedInUserInstance.get().fullName
        enkelvoudigInformatieobjectWithInhoud.taal = ConfiguratieService.TAAL_NEDERLANDS
        enkelvoudigInformatieobjectWithInhoud.informatieobjecttype = eMailObjectType.url
        enkelvoudigInformatieobjectWithInhoud.inhoud =
            InformatieobjectenUtil.convertByteArrayToBase64String(pdfDocument)
        enkelvoudigInformatieobjectWithInhoud.vertrouwelijkheidaanduiding =
            EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR
        enkelvoudigInformatieobjectWithInhoud.formaat = MEDIA_TYPE_PDF
        enkelvoudigInformatieobjectWithInhoud.bestandsnaam = "$subject.pdf"
        enkelvoudigInformatieobjectWithInhoud.status = EnkelvoudigInformatieObjectData.StatusEnum.DEFINITIEF
        enkelvoudigInformatieobjectWithInhoud.vertrouwelijkheidaanduiding =
            EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR
        enkelvoudigInformatieobjectWithInhoud.verzenddatum = LocalDate.now()

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
                        val headerParagraph = Paragraph()
                        val font = PdfFontFactory.createFont(StandardFonts.COURIER)

                        headerParagraph.setFont(font).setFontSize(FONT_SIZE).setFontColor(ColorConstants.BLACK)
                        headerParagraph.add("$MAIL_VERZENDER: $verzender \n\n")
                        headerParagraph.add("$MAIL_ONTVANGER: $ontvanger \n\n")
                        if (attachments.isNotEmpty()) {
                            val content = attachments.joinToString(",") {
                                    attachment: Attachment ->
                                attachment.filename
                            }
                            headerParagraph.add("$MAIL_BIJLAGE: $content \n\n")
                        }

                        headerParagraph.add("$MAIL_ONDERWERP: $subject \n\n")
                        headerParagraph.add("$MAIL_BERICHT \n")
                        document.add(headerParagraph)

                        val emailBodyParagraph = Paragraph()
                        val cleaner = HtmlCleaner()
                        val rootTagNode = cleaner.clean(body)
                        val cleanerProperties = cleaner.properties
                        cleanerProperties.isOmitXmlDeclaration = true

                        val xmlSerializer: XmlSerializer = PrettyXmlSerializer(cleanerProperties)
                        val html = xmlSerializer.getAsString(rootTagNode)
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

    private fun getEmailInformatieObjectType(zaak: Zaak): InformatieObjectType {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        return zaaktype.informatieobjecttypen.stream()
            .map { informatieobjecttypeURI: URI ->
                ztcClientService.readInformatieobjecttype(
                    informatieobjecttypeURI
                )
            }
            .filter { infoObject: InformatieObjectType ->
                (
                    infoObject.omschrijving
                        == ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL
                    )
            }.findFirst()
            .orElseThrow()
    }

    private fun getAttachments(bijlagenString: Array<String>): List<Attachment> {
        val bijlagen: MutableList<UUID> = ArrayList()
        if (ArrayUtils.isNotEmpty(bijlagenString)) {
            Arrays.stream(bijlagenString).forEach { uuidString: String -> bijlagen.add(UUIDUtil.uuid(uuidString)) }
        } else {
            return emptyList()
        }

        val attachments: MutableList<Attachment> = ArrayList()
        bijlagen.forEach(
            Consumer { uuid: UUID ->
                val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(
                    uuid
                )
                val byteArrayInputStream = drcClientService.downloadEnkelvoudigInformatieobject(
                    uuid
                )
                val attachment = Attachment(
                    enkelvoudigInformatieobject.formaat,
                    enkelvoudigInformatieobject.bestandsnaam,
                    String(
                        Base64.getEncoder()
                            .encode(byteArrayInputStream.readAllBytes())
                    )
                )
                attachments.add(attachment)
            }
        )

        return attachments
    }

    private fun resolveVariabelen(tekst: String, bronnen: Bronnen): String {
        return mailTemplateHelper.resolveVariabelen(
            mailTemplateHelper.resolveVariabelen(
                mailTemplateHelper.resolveVariabelen(
                    mailTemplateHelper.resolveVariabelen(tekst),
                    bronnen.zaak
                ),
                bronnen.document
            ),
            bronnen.taskInfo
        )
    }
}
