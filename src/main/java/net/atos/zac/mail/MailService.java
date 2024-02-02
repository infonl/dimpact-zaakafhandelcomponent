/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.mail;

import static java.util.stream.Collectors.joining;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertByteArrayToBase64String;
import static net.atos.zac.configuratie.ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN;
import static net.atos.zac.util.JsonbUtil.JSONB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XmlSerializer;

import com.fasterxml.uuid.impl.UUIDUtil;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;

import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObject;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObjectData;
import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.flowable.TaakVariabelenService;
import net.atos.zac.mail.model.Attachment;
import net.atos.zac.mail.model.Bronnen;
import net.atos.zac.mail.model.EMail;
import net.atos.zac.mail.model.EMails;
import net.atos.zac.mail.model.MailAdres;
import net.atos.zac.mailtemplates.MailTemplateHelper;
import net.atos.zac.mailtemplates.model.MailGegevens;

@ApplicationScoped
public class MailService {

    private static final String MAILJET_API_KEY =
            ConfigProvider.getConfig().getValue("mailjet.api.key", String.class);

    private static final String MAILJET_API_SECRET_KEY =
            ConfigProvider.getConfig().getValue("mailjet.api.secret.key", String.class);

    // http://www.faqs.org/rfcs/rfc2822.html
    private static final int SUBJECT_MAXWIDTH = 78;

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    private static final String MEDIA_TYPE_PDF = "application/pdf";

    private static final String MAIL_VERZENDER = "Afzender";

    private static final String MAIL_ONTVANGER = "Ontvanger";

    private static final String MAIL_BIJLAGE = "Bijlage";

    private static final String MAIL_ONDERWERP = "Onderwerp";

    private static final String MAIL_BERICHT = "Bericht";

    @Inject
    private ConfiguratieService configuratieService;

    @Inject
    private ZGWApiService zgwApiService;

    @Inject
    private ZTCClientService ztcClientService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private DRCClientService drcClientService;

    @Inject
    private MailTemplateHelper mailTemplateHelper;

    @Inject
    private TaakVariabelenService taakVariabelenService;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    private final ClientOptions clientOptions = ClientOptions.builder().apiKey(MAILJET_API_KEY)
            .apiSecretKey(MAILJET_API_SECRET_KEY).build();

    private final MailjetClient mailjetClient = new MailjetClient(clientOptions);

    public MailAdres getGemeenteMailAdres() {
        return new MailAdres(configuratieService.readGemeenteMail(), configuratieService.readGemeenteNaam());
    }

    public String sendMail(final MailGegevens mailGegevens, final Bronnen bronnen) {

        final String subject = StringUtils.abbreviate(
                resolveVariabelen(mailGegevens.getSubject(), bronnen),
                SUBJECT_MAXWIDTH);
        final String body = resolveVariabelen(mailGegevens.getBody(), bronnen);
        final List<Attachment> attachments = getAttachments(mailGegevens.getAttachments());

        final EMail eMail = new EMail(
                mailGegevens.getFrom(), List.of(mailGegevens.getTo()), mailGegevens.getReplyTo(),
                subject, body, attachments);
        final MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .setBody(JSONB.toJson(new EMails(List.of(eMail))));
        try {
            final int status = mailjetClient.post(request).getStatus();
            if (status < 300) {
                if (mailGegevens.isCreateDocumentFromMail()) {
                    createZaakDocumentFromMail(
                            mailGegevens.getFrom().getEmail(), mailGegevens.getTo().getEmail(),
                            subject, body, attachments,
                            bronnen.zaak);
                }
            } else {
                LOG.log(Level.WARNING,
                        String.format("Failed to send mail with subject '%s' (http result %d).", subject, status));
            }
        } catch (MailjetException e) {
            LOG.log(Level.SEVERE, String.format("Failed to send mail with subject '%s'.", subject), e);
        }

        return body;
    }

    private void createZaakDocumentFromMail(final String verzender, final String ontvanger, final String subject,
            final String body, final List<Attachment> attachments, final Zaak zaak) {
        final EnkelvoudigInformatieObjectData informatieObject =
                createDocumentInformatieObject(verzender, ontvanger, subject, body, attachments, zaak);
        zgwApiService.createZaakInformatieobjectForZaak(zaak, informatieObject, subject,
                                                        subject, OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN);
    }

