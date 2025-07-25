/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail

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
import com.itextpdf.layout.element.Paragraph
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Transport
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.util.MediaTypes
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.mail.model.Attachment
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mailtemplates.MailTemplateHelper
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.toBase64String
import org.apache.commons.lang3.StringUtils
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.util.Base64
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.ByteArray
import kotlin.String
import kotlin.Suppress
import kotlin.apply
import kotlin.let
import kotlin.takeIf

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
class MailService @Inject constructor(
    private var configuratieService: ConfiguratieService,
    private var zgwApiService: ZGWApiService,
    private var ztcClientService: ZtcClientService,
    private var drcClientService: DrcClientService,
    private var mailTemplateHelper: MailTemplateHelper,
    private var loggedInUserInstance: Instance<LoggedInUser>,

    @ConfigProperty(name = "SMTP_USERNAME")
    private val smtpUsername: Optional<String> = Optional.empty()
) {
    companion object {
        private val LOG = Logger.getLogger(MailService::class.java.name)

        @Resource(mappedName = "java:jboss/mail/zac")
        lateinit var mailSession: Session

        // http://www.faqs.org/rfcs/rfc2822.html
        private const val SUBJECT_MAX_WIDTH = 78

        private const val FONT_SIZE = 16f
        private const val MAIL_VERZENDER = "Afzender"
        private const val MAIL_ONTVANGER = "Ontvanger"
        private const val MAIL_BIJLAGE = "Bijlage"
        private const val MAIL_ONDERWERP = "Onderwerp"
        private const val MAIL_BERICHT = "Bericht"

        // https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
        private const val JAVAMAIL_SMTP_AUTH_KEY = "mail.smtp.auth"
    }

    fun getGemeenteMailAdres() =
        MailAdres(
            configuratieService.readGemeenteMail(),
            configuratieService.readGemeenteNaam()
        )

    fun sendMail(mailGegevens: MailGegevens, bronnen: Bronnen): String? {
        val subject =
            StringUtils.abbreviate(resolveVariabelen(mailGegevens.subject, bronnen), SUBJECT_MAX_WIDTH)
        val body = resolveVariabelen(mailGegevens.body, bronnen)
        val attachments = getAttachments(mailGegevens.attachments)
        val fromAddress = mailGegevens.from.toAddress()
        val replyToAddress = mailGegevens.replyTo?.toAddress()?.takeIf { fromAddress != it }
        val message = MailMessageBuilder(
            fromAddress = fromAddress,
            toAddress = mailGegevens.to.toAddress(),
            replyToAddress = replyToAddress,
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
            return null
        }

        return body
    }

    @PostConstruct
    @Suppress("UnusedPrivateMember")
    private fun initPasswordAuthentication() {
        // If there's no SMTP_USERNAME environment variable set, we consider this as a case, where SMTP server
        // has no authentication. In this case we disable SMTP authentication in the mail session to prevent sending
        // the default fake credentials configured in src/main/resources/wildfly/configure-wildfly.cli
        //
        // Without the fake credentials, the SMTP mail session is not properly configured, and:
        //    - Weld fails to instantiate the mail session and satisfy the @Resource dependency above
        //    - mail Transport below throws AuthenticationFailedException because of insufficient configuration
        if (!smtpUsername.isPresent) {
            mailSession.properties.setProperty(JAVAMAIL_SMTP_AUTH_KEY, "false")
            LOG.warning { "SMTP authentication disabled" }
        }
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
            bronorganisatie = configuratieService.readBronOrganisatie()
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
                                    val content = attachments.joinToString(",") { it.filename }
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
                        HtmlConverter.convertToElements(html).forEach {
                            emailBodyParagraph.add(it as IBlockElement)
                            // the individual (HTML paragraph) block elements are not separated
                            // with new lines, so we add them explicitly here
                            emailBodyParagraph.add("\n")
                        }
                        document.add(emailBodyParagraph)
                    }
                }
            }
        } catch (pdfException: PdfException) {
            LOG.log(Level.SEVERE, "Failed to create PDF document", pdfException)
        } catch (ioException: IOException) {
            LOG.log(Level.SEVERE, "Failed to create PDF document", ioException)
        }
        return byteArrayOutputStream.toByteArray()
    }

    private fun getEmailInformatieObjectType(zaak: Zaak): InformatieObjectType =
        ztcClientService.readZaaktype(zaak.zaaktype).informatieobjecttypen
            .map { ztcClientService.readInformatieobjecttype(it) }
            .first { it.omschrijving == ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL }

    private fun getAttachments(attachmentUUIDs: List<String>): List<Attachment> =
        attachmentUUIDs
            // currently the client is able to provide empty strings in the attachment UUID array,
            // so we filter them out first
            // ideally we should not allow empty strings in the first place in the corresponding ZAC API endpoint
            .filter(String::isNotBlank)
            .map(UUIDUtil::uuid)
            .map { uuid ->
                val infoObject = drcClientService.readEnkelvoudigInformatieobject(uuid)
                val content = drcClientService.downloadEnkelvoudigInformatieobject(uuid).readAllBytes()
                Attachment(
                    contentType = infoObject.formaat,
                    filename = infoObject.bestandsnaam,
                    base64Content = Base64.getEncoder().encodeToString(content)
                )
            }

    private fun resolveVariabelen(tekst: String, bronnen: Bronnen): String =
        mailTemplateHelper.resolveGemeenteVariable(tekst).let {
            mailTemplateHelper.resolveZaakVariables(it, bronnen.zaak ?: return@let it)
        }.let {
            mailTemplateHelper.resolveEnkelvoudigInformatieObjectVariables(it, bronnen.document ?: return@let it)
        }.let {
            mailTemplateHelper.resolveTaskVariables(it, bronnen.taskInfo ?: return@let it)
        }
}
