/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation;

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
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.documentcreation.converter.DocumentCreationDataConverter;
import net.atos.zac.documentcreation.model.Data;
import net.atos.zac.documentcreation.model.DocumentCreationData;
import net.atos.zac.documentcreation.model.DocumentCreationResponse;
import net.atos.zac.documentcreation.model.Registratie;
import net.atos.zac.documentcreation.model.WizardRequest;

@ApplicationScoped
public class SmartDocumentsService {
    private static final String AUDIT_TOELICHTING = "Door SmartDocuments";
    private static final Logger LOG = Logger.getLogger(SmartDocumentsService.class.getName());

    private SmartDocumentsClient smartDocumentsClient;
    private String smartDocumentsURL;
    private String authenticationToken;
    private Optional<String> fixedUserName;
    private DocumentCreationDataConverter documentCreationDataConverter;
    private Instance<LoggedInUser> loggedInUserInstance;
    private ZtcClientService ztcClientService;
    private ZrcClientService zrcClientService;

    /**
     * Empty no-op constructor as required by Weld.
     */
    public SmartDocumentsService() {
    }

    @Inject
    public SmartDocumentsService(
            @RestClient SmartDocumentsClient smartDocumentsClient,
            @ConfigProperty(name = "SD_CLIENT_MP_REST_URL") String smartDocumentsURL,
            @ConfigProperty(name = "SD_AUTHENTICATION") String authenticationToken,
            @ConfigProperty(name = "SD_FIXED_USER_NAME") Optional<String> fixedUserName,
            DocumentCreationDataConverter documentCreationDataConverter,
            Instance<LoggedInUser> loggedInUserInstance,
            ZtcClientService ztcClientService,
            ZrcClientService zrcClientService
    ) {
        this.smartDocumentsClient = smartDocumentsClient;
        this.smartDocumentsURL = smartDocumentsURL;
        this.authenticationToken = authenticationToken;
        this.fixedUserName = fixedUserName;
        this.documentCreationDataConverter = documentCreationDataConverter;
        this.loggedInUserInstance = loggedInUserInstance;
        this.ztcClientService = ztcClientService;
        this.zrcClientService = zrcClientService;
    }

    /**
     * Create a document using the SmartDocuments wizard.
     *
     * @param documentCreationData Gegevens op basis van welke het document wordt gecreeerd.
     * @return De redirect URL naar de SmartDocuments Wizard
     */
    public DocumentCreationResponse createDocumentAttended(final DocumentCreationData documentCreationData) {
        final LoggedInUser loggedInUser = loggedInUserInstance.get();
        final Registratie registratie = createRegistratie(documentCreationData);
        final Data data = documentCreationDataConverter.createData(documentCreationData, loggedInUser);
        final WizardRequest wizardRequest = new WizardRequest(createSmartDocument(documentCreationData), registratie, data);
        final String userName = fixedUserName.orElse(loggedInUser.getId());
        try {
            LOG.fine(String.format("Starting Smart Documents wizard for user: '%s'", userName));
            final WizardResponse wizardResponse = smartDocumentsClient.wizardDeposit(
                    format("Basic %s", authenticationToken),
                    userName,
                    wizardRequest
            );
            return new DocumentCreationResponse(
                    UriBuilder.fromUri(smartDocumentsURL)
                            .path("smartdocuments/wizard")
                            .queryParam("ticket", wizardResponse.getTicket())
                            .build()
            );
        } catch (final BadRequestException badRequestException) {
            return new DocumentCreationResponse("Aanmaken van een document is helaas niet mogelijk. " +
                                                "Ben je als user geregistreerd in SmartDocuments? " +
                                                "Details: " + badRequestException.getMessage());
        }
    }

    private SmartDocument createSmartDocument(final DocumentCreationData documentCreationData) {
        final SmartDocument smartDocument = new SmartDocument();
        smartDocument.setSelection(new Selection());
        smartDocument.getSelection().setTemplateGroup(
                ztcClientService.readZaaktype(documentCreationData.getZaak().getZaaktype()).getOmschrijving()
        );
        return smartDocument;
    }

    private Registratie createRegistratie(final DocumentCreationData documentCreationData) {
        final Registratie registratie = new Registratie();
        registratie.bronOrganisatie = BRON_ORGANISATIE;
        registratie.zaak = zrcClientService.createUrlExternToZaak(documentCreationData.getZaak().getUuid());
        registratie.informatieObjectStatus = documentCreationData.getInformatieobjectStatus();
        registratie.informatieObjectType = documentCreationData.getInformatieobjecttype().getUrl();
        registratie.creatieDatum = LocalDate.now();
        registratie.auditToelichting = AUDIT_TOELICHTING;
        return registratie;
    }

    /**
     * Lists all Smart Document templates groups and templates available for the current user.
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
