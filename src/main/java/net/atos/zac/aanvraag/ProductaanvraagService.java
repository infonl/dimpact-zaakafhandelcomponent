/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.aanvraag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import net.atos.client.or.object.ObjectsClientService;
import net.atos.client.or.object.model.ORObject;
import net.atos.client.vrl.VRLClientService;
import net.atos.client.vrl.model.CommunicatieKanaal;
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
import net.atos.zac.aanvraag.model.IndicatieMachtigingJsonAdapter;
import net.atos.zac.aanvraag.model.RolOmschrijvingGeneriekJsonAdapter;
import net.atos.zac.aanvraag.model.generated.Betrokkene;
import net.atos.zac.aanvraag.model.generated.ProductaanvraagDimpact;
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
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.atos.zac.configuratie.ConfiguratieService.BRON_ORGANISATIE;
import static net.atos.zac.configuratie.ConfiguratieService.COMMUNICATIEKANAAL_EFORMULIER;
import static net.atos.zac.util.UriUtil.uuidFromURI;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ApplicationScoped
public class ProductaanvraagService {
    public static final String AANVRAAG_PDF_TITEL = "Aanvraag PDF";
    public static final String AANVRAAG_PDF_BESCHRIJVING = "PDF document met de aanvraag gegevens van de zaak";
    public static final String ZAAK_INFORMATIEOBJECT_REDEN = "Document toegevoegd tijdens het starten van de van de zaak vanuit een product aanvraag";

    private static final Logger LOG = Logger.getLogger(ProductaanvraagService.class.getName());
    private static final String ROL_TOELICHTING = "Overgenomen vanuit de product aanvraag";
    private static final String PRODUCT_AANVRAAG_FORMULIER_DATA_VELD = "aanvraaggegevens";
    private static final String FORMULIER_KLEINE_EVENEMENTEN_MELDING_EIGENSCHAPNAAM_NAAM_EVENEMENT = "naamEvenement";
    private static final String FORMULIER_KLEINE_EVENEMENTEN_MELDING_EIGENSCHAPNAAM_OMSCHRIJVING_EVENEMENT = "omschrijvingEvenement";
    private static final String FORMULIER_VELD_ZAAK_TOELICHTING = "zaak_toelichting";
    private static final String FORMULIER_VELD_ZAAK_OMSCHRIJVING = "zaak_omschrijving";

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

    public void verwerkProductaanvraag(final URI productaanvraagUrl) {
        LOG.fine(() -> "Verwerken productaanvraag: %s".formatted(productaanvraagUrl));

        final var productaanvraagObject = objectsClientService.readObject(uuidFromURI(productaanvraagUrl));
        final var productaanvraag = getProductaanvraag(productaanvraagObject);
        final Optional<UUID> zaaktypeUUID = zaakafhandelParameterBeheerService.findZaaktypeUUIDByProductaanvraagType(
                productaanvraag.getType());
        if (zaaktypeUUID.isPresent()) {
            try {
                LOG.fine(() -> "Start zaak met CMMN case. Zaaktype: %s".formatted(zaaktypeUUID.get().toString()));

                registreerZaakMetCMMNCase(zaaktypeUUID.get(), productaanvraag, productaanvraagObject);
            } catch (RuntimeException ex) {
                warning("CMMN", productaanvraag, ex);
            }
        } else {
            final var zaaktype = findZaaktypeByIdentificatie(productaanvraag.getType());
            if (zaaktype.isPresent()) {
                try {

                    LOG.fine(() -> "Start zaak met BPMN proces. Zaaktype: %s".formatted(zaaktype.get().toString()));

                    registreerZaakMetBPMNProces(zaaktype.get(), productaanvraag, productaanvraagObject);
                } catch (RuntimeException ex) {
                    warning("BPMN", productaanvraag, ex);
                }
            } else {
                LOG.info(message(productaanvraag,
                        "Er is geen zaaktype gevonden voor het type '%s'. Er wordt geen zaak aangemaakt."
                                .formatted(productaanvraag.getType())));
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
        // TODO: enum conversions fail
        // e.g. jakarta.json.bind.JsonbException: Internal error: No enum constant
        // net.atos.zac.aanvraag.model.generated.Betrokkene.IndicatieMachtiging.gemachtigde
        // for now add workaround using custom JSON enum adapters
        return JsonbBuilder.create(
                new JsonbConfig().withAdapters(
                        new IndicatieMachtigingJsonAdapter(),
                        new RolOmschrijvingGeneriekJsonAdapter()
                )
        ).fromJson(
                JsonbUtil.JSONB.toJson(productaanvraagObject.getRecord().getData()),
                ProductaanvraagDimpact.class
        );
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
        productaanvraag.getBetrokkenen().stream()
                .filter(betrokkene -> betrokkene.getRolOmschrijvingGeneriek().equals(Betrokkene.RolOmschrijvingGeneriek.INITIATOR))
                // TODO: check; there can be only one initiator for a particular zaak?
                .forEach(betrokkene -> inboxProductaanvraag.setInitiatorID(betrokkene.getInpBsn()));

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
        var zaak = new Zaak();
        final var zaaktype = ztcClientService.readZaaktype(zaaktypeUuid);
        zaak.setZaaktype(zaaktype.getUrl());
        zaak.setOmschrijving(
                (String) formulierData.get(FORMULIER_KLEINE_EVENEMENTEN_MELDING_EIGENSCHAPNAAM_NAAM_EVENEMENT));
        zaak.setToelichting(
                (String) formulierData.get(FORMULIER_KLEINE_EVENEMENTEN_MELDING_EIGENSCHAPNAAM_OMSCHRIJVING_EVENEMENT));
        zaak.setStartdatum(productaanvraagObject.getRecord().getStartAt());
        zaak.setBronorganisatie(BRON_ORGANISATIE);
        zaak.setVerantwoordelijkeOrganisatie(BRON_ORGANISATIE);
        final Optional<CommunicatieKanaal> communicatiekanaal = vrlClientService.findCommunicatiekanaal(
                COMMUNICATIEKANAAL_EFORMULIER);
        if (communicatiekanaal.isPresent()) {
            zaak.setCommunicatiekanaal(communicatiekanaal.get().getUrl());
        }

        LOG.fine("Creating zaak using the ZGW API: " + zaak);
        zaak = zgwApiService.createZaak(zaak);
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                zaaktypeUuid);
        toekennenZaak(zaak, zaakafhandelParameters);
        pairProductaanvraagInfoWithZaak(productaanvraag, productaanvraagObject, zaak);
        cmmnService.startCase(zaak, zaaktype, zaakafhandelParameters, formulierData);
    }

    private void pairProductaanvraagInfoWithZaak(
            final ProductaanvraagDimpact productaanvraag,
            final ORObject productaanvraagObject,
            final Zaak zaak
    ) {
        pairProductaanvraagWithZaak(productaanvraagObject, zaak.getUrl());
        pairAanvraagPDFWithZaak(productaanvraag, zaak.getUrl());
        pairBijlagenWithZaak(productaanvraag.getBijlagen(), zaak.getUrl());
        productaanvraag.getBetrokkenen().stream()
                .filter(betrokkene -> betrokkene.getRolOmschrijvingGeneriek().equals(Betrokkene.RolOmschrijvingGeneriek.INITIATOR))
                // TODO: check; there can be only one initiator for a particular zaak..
                .forEach(betrokkene -> addInitiator(betrokkene.getInpBsn(), zaak.getUrl(), zaak.getZaaktype()));
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
                .filter(zaak -> zaak.getIdentificatie().equals(zaaktypeIdentificatie))
                .findFirst();
    }
}
