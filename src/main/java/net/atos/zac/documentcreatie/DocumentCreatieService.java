/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreatie;

import static java.lang.String.format;
import static net.atos.zac.configuratie.ConfiguratieService.BRON_ORGANISATIE;

import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.sd.SmartDocumentsClient;
import net.atos.client.sd.exception.BadRequestException;
import net.atos.client.sd.model.Selection;
import net.atos.client.sd.model.SmartDocument;
import net.atos.client.sd.model.WizardResponse;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.documentcreatie.converter.DataConverter;
import net.atos.zac.documentcreatie.model.Data;
import net.atos.zac.documentcreatie.model.DocumentCreatieGegevens;
import net.atos.zac.documentcreatie.model.DocumentCreatieResponse;
import net.atos.zac.documentcreatie.model.Registratie;
import net.atos.zac.documentcreatie.model.WizardRequest;

@ApplicationScoped
public class DocumentCreatieService {
    private static final String AUDIT_TOELICHTING = "Door SmartDocuments";
    private static final Logger LOG = Logger.getLogger(DocumentCreatieService.class.getName());

    @Inject @RestClient private SmartDocumentsClient smartDocumentsClient;

    @Inject
    @ConfigProperty(name = "SD_CLIENT_MP_REST_URL")
    private String smartDocumentsURL;

    @Inject
    @ConfigProperty(name = "SD_AUTHENTICATION")
    private String authenticationToken;

    @Inject
    @ConfigProperty(name = "SD_FIXED_USER_NAME")
    private Optional<String> fixedUserName;

    @Inject private DataConverter dataConverter;

    @Inject private Instance<LoggedInUser> loggedInUserInstance;

    @Inject private ZTCClientService ztcClientService;

    @Inject private ZRCClientService zrcClientService;

    /**
     * Create a document using the SmartDocuments wizard.
     *
     * @param documentCreatieGegevens Gegevens op basis van welke het document wordt gecreeerd.
     * @return De redirect URL naar de SmartDocuments Wizard
     */
    public DocumentCreatieResponse creeerDocumentAttendedSD(
            final DocumentCreatieGegevens documentCreatieGegevens) {
        final LoggedInUser loggedInUser = loggedInUserInstance.get();
        final Registratie registratie = createRegistratie(documentCreatieGegevens);
        final Data data = dataConverter.createData(documentCreatieGegevens, loggedInUser);
        final WizardRequest wizardRequest =
                new WizardRequest(createSmartDocument(documentCreatieGegevens), registratie, data);
        final String userName = fixedUserName.orElse(loggedInUser.getId());
        try {
            LOG.fine(String.format("Starting Smart Documents wizard for user: '%s'", userName));
            final WizardResponse wizardResponse =
                    smartDocumentsClient.wizardDeposit(
                            format("Basic %s", authenticationToken), userName, wizardRequest);
            return new DocumentCreatieResponse(
                    UriBuilder.fromUri(smartDocumentsURL)
                            .path("smartdocuments/wizard")
                            .queryParam("ticket", wizardResponse.ticket)
                            .build());
        } catch (final BadRequestException badRequestException) {
            return new DocumentCreatieResponse(
                    "Aanmaken van een document is helaas niet mogelijk. (ben je als user"
                            + " geregistreerd in SmartDocuments?)");
        }
    }

    private SmartDocument createSmartDocument(
            final DocumentCreatieGegevens documentCreatieGegevens) {
        final SmartDocument smartDocument = new SmartDocument();
        smartDocument.selection = new Selection();
        smartDocument.selection.templateGroup =
                ztcClientService
                        .readZaaktype(documentCreatieGegevens.getZaak().getZaaktype())
                        .getOmschrijving();
        return smartDocument;
    }

    private Registratie createRegistratie(final DocumentCreatieGegevens documentCreatieGegevens) {
        final Registratie registratie = new Registratie();
        registratie.bronOrganisatie = BRON_ORGANISATIE;
        registratie.zaak =
                zrcClientService.createUrlExternToZaak(documentCreatieGegevens.getZaak().getUuid());
        registratie.informatieObjectStatus = documentCreatieGegevens.getInformatieobjectStatus();
        registratie.informatieObjectType =
                documentCreatieGegevens.getInformatieobjecttype().getUrl();
        registratie.creatieDatum = LocalDate.now();
        registratie.auditToelichting = AUDIT_TOELICHTING;
        return registratie;
    }
}