    private EnkelvoudigInformatieObjectData createDocumentInformatieObject(final String verzender,
            final String ontvanger, final String subject, final String body, final List<Attachment> attachments,
            final Zaak zaak) {
        final InformatieObjectType eMailObjectType = getEmailInformatieObjectType(zaak);
        final byte[] pdfDocument = createPdfDocument(verzender, ontvanger, subject, body, attachments);

        final EnkelvoudigInformatieObjectData enkelvoudigInformatieobjectWithInhoud = new EnkelvoudigInformatieObjectData();
        enkelvoudigInformatieobjectWithInhoud.setBronorganisatie(ConfiguratieService.BRON_ORGANISATIE);
        enkelvoudigInformatieobjectWithInhoud.setCreatiedatum(LocalDate.now());
        enkelvoudigInformatieobjectWithInhoud.setTitel(subject);
        enkelvoudigInformatieobjectWithInhoud.setAuteur(loggedInUserInstance.get().getFullName());
        enkelvoudigInformatieobjectWithInhoud.setTaal(ConfiguratieService.TAAL_NEDERLANDS);
        enkelvoudigInformatieobjectWithInhoud.setInformatieobjecttype(eMailObjectType.getUrl());
        enkelvoudigInformatieobjectWithInhoud.setInhoud(convertByteArrayToBase64String(pdfDocument));
        enkelvoudigInformatieobjectWithInhoud.setVertrouwelijkheidaanduiding(
                EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR);
        enkelvoudigInformatieobjectWithInhoud.setFormaat(MEDIA_TYPE_PDF);
        enkelvoudigInformatieobjectWithInhoud.setBestandsnaam(String.format("%s.pdf", subject));
        enkelvoudigInformatieobjectWithInhoud.setStatus(EnkelvoudigInformatieObjectData.StatusEnum.DEFINITIEF);
        enkelvoudigInformatieobjectWithInhoud.setVertrouwelijkheidaanduiding(
                EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR);
        enkelvoudigInformatieobjectWithInhoud.setVerzenddatum(LocalDate.now());
        return enkelvoudigInformatieobjectWithInhoud;
    }

    private byte[] createPdfDocument(final String verzender, final String ontvanger, final String subject,
            final String body, final List<Attachment> attachments) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final Document document = new Document();
        try {
            PdfWriter.getInstance(document, byteArrayOutputStream);
            document.open();
            document.addTitle(subject);
            final Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
            final Paragraph paragraph = new Paragraph(StringUtils.EMPTY, font);
            addToParagraph(paragraph, MAIL_VERZENDER, verzender);
            addToParagraph(paragraph, MAIL_ONTVANGER, ontvanger);
            if (!attachments.isEmpty()) {
                addToParagraph(paragraph, MAIL_BIJLAGE,
                               attachments.stream().map(attachment -> String.valueOf(attachment.getFilename()))
                                       .collect(joining(", ")));
            }
            addToParagraph(paragraph, MAIL_ONDERWERP, subject);
            paragraph.add(MAIL_BERICHT);

            final HtmlCleaner cleaner = new HtmlCleaner();
            final TagNode rootTagNode = cleaner.clean(body);
            final CleanerProperties cleanerProperties = cleaner.getProperties();
            cleanerProperties.setOmitXmlDeclaration(true);

            final XmlSerializer xmlSerializer = new PrettyXmlSerializer(cleanerProperties);
            final String html = xmlSerializer.getAsString(rootTagNode);

            paragraph.addAll(XMLWorkerHelper.parseToElementList(html, null));
            document.add(paragraph);
            document.close();
        } catch (final DocumentException | IOException e) {
            LOG.log(Level.SEVERE, "Failed to create pdf document", e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private void addToParagraph(final Paragraph paragraph, final String propertie, final String inhoud) {
        paragraph.add(String.format("%s: %s", propertie, inhoud));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
    }

    private InformatieObjectType getEmailInformatieObjectType(final Zaak zaak) {
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());
        return zaaktype.getInformatieobjecttypen().stream()
                .map(ztcClientService::readInformatieobjecttype)
                .filter(infoObject -> infoObject.getOmschrijving()
                        .equals(ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_EMAIL)).findFirst()
                .orElseThrow();
    }

    private List<Attachment> getAttachments(final String[] bijlagenString) {
        final List<UUID> bijlagen = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(bijlagenString)) {
            Arrays.stream(bijlagenString).forEach(uuidString -> bijlagen.add(UUIDUtil.uuid(uuidString)));
        } else {
            return Collections.emptyList();
        }

        final List<Attachment> attachments = new ArrayList<>();
        bijlagen.forEach(uuid -> {
            final EnkelvoudigInformatieObject enkelvoudigInformatieobject =
                    drcClientService.readEnkelvoudigInformatieobject(
                    uuid);
            final ByteArrayInputStream byteArrayInputStream = drcClientService.downloadEnkelvoudigInformatieobject(
                    uuid);
            final Attachment attachment = new Attachment(enkelvoudigInformatieobject.getFormaat(),
                                                         enkelvoudigInformatieobject.getBestandsnaam(),
                                                         new String(Base64.getEncoder()
                                                                            .encode(byteArrayInputStream.readAllBytes())));
            attachments.add(attachment);
        });

        return attachments;
    }

    private String resolveVariabelen(final String tekst, final Bronnen bronnen) {
        return mailTemplateHelper.resolveVariabelen(
                mailTemplateHelper.resolveVariabelen(
                        mailTemplateHelper.resolveVariabelen(
                                mailTemplateHelper.resolveVariabelen(
                                        tekst),
                                getZaakBron(bronnen))
                        , bronnen.document)
                , bronnen.taskInfo);
    }

    private Zaak getZaakBron(final Bronnen bronnen) {
        return (bronnen.zaak != null || bronnen.taskInfo == null)
                ? bronnen.zaak
                : zrcClientService.readZaak(taakVariabelenService.readZaakUUID(bronnen.taskInfo));
    }
}
