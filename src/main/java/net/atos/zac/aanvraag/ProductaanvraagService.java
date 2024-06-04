/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.aanvraag;

import static net.atos.zac.configuratie.ConfiguratieService.BRON_ORGANISATIE;
import static net.atos.zac.configuratie.ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER;
import static net.atos.zac.util.UriUtil.uuidFromURI;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import net.atos.client.or.object.ObjectsClientService;
import net.atos.client.or.object.model.ORObject;
import net.atos.client.vrl.VRLClientService;
import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Medewerker;
import net.atos.client.zgw.zrc.model.NatuurlijkPersoon;
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid;
import net.atos.client.zgw.zrc.model.RolMedewerker;
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon;
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.aanvraag.model.InboxProductaanvraag;
import net.atos.zac.aanvraag.model.generated.Betrokkene;
import net.atos.zac.aanvraag.model.generated.Geometry;
import net.atos.zac.aanvraag.model.generated.ProductaanvraagDimpact;
import net.atos.zac.aanvraag.util.BetalingStatusEnumJsonAdapter;
import net.atos.zac.aanvraag.util.GeometryTypeEnumJsonAdapter;
import net.atos.zac.aanvraag.util.IndicatieMachtigingEnumJsonAdapter;
import net.atos.zac.aanvraag.util.RolOmschrijvingGeneriekEnumJsonAdapter;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.documenten.InboxDocumentenService;
import net.atos.zac.flowable.BPMNService;
import net.atos.zac.flowable.CMMNService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;
import net.atos.zac.util.JsonbUtil;
import net.atos.zac.zaaksturing.ZaakafhandelParameterBeheerService;
import net.atos.zac.zaaksturing.ZaakafhandelParameterService;
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters;

@ApplicationScoped
public class ProductaanvraagService {
    public static final String AANVRAAG_PDF_TITEL = "Aanvraag PDF";
    public static final String AANVRAAG_PDF_BESCHRIJVING = "PDF document met de aanvraag gegevens van de zaak";
    public static final String ZAAK_INFORMATIEOBJECT_REDEN = "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag";

    private static final Logger LOG = Logger.getLogger(ProductaanvraagService.class.getName());
    private static final String ROL_TOELICHTING = "Overgenomen vanuit de product aanvraag";
    private static final String PRODUCT_AANVRAAG_FORMULIER_DATA_VELD = "aanvraaggegevens";
    private static final String FORMULIER_VELD_ZAAK_TOELICHTING = "zaak_toelichting";
    private static final String FORMULIER_VELD_ZAAK_OMSCHRIJVING = "zaak_omschrijving";
    private static final String ZAAK_DESCRIPTION_FORMAT = "Aangemaakt vanuit %s met kenmerk '%s'";

    /**
     * Maximum length of the description field in a zaak as defined by the ZGW ZRC API.
     */
    private static final int ZAAK_DESCRIPTION_MAX_LENGTH = 80;

    private ObjectsClientService objectsClientService;
    private ZGWApiService zgwApiService;
    private ZRCClientService zrcClientService;
    private DRCClientService drcClientService;
    private ZTCClientService ztcClientService;
    private VRLClientService vrlClientService;
    private IdentityService identityService;
    private ZaakafhandelParameterService zaakafhandelParameterService;
    private ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService;
    private InboxDocumentenService inboxDocumentenService;
    private InboxProductaanvraagService inboxProductaanvraagService;
    private CMMNService cmmnService;
    private BPMNService bpmnService;
    private ConfiguratieService configuratieService;

    /**
     * Empty no-op constructor as required by Weld.
     */
    public ProductaanvraagService() {
    }

    @Inject
    public ProductaanvraagService(
            ObjectsClientService objectsClientService,
            ZGWApiService zgwApiService,
            ZRCClientService zrcClientService,
            DRCClientService drcClientService,
            ZTCClientService ztcClientService,
            VRLClientService vrlClientService,
            IdentityService identityService,
            ZaakafhandelParameterService zaakafhandelParameterService,
            ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService,
            InboxDocumentenService inboxDocumentenService,
            InboxProductaanvraagService inboxProductaanvraagService,
            CMMNService cmmnService,
            BPMNService bpmnService,
            ConfiguratieService configuratieService
    ) {
        this.objectsClientService = objectsClientService;
        this.zgwApiService = zgwApiService;
        this.zrcClientService = zrcClientService;
        this.drcClientService = drcClientService;
        this.ztcClientService = ztcClientService;
        this.vrlClientService = vrlClientService;
        this.identityService = identityService;
        this.zaakafhandelParameterService = zaakafhandelParameterService;
        this.zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService;
        this.inboxDocumentenService = inboxDocumentenService;
        this.inboxProductaanvraagService = inboxProductaanvraagService;
        this.cmmnService = cmmnService;
        this.bpmnService = bpmnService;
        this.configuratieService = configuratieService;
    }

