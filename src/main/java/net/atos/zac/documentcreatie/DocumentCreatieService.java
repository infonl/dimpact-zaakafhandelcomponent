/*
 * SPDX-FileCopyrightText: 2022 Atos
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

import net.atos.client.smartdocuments.SmartDocumentsClient;
import net.atos.client.smartdocuments.exception.BadRequestException;
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse;
import net.atos.client.smartdocuments.model.wizard.Selection;
import net.atos.client.smartdocuments.model.wizard.SmartDocument;
import net.atos.client.smartdocuments.model.wizard.WizardResponse;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.ztc.ZtcClientService;
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

    private SmartDocumentsClient smartDocumentsClient;
    private String smartDocumentsURL;
    private String authenticationToken;
    private Optional<String> fixedUserName;
    private DataConverter dataConverter;
    private Instance<LoggedInUser> loggedInUserInstance;
    private ZtcClientService ztcClientService;
    private ZRCClientService zrcClientService;

    /**
     * Empty no-op constructor as required by Weld.
     */
    public DocumentCreatieService() {
    }

    @Inject
    public DocumentCreatieService(
            @RestClient SmartDocumentsClient smartDocumentsClient,
            @ConfigProperty(name = "SD_CLIENT_MP_REST_URL") String smartDocumentsURL,
            @ConfigProperty(name = "SD_AUTHENTICATION") String authenticationToken,
            @ConfigProperty(name = "SD_FIXED_USER_NAME") Optional<String> fixedUserName,
            DataConverter dataConverter,
            Instance<LoggedInUser> loggedInUserInstance,
            ZtcClientService ztcClientService,
            ZRCClientService zrcClientService
    ) {
        this.smartDocumentsClient = smartDocumentsClient;
        this.smartDocumentsURL = smartDocumentsURL;
        this.authenticationToken = authenticationToken;
        this.fixedUserName = fixedUserName;
        this.dataConverter = dataConverter;
        this.loggedInUserInstance = loggedInUserInstance;
        this.ztcClientService = ztcClientService;
        this.zrcClientService = zrcClientService;
    }

    /**
     * Create a document using the SmartDocuments wizard.
     *
     * @param documentCreatieGegevens Gegevens op basis van welke het document wordt gecreeerd.
     * @return De redirect URL naar de SmartDocuments Wizard
     */
    public DocumentCreatieResponse creeerDocumentAttendedSD(final DocumentCreatieGegevens documentCreatieGegevens) {
        final LoggedInUser loggedInUser = loggedInUserInstance.get();
        final Registratie registratie = createRegistratie(documentCreatieGegevens);
        final Data data = dataConverter.createData(documentCreatieGegevens, loggedInUser);
        final WizardRequest wizardRequest = new WizardRequest(createSmartDocument(documentCreatieGegevens), registratie, data);
        final String userName = fixedUserName.orElse(loggedInUser.getId());
        try {
            LOG.fine(String.format("Starting Smart Documents wizard for user: '%s'", userName));
            final WizardResponse wizardResponse = smartDocumentsClient.wizardDeposit(
                    format("Basic %s", authenticationToken),
                    userName,
                    wizardRequest
            );
            return new DocumentCreatieResponse(
                    UriBuilder.fromUri(smartDocumentsURL)
                            .path("smartdocuments/wizard")
                            .queryParam("ticket", wizardResponse.getTicket())
                            .build()
            );
        } catch (final BadRequestException badRequestException) {
            return new DocumentCreatieResponse("Aanmaken van een document is helaas niet mogelijk. " +
                                               "Ben je als user geregistreerd in SmartDocuments? " +
                                               "Details: " + badRequestException.getMessage());
        }
    }

    private SmartDocument createSmartDocument(final DocumentCreatieGegevens documentCreatieGegevens) {
        final SmartDocument smartDocument = new SmartDocument();
        smartDocument.setSelection(new Selection());
        smartDocument.getSelection().setTemplateGroup(
                ztcClientService.readZaaktype(documentCreatieGegevens.getZaak().getZaaktype()).getOmschrijving()
        );
        return smartDocument;
    }

    private Registratie createRegistratie(final DocumentCreatieGegevens documentCreatieGegevens) {
        final Registratie registratie = new Registratie();
        registratie.bronOrganisatie = BRON_ORGANISATIE;
        registratie.zaak = zrcClientService.createUrlExternToZaak(documentCreatieGegevens.getZaak().getUuid());
        registratie.informatieObjectStatus = documentCreatieGegevens.getInformatieobjectStatus();
        registratie.informatieObjectType = documentCreatieGegevens.getInformatieobjecttype().getUrl();
        registratie.creatieDatum = LocalDate.now();
        registratie.auditToelichting = AUDIT_TOELICHTING;
        return registratie;
    }

    /**
     * Lists all templates available for the current user
     *
     * @return A structure describing templates and groups
     */
    public SmartDocumentsTemplatesResponse listTemplates() {
        final LoggedInUser loggedInUser = loggedInUserInstance.get();
        final String userName = fixedUserName.orElse(loggedInUser.getId());
        return smartDocumentsClient.listTemplates(
                format("Basic %s", authenticationToken),
                userName
        );
    }
}
