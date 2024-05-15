/*
 * SPDX-FileCopyrightText: 2022 Atos
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
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.mail.model.Attachment
import net.atos.zac.mail.model.Bronnen
import net.atos.zac.mail.model.EMail
import net.atos.zac.mail.model.EMails
import net.atos.zac.mail.model.MailAdres
import net.atos.zac.mailtemplates.MailTemplateHelper
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.util.JsonbUtil
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
import java.util.stream.Collectors

@ApplicationScoped
class MailService {
    private var configuratieService: ConfiguratieService? = null
    private var zgwApiService: ZGWApiService? = null
    private var ztcClientService: ZTCClientService? = null
    private var zrcClientService: ZRCClientService? = null
    private var drcClientService: DRCClientService? = null
    private var mailTemplateHelper: MailTemplateHelper? = null
    private var taakVariabelenService: TaakVariabelenService? = null
    private var loggedInUserInstance: Instance<LoggedInUser>? = null

    /**
     * Default no-arg constructor, required by Weld.
     */
    constructor()

    @Inject
    constructor(
        configuratieService: ConfiguratieService?,
        zgwApiService: ZGWApiService?,
        ztcClientService: ZTCClientService?,
        zrcClientService: ZRCClientService?,
        drcClientService: DRCClientService?,
        mailTemplateHelper: MailTemplateHelper?,
        taakVariabelenService: TaakVariabelenService?,
        loggedInUserInstance: Instance<LoggedInUser>?
    ) {
        this.configuratieService = configuratieService
        this.zgwApiService = zgwApiService
        this.ztcClientService = ztcClientService
        this.zrcClientService = zrcClientService
        this.drcClientService = drcClientService
        this.mailTemplateHelper = mailTemplateHelper
        this.taakVariabelenService = taakVariabelenService
        this.loggedInUserInstance = loggedInUserInstance
    }

    private val mailjetClient: MailjetClient? =
        MailjetClientHelper.createMailjetClient(MAILJET_API_KEY, MAILJET_API_SECRET_KEY)

    val gemeenteMailAdres: MailAdres
        get() = MailAdres(configuratieService!!.readGemeenteMail(), configuratieService!!.readGemeenteNaam())

    fun sendMail(mailGegevens: MailGegevens, bronnen: Bronnen): String {
        val subject = StringUtils.abbreviate(
            resolveVariabelen(mailGegevens.subject, bronnen),
            SUBJECT_MAXWIDTH
        )
        val body = resolveVariabelen(mailGegevens.body, bronnen)
        val attachments = getAttachments(mailGegevens.attachments)

        val eMail = EMail(
            mailGegevens.from,
            java.util.List.of(mailGegevens.to),
            mailGegevens.replyTo,
            subject,
            body,
            attachments
        )
        val request = MailjetRequest(Emailv31.resource)
            .setBody(JsonbUtil.JSONB.toJson(EMails(java.util.List.of(eMail))))
        try {
            val status = mailjetClient!!.post(request).status
            if (status < 300) {
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
                LOG.log(
                    Level.WARNING,
                    String.format("Failed to send mail with subject '%s' (http result %d).", subject, status)
                )
            }
        } catch (e: MailjetException) {
            LOG.log(Level.SEVERE, String.format("Failed to send mail with subject '%s'.", subject), e)
        }

        return body
    }

    private fun createZaakDocumentFromMail(
        verzender: String,
        ontvanger: String,
        subject: String,
        body: String,
        attachments: List<Attachment>,
        zaak: Zaak?
    ) {
        val eMailObjectType = getEmailInformatieObjectType(zaak)
        val pdfDocument = createPdfDocument(verzender, ontvanger, subject, body, attachments)

        val enkelvoudigInformatieobjectWithInhoud = EnkelvoudigInformatieObjectData()
        enkelvoudigInformatieobjectWithInhoud.bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
        enkelvoudigInformatieobjectWithInhoud.creatiedatum = LocalDate.now()
        enkelvoudigInformatieobjectWithInhoud.titel = subject
        enkelvoudigInformatieobjectWithInhoud.auteur = loggedInUserInstance!!.get().fullName
        enkelvoudigInformatieobjectWithInhoud.taal = ConfiguratieService.TAAL_NEDERLANDS
        enkelvoudigInformatieobjectWithInhoud.informatieobjecttype = eMailObjectType.url
        enkelvoudigInformatieobjectWithInhoud.inhoud =
            InformatieobjectenUtil.convertByteArrayToBase64String(pdfDocument)
        enkelvoudigInformatieobjectWithInhoud.vertrouwelijkheidaanduiding =
            EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR
        enkelvoudigInformatieobjectWithInhoud.formaat = MEDIA_TYPE_PDF
        enkelvoudigInformatieobjectWithInhoud.bestandsnaam = String.format("%s.pdf", subject)
        enkelvoudigInformatieobjectWithInhoud.status = EnkelvoudigInformatieObjectData.StatusEnum.DEFINITIEF
        enkelvoudigInformatieobjectWithInhoud.vertrouwelijkheidaanduiding =
            EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR
        enkelvoudigInformatieobjectWithInhoud.verzenddatum = LocalDate.now()

        zgwApiService!!.createZaakInformatieobjectForZaak(
            zaak,
            enkelvoudigInformatieobjectWithInhoud,
            subject,
            subject,
            ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
        )
    }

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

                        headerParagraph.setFont(font).setFontSize(16f).setFontColor(ColorConstants.BLACK)
                        headerParagraph.add(String.format("%s: %s %n %n", MAIL_VERZENDER, verzender))
                        headerParagraph.add(String.format("%s: %s %n %n", MAIL_ONTVANGER, ontvanger))
                        if (!attachments.isEmpty()) {
                            val content =
                                attachments.stream().map { attachment: Attachment -> attachment.filename.toString() }
                                    .collect(Collectors.joining(", "))
                            headerParagraph.add(String.format("%s: %s %n %n", MAIL_BIJLAGE, content))
                        }

                        headerParagraph.add(String.format("%s: %s %n %n", MAIL_ONDERWERP, subject))
                        headerParagraph.add(String.format("%s: %n", MAIL_BERICHT))
                        document.add(headerParagraph)

                        val emailBodyParagraph = Paragraph()
                        val cleaner = HtmlCleaner()
                        val rootTagNode = cleaner.clean(body)
                        val cleanerProperties = cleaner.properties
                        cleanerProperties.isOmitXmlDeclaration = true

                        val xmlSerializer: XmlSerializer = PrettyXmlSerializer(cleanerProperties)
                        val html = xmlSerializer.getAsString(rootTagNode)
                        HtmlConverter.convertToElements(html).forEach(Consumer { element: IElement? ->
                            emailBodyParagraph.add(element as IBlockElement?)
                            // the individual (HTML paragraph) block elements are not separated
                            // with new lines, so we add them explicitly here
                            emailBodyParagraph.add("\n")
                        })
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

    private fun getEmailInformatieObjectType(zaak: Zaak?): InformatieObjectType {
        val zaaktype = ztcClientService!!.readZaaktype(zaak!!.zaaktype)
        return zaaktype.informatieobjecttypen.stream()
            .map { informatieobjecttypeURI: URI? ->
                ztcClientService!!.readInformatieobjecttype(
                    informatieobjecttypeURI!!
                )
            }
            .filter { infoObject: InformatieObjectType ->
                (infoObject.omschrijving
                        == ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL)
            }.findFirst()
            .orElseThrow()
    }

    private fun getAttachments(bijlagenString: Array<String>): List<Attachment> {
        val bijlagen: MutableList<UUID> = ArrayList()
        if (ArrayUtils.isNotEmpty<String>(bijlagenString)) {
            Arrays.stream(bijlagenString).forEach { uuidString: String? -> bijlagen.add(UUIDUtil.uuid(uuidString)) }
        } else {
            return emptyList()
        }

        val attachments: MutableList<Attachment> = ArrayList()
        bijlagen.forEach(Consumer { uuid: UUID? ->
            val enkelvoudigInformatieobject = drcClientService!!.readEnkelvoudigInformatieobject(
                uuid
            )
            val byteArrayInputStream = drcClientService!!.downloadEnkelvoudigInformatieobject(
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
        })

        return attachments
    }

    private fun resolveVariabelen(tekst: String, bronnen: Bronnen): String {
        return mailTemplateHelper!!.resolveVariabelen(
            mailTemplateHelper!!.resolveVariabelen(
                mailTemplateHelper!!.resolveVariabelen(
                    mailTemplateHelper!!.resolveVariabelen(tekst),
                    getZaakBron(bronnen)
                ),
                bronnen.document
            ),
            bronnen.taskInfo
        )
    }

    private fun getZaakBron(bronnen: Bronnen): Zaak? {
        return if ((bronnen.zaak != null || bronnen.taskInfo == null)) bronnen.zaak else zrcClientService!!.readZaak(
            taakVariabelenService
                .readZaakUUID(bronnen.taskInfo)
        )
    }

    companion object {
        private val MAILJET_API_KEY: String = ConfigProvider.getConfig().getValue("mailjet.api.key", String::class.java)

        private val MAILJET_API_SECRET_KEY: String =
            ConfigProvider.getConfig().getValue("mailjet.api.secret.key", String::class.java)

        // http://www.faqs.org/rfcs/rfc2822.html
        private const val SUBJECT_MAXWIDTH = 78

        private val LOG: Logger = Logger.getLogger(MailService::class.java.name)

        private const val MEDIA_TYPE_PDF = "application/pdf"

        private const val MAIL_VERZENDER = "Afzender"

        private const val MAIL_ONTVANGER = "Ontvanger"

        private const val MAIL_BIJLAGE = "Bijlage"

        private const val MAIL_ONDERWERP = "Onderwerp"

        private const val MAIL_BERICHT = "Bericht"
    }
}