    public void handleProductaanvraag(final UUID productaanvraagObjectUUID) {
        try {
            final var productaanvraagObject = objectsClientService.readObject(productaanvraagObjectUUID);
            if (isProductaanvraagDimpact(productaanvraagObject)) {
                LOG.info(() -> "Handle productaanvraag-Dimpact object UUID: %s".formatted(productaanvraagObjectUUID));
                verwerkProductaanvraag(productaanvraagObject);
            }
        } catch (RuntimeException ex) {
            LOG.log(
                    Level.WARNING,
                    "Failed to handle productaanvraag-Dimpact object UUID: %s"
                            .formatted(productaanvraagObjectUUID),
                    ex
            );
        }
    }

    private boolean isProductaanvraagDimpact(final ORObject productaanvraagObject) {
        // check if the required attributes defined by the 'Productaanvraag Dimpact' JSON schema are present
        // this is a bit of a poor man's solution because we are currently 'misusing' the very generic Objects API
        // to store specific productaanvraag JSON data
        final var productAanvraagData = productaanvraagObject.getRecord().getData();
        return productAanvraagData.containsKey("bron") &&
               productAanvraagData.containsKey("type") &&
               productAanvraagData.containsKey("aanvraaggegevens");
    }

    private void verwerkProductaanvraag(final ORObject productaanvraagObject) {
        LOG.fine(() -> "Start handling productaanvraag with object URL: %s".formatted(productaanvraagObject.getUrl()));
        final var productaanvraag = getProductaanvraag(productaanvraagObject);
        final Optional<UUID> zaaktypeUUID = zaakafhandelParameterBeheerService.findZaaktypeUUIDByProductaanvraagType(
                productaanvraag.getType()
        );
        if (zaaktypeUUID.isPresent()) {
            try {
                LOG.fine(() -> "Creating a zaak using a CMMN case. Zaaktype: %s".formatted(zaaktypeUUID.get().toString()));
                registreerZaakMetCMMNCase(zaaktypeUUID.get(), productaanvraag, productaanvraagObject);
            } catch (RuntimeException ex) {
                warning("CMMN", productaanvraag, ex);
            }
        } else {
            final var zaaktype = findZaaktypeByIdentificatie(productaanvraag.getType());
            if (zaaktype.isPresent()) {
                try {
                    LOG.fine(() -> "Creating a zaak using a BPMN proces. Zaaktype: %s".formatted(zaaktype.get().toString()));
                    registreerZaakMetBPMNProces(zaaktype.get(), productaanvraag, productaanvraagObject);
                } catch (RuntimeException ex) {
                    warning("BPMN", productaanvraag, ex);
                }
            } else {
                LOG.info(
                        message(
                                productaanvraag,
                                "No zaaktype found for productaanvraag-Dimpact type '%s'. No zaak was created."
                                        .formatted(productaanvraag.getType())
                        )
                );
                registreerInbox(productaanvraag, productaanvraagObject);
            }
        }
    }

    private void warning(final String type, final ProductaanvraagDimpact productaanvraag, final RuntimeException ex) {
        LOG.log(Level.WARNING,
                message(productaanvraag, "Er is iets fout gegaan bij het aanmaken van een %s-zaak."
                        .formatted(type)), ex);
    }

    private String message(final ProductaanvraagDimpact productaanvraag, final String message) {
        return "Productaanvraag %s: %s".formatted(
                productaanvraag.getAanvraaggegevens(),
                message
        );
    }

