/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail

import com.fasterxml.uuid.impl.UUIDUtil
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.Transport
import net.atos.zac.util.MediaTypes
import nl.info.client.officeconverter.OfficeConverterClientService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.mail.model.Attachment
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mailtemplates.MailTemplateHelper
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.mailtemplates.model.MailTemplateVariables
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.toBase64String
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer
import java.io.ByteArrayInputStream
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
    private var configurationService: ConfigurationService,
    private var zgwApiService: ZgwApiService,
    private var ztcClientService: ZtcClientService,
    private var drcClientService: DrcClientService,
    private var mailTemplateHelper: MailTemplateHelper,
    private var officeConverterClientService: OfficeConverterClientService,
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

        private const val EMAIL_HTML_FILENAME = "email.html"
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
            configurationService.readGemeenteMail(),
            configurationService.readGemeenteNaam()
        )

    fun sendMail(mailGegevens: MailGegevens, bronnen: Bronnen): String? {
        val zaakdata: Map<String, Any> = bronnen.zaak
            ?.takeIf {
                mailGegevens.subject.contains(MailTemplateVariables.ZAAKDATA_VARIABLE_PREFIX) ||
                    mailGegevens.body.contains(MailTemplateVariables.ZAAKDATA_VARIABLE_PREFIX)
            }
            ?.let { mailTemplateHelper.readZaakdata(it) }
            ?: emptyMap()
        val subject =
            StringUtils.abbreviate(resolveVariabelen(mailGegevens.subject, bronnen, zaakdata), SUBJECT_MAX_WIDTH)
        val body = resolveVariabelen(mailGegevens.body, bronnen, zaakdata)
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
        val html = buildEmailHtml(verzender, ontvanger, subject, body, attachments)
        val pdfDocument = try {
            officeConverterClientService.convertToPDF(
                ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)),
                EMAIL_HTML_FILENAME
            ).use { it.readAllBytes() }
        } catch (@Suppress("TooGenericExceptionCaught") exception: RuntimeException) {
            LOG.log(
                Level.SEVERE,
                "Failed to convert the sent e-mail with subject '$subject' to PDF. No zaak document was created.",
                exception
            )
            return
        }
        val enkelvoudigInformatieobjectWithInhoud = EnkelvoudigInformatieObjectCreateLockRequest().apply {
            bronorganisatie = configurationService.readBronOrganisatie()
            creatiedatum = LocalDate.now()
            titel = subject
            auteur = loggedInUserInstance.get().getFullName()
            taal = ConfigurationService.TAAL_NEDERLANDS
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
            ConfigurationService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
        )
    }

    private fun buildEmailHtml(
        verzender: String,
        ontvanger: String,
        subject: String,
        body: String,
        attachments: List<Attachment>
    ): String {
        val headerHtml = buildList {
            add("$MAIL_VERZENDER: $verzender")
            add("$MAIL_ONTVANGER: $ontvanger")
            if (attachments.isNotEmpty()) {
                add("$MAIL_BIJLAGE: ${attachments.joinToString(",") { it.filename }}")
            }
            add("$MAIL_ONDERWERP: $subject")
            add(MAIL_BERICHT)
        }.joinToString("\n") { "<p>${escapeHtml4(it)}</p>" }
        val cleaner = HtmlCleaner()
        val rootTagNode = cleaner.clean(body)
        val cleanerProperties = cleaner.properties.apply {
            isOmitXmlDeclaration = true
        }
        // HtmlCleaner wraps the body in an html/head/body envelope of its own, which LibreOffice ignores.
        // Its omit-envelope serializer option throws on the resulting nameless root node, so we leave it in.
        val sanitisedBody = PrettyXmlSerializer(cleanerProperties).getAsString(rootTagNode)
        // without the charset meta tag LibreOffice mangles diacritics
        return """
            <!DOCTYPE html>
            <html lang="nl">
            <head>
            <meta charset="utf-8">
            <style>
            body { font-family: sans-serif; }
            .header { font-family: monospace; }
            </style>
            </head>
            <body>
            <div class="header">
            $headerHtml
            </div>
            $sanitisedBody
            </body>
            </html>
        """.trimIndent()
    }

    private fun getEmailInformatieObjectType(zaak: Zaak): InformatieObjectType =
        ztcClientService.readZaaktype(zaak.zaaktype).informatieobjecttypen
            .map { ztcClientService.readInformatieobjecttype(it) }
            .first { it.omschrijving == ConfigurationService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL }

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

    private fun resolveVariabelen(tekst: String, bronnen: Bronnen, zaakdata: Map<String, Any>): String =
        mailTemplateHelper.resolveGemeenteVariable(tekst).let {
            mailTemplateHelper.resolveZaakVariables(it, bronnen.zaak ?: return@let it, loggedInUserInstance.get().id)
        }.let {
            mailTemplateHelper.resolveEnkelvoudigInformatieObjectVariables(it, bronnen.document ?: return@let it)
        }.let {
            mailTemplateHelper.resolveTaskVariables(it, bronnen.taskInfo ?: return@let it)
        }.let {
            mailTemplateHelper.resolveZaakdataVariables(it, zaakdata)
        }
}