    private void registreerZaakMetBPMNProces(
            final ZaakType zaaktype,
            final ProductaanvraagDimpact productaanvraag,
            final ORObject productaanvraagObject
    ) {
        final Map<String, Object> formulierData = getFormulierData(productaanvraagObject);
        var zaak = new Zaak();
        zaak.setZaaktype(zaaktype.getUrl());
        zaak.setBronorganisatie(BRON_ORGANISATIE);
        zaak.setVerantwoordelijkeOrganisatie(BRON_ORGANISATIE);
        zaak.setStartdatum(LocalDate.now());
        final var omschrijving = (String) formulierData.get(FORMULIER_VELD_ZAAK_OMSCHRIJVING);
        if (isNotBlank(omschrijving)) {
            zaak.setOmschrijving(omschrijving);
        }
        final var toelichting = (String) formulierData.get(FORMULIER_VELD_ZAAK_TOELICHTING);
        if (StringUtils.isNotBlank(toelichting)) {
            zaak.setToelichting(toelichting);
        }
        zaak = zgwApiService.createZaak(zaak);
        final var PROCESS_DEFINITION_KEY = "test-met-proces";
        final var processDefinition = bpmnService.readProcessDefinitionByprocessDefinitionKey(PROCESS_DEFINITION_KEY);
        zrcClientService.createRol(creeerRolGroep(processDefinition.getDescription(), zaak));
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, zaak);
        bpmnService.startProcess(zaak, zaaktype, formulierData, PROCESS_DEFINITION_KEY);
    }

    public Map<String, Object> getFormulierData(final ORObject productaanvraagObject) {
        final Map<String, Object> formulierData = new HashMap<>();
        ((Map<String, Object>) productaanvraagObject.getRecord().getData().get(PRODUCT_AANVRAAG_FORMULIER_DATA_VELD))
                .forEach((stap, velden) -> formulierData.putAll((Map<String, Object>) velden));
        return formulierData;
    }

    public ProductaanvraagDimpact getProductaanvraag(final ORObject productaanvraagObject) {
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                // register our custom enum JSON adapters because by default enums are deserialized using the enum's name
                // instead of the value and this fails because in the generated model classes the enum names are
                // capitalized and the values are not
                .withAdapters(
                        new IndicatieMachtigingEnumJsonAdapter(),
                        new RolOmschrijvingGeneriekEnumJsonAdapter(),
                        new BetalingStatusEnumJsonAdapter(),
                        new GeometryTypeEnumJsonAdapter()
                ))
        ) {
            return jsonb.fromJson(
                    JsonbUtil.JSONB.toJson(productaanvraagObject.getRecord().getData()),
                    ProductaanvraagDimpact.class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addInitiator(final String bsn, final URI zaak, final URI zaaktype) {
        final RolType initiator = ztcClientService.readRoltype(RolType.OmschrijvingGeneriekEnum.INITIATOR, zaaktype);
        final RolNatuurlijkPersoon rolNatuurlijkPersoon = new RolNatuurlijkPersoon(
                zaak,
                initiator,
                ROL_TOELICHTING,
                new NatuurlijkPersoon(bsn)
        );
        zrcClientService.createRol(rolNatuurlijkPersoon);
    }

    private void registreerInbox(final ProductaanvraagDimpact productaanvraag, final ORObject productaanvraagObject) {
        final InboxProductaanvraag inboxProductaanvraag = new InboxProductaanvraag();
        inboxProductaanvraag.setProductaanvraagObjectUUID(productaanvraagObject.getUuid());
        inboxProductaanvraag.setType(productaanvraag.getType());
        inboxProductaanvraag.setOntvangstdatum(productaanvraagObject.getRecord().getRegistrationAt());
        ListUtils.emptyIfNull(productaanvraag.getBetrokkenen()).stream()
                .filter(betrokkene -> betrokkene.getRolOmschrijvingGeneriek().equals(Betrokkene.RolOmschrijvingGeneriek.INITIATOR))
                // there can be at most only one initiator for a particular zaak so even if there are multiple (theorically possible)
                // we are only interested in the first one
                .findFirst()
                .ifPresent(betrokkene -> inboxProductaanvraag.setInitiatorID(betrokkene.getInpBsn()));

        if (productaanvraag.getPdf() != null) {
            final UUID aanvraagDocumentUUID = uuidFromURI(productaanvraag.getPdf());
            inboxProductaanvraag.setAanvraagdocumentUUID(aanvraagDocumentUUID);
            deleteInboxDocument(aanvraagDocumentUUID);
        }
        final List<URI> bijlagen = ListUtils.emptyIfNull(productaanvraag.getBijlagen());
        inboxProductaanvraag.setAantalBijlagen(bijlagen.size());
        bijlagen.forEach(bijlage -> deleteInboxDocument(uuidFromURI(bijlage)));
        inboxProductaanvraagService.create(inboxProductaanvraag);
    }

    private void deleteInboxDocument(final UUID documentUUID) {
        inboxDocumentenService.find(documentUUID)
                .ifPresent(inboxDocument -> inboxDocumentenService.delete(inboxDocument.getId()));
    }

    private void registreerZaakMetCMMNCase(
            final UUID zaaktypeUuid,
            final ProductaanvraagDimpact productaanvraag,
            final ORObject productaanvraagObject
    ) {
        final var formulierData = getFormulierData(productaanvraagObject);
        final var zaak = new Zaak();
        final var zaaktype = ztcClientService.readZaaktype(zaaktypeUuid);
        zaak.setZaaktype(zaaktype.getUrl());
        var zaakOmschrijving = getZaakOmschrijving(productaanvraag);
        zaak.setOmschrijving(zaakOmschrijving);
        // note that we leave the 'toelichting' field empty for a zaak created from a productaanvraag
        zaak.setStartdatum(productaanvraagObject.getRecord().getStartAt());
        zaak.setBronorganisatie(BRON_ORGANISATIE);
        zaak.setVerantwoordelijkeOrganisatie(BRON_ORGANISATIE);
        vrlClientService.findCommunicatiekanaal(COMMUNICATIEKANAAL_EFORMULIER).ifPresent(
                communicatieKanaal -> zaak.setCommunicatiekanaal(communicatieKanaal.getUrl())
        );
        final var zaakgegevens = productaanvraag.getZaakgegevens();
        if (
            zaakgegevens != null &&
            zaakgegevens.getGeometry() != null &&
            zaakgegevens.getGeometry().getType() == Geometry.Type.POINT
        ) {
            zaak.setZaakgeometrie(AanvraagToZgwConverterKt.convertToZgwPoint(zaakgegevens.getGeometry()));
        }

        LOG.fine("Creating zaak using the ZGW API: " + zaak);
        final var createdZaak = zgwApiService.createZaak(zaak);
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUuid);
        toekennenZaak(createdZaak, zaakafhandelParameters);
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, createdZaak);
        cmmnService.startCase(createdZaak, zaaktype, zaakafhandelParameters, formulierData);
    }

    private void pairProductaanvraagInfoWithZaak(
            final ProductaanvraagDimpact productaanvraag,
            final ORObject productaanvraagObject,
            final Zaak zaak
    ) {
        pairProductaanvraagWithZaak(productaanvraagObject, zaak.getUrl());
        pairAanvraagPDFWithZaak(productaanvraag, zaak.getUrl());
        pairBijlagenWithZaak(productaanvraag.getBijlagen(), zaak.getUrl());
        ListUtils.emptyIfNull(productaanvraag.getBetrokkenen()).stream()
                .filter(betrokkene -> betrokkene.getRolOmschrijvingGeneriek().equals(Betrokkene.RolOmschrijvingGeneriek.INITIATOR))
                // there can be at most only one initiator for a particular zaak so even if there are multiple (theorically possible)
                // we are only interested in the first one
                .findFirst()
                .ifPresent(betrokkene -> addInitiator(betrokkene.getInpBsn(), zaak.getUrl(), zaak.getZaaktype()));
    }

    public void pairProductaanvraagWithZaak(final ORObject productaanvraag, final URI zaakUrl) {
        final ZaakobjectProductaanvraag zaakobject = new ZaakobjectProductaanvraag(zaakUrl, productaanvraag.getUrl());
        zrcClientService.createZaakobject(zaakobject);
    }

    public void pairAanvraagPDFWithZaak(final ProductaanvraagDimpact productaanvraag, final URI zaakUrl) {
        final ZaakInformatieobject zaakInformatieobject = new ZaakInformatieobject();
        zaakInformatieobject.setInformatieobject(productaanvraag.getPdf());
        zaakInformatieobject.setZaak(zaakUrl);
        zaakInformatieobject.setTitel(AANVRAAG_PDF_TITEL);
        zaakInformatieobject.setBeschrijving(AANVRAAG_PDF_BESCHRIJVING);

        LOG.fine("Creating zaak informatieobject: " + zaakInformatieobject);
        zrcClientService.createZaakInformatieobject(zaakInformatieobject, ZAAK_INFORMATIEOBJECT_REDEN);
    }

    public void pairBijlagenWithZaak(final List<URI> bijlageURIs, final URI zaakUrl) {
        for (final URI bijlageURI : ListUtils.emptyIfNull(bijlageURIs)) {
            final EnkelvoudigInformatieObject bijlage = drcClientService.readEnkelvoudigInformatieobject(bijlageURI);
            final ZaakInformatieobject zaakInformatieobject = new ZaakInformatieobject();
            zaakInformatieobject.setInformatieobject(bijlage.getUrl());
            zaakInformatieobject.setZaak(zaakUrl);
            zaakInformatieobject.setTitel(bijlage.getTitel());
            zaakInformatieobject.setBeschrijving(bijlage.getBeschrijving());
            zrcClientService.createZaakInformatieobject(zaakInformatieobject, ZAAK_INFORMATIEOBJECT_REDEN);
        }
    }

    private void toekennenZaak(final Zaak zaak, final ZaakafhandelParameters zaakafhandelParameters) {
        if (zaakafhandelParameters.getGroepID() != null) {
            LOG.info(String.format("Zaak %s: toegekend aan groep '%s'", zaak.getUuid(),
                    zaakafhandelParameters.getGroepID()));
            zrcClientService.createRol(creeerRolGroep(zaakafhandelParameters.getGroepID(), zaak));
        }
        if (zaakafhandelParameters.getGebruikersnaamMedewerker() != null) {
            LOG.info(String.format("Zaak %s: toegekend aan behandelaar '%s'", zaak.getUuid(),
                    zaakafhandelParameters.getGebruikersnaamMedewerker()));
            zrcClientService.createRol(creeerRolMedewerker(zaakafhandelParameters.getGebruikersnaamMedewerker(), zaak));
        }
    }

    private RolOrganisatorischeEenheid creeerRolGroep(final String groepID, final Zaak zaak) {
        final Group group = identityService.readGroup(groepID);
        final OrganisatorischeEenheid groep = new OrganisatorischeEenheid();
        groep.setIdentificatie(group.getId());
        groep.setNaam(group.getName());
        final RolType roltype = ztcClientService.readRoltype(RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.getZaaktype());
        return new RolOrganisatorischeEenheid(zaak.getUrl(), roltype, "Behandelend groep van de zaak", groep);
    }

    private RolMedewerker creeerRolMedewerker(final String behandelaarGebruikersnaam, final Zaak zaak) {
        final User user = identityService.readUser(behandelaarGebruikersnaam);
        final Medewerker medewerker = new Medewerker();
        medewerker.setIdentificatie(user.getId());
        medewerker.setVoorletters(user.getFirstName());
        medewerker.setAchternaam(user.getLastName());
        final RolType roltype = ztcClientService.readRoltype(RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.getZaaktype());
        return new RolMedewerker(zaak.getUrl(), roltype, "Behandelaar van de zaak", medewerker);
    }

    private Optional<ZaakType> findZaaktypeByIdentificatie(final String zaaktypeIdentificatie) {
        return ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI()).stream()
                .filter(zaakType -> zaakType.getIdentificatie().equals(zaaktypeIdentificatie))
                .findFirst();
    }

    private static String getZaakOmschrijving(ProductaanvraagDimpact productaanvraag) {
        final var zaakOmschrijving = String.format(
                ZAAK_DESCRIPTION_FORMAT,
                productaanvraag.getBron().getNaam(),
                productaanvraag.getBron().getKenmerk()
        );
        if (zaakOmschrijving.length() > ZAAK_DESCRIPTION_MAX_LENGTH) {
            // we truncate the zaak description to the maximum length allowed by the ZGW ZRC API
            // or else it will not be accepted by the ZGW API implementation component
            LOG.warning(
                    String.format(
                            "Truncating zaak description '%s' to the maximum length allowed by the ZGW ZRC API",
                            zaakOmschrijving
                    )
            );
            return zaakOmschrijving.substring(0, ZAAK_DESCRIPTION_MAX_LENGTH);
        } else {
            return zaakOmschrijving;
        }
    }
}
