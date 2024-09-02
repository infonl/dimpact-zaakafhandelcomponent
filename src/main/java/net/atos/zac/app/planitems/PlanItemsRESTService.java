/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.planitems;

import static net.atos.zac.flowable.task.TaakVariabelenService.isZaakOpschorten;
import static net.atos.zac.flowable.task.TaakVariabelenService.readMailAttachments;
import static net.atos.zac.flowable.task.TaakVariabelenService.readMailBody;
import static net.atos.zac.flowable.task.TaakVariabelenService.readMailFrom;
import static net.atos.zac.flowable.task.TaakVariabelenService.readMailReplyTo;
import static net.atos.zac.flowable.task.TaakVariabelenService.readMailTo;
import static net.atos.zac.flowable.task.TaakVariabelenService.setMailBody;
import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.flowable.cmmn.api.runtime.PlanItemInstance;

import net.atos.client.zgw.brc.BrcClientService;
import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.generated.Resultaat;
import net.atos.zac.admin.ZaakafhandelParameterService;
import net.atos.zac.admin.model.HumanTaskParameters;
import net.atos.zac.admin.model.MailtemplateKoppeling;
import net.atos.zac.admin.model.ZaakafhandelParameters;
import net.atos.zac.app.exception.InputValidationFailedException;
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter;
import net.atos.zac.app.planitems.converter.RESTPlanItemConverter;
import net.atos.zac.app.planitems.model.RESTHumanTaskData;
import net.atos.zac.app.planitems.model.RESTPlanItem;
import net.atos.zac.app.planitems.model.RESTProcessTaskData;
import net.atos.zac.app.planitems.model.RESTUserEventListenerData;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.flowable.ZaakVariabelenService;
import net.atos.zac.flowable.cmmn.CMMNService;
import net.atos.zac.mail.MailService;
import net.atos.zac.mail.model.BronnenKt;
import net.atos.zac.mail.model.MailAdres;
import net.atos.zac.mailtemplates.MailTemplateService;
import net.atos.zac.mailtemplates.model.Mail;
import net.atos.zac.mailtemplates.model.MailGegevens;
import net.atos.zac.mailtemplates.model.MailTemplate;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.shared.helper.OpschortenZaakHelper;
import net.atos.zac.util.DateTimeConverterUtil;
import net.atos.zac.util.UriUtil;
import net.atos.zac.zoeken.IndexeerService;

/**
 * Provides REST endpoints for CMMN plan items.
 */
@Singleton
@Path("planitems")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlanItemsRESTService {
    private static final String REDEN_OPSCHORTING = "Aanvullende informatie opgevraagd";

    private ZaakVariabelenService zaakVariabelenService;
    private CMMNService cmmnService;
    private ZrcClientService zrcClientService;
    private BrcClientService brcClientService;
    private ZaakafhandelParameterService zaakafhandelParameterService;
    private RESTPlanItemConverter planItemConverter;
    private ZGWApiService zgwApiService;
    private IndexeerService indexeerService;
    private MailService mailService;
    private ConfiguratieService configuratieService;
    private MailTemplateService mailTemplateService;
    private PolicyService policyService;
    private OpschortenZaakHelper opschortenZaakHelper;
    private RESTMailGegevensConverter restMailGegevensConverter;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public PlanItemsRESTService() {
    }

    @Inject
    public PlanItemsRESTService(
            ZaakVariabelenService zaakVariabelenService,
            CMMNService cmmnService,
            ZrcClientService zrcClientService,
            BrcClientService brcClientService,
            ZaakafhandelParameterService zaakafhandelParameterService,
            RESTPlanItemConverter planItemConverter,
            ZGWApiService zgwApiService,
            IndexeerService indexeerService,
            MailService mailService,
            ConfiguratieService configuratieService,
            MailTemplateService mailTemplateService,
            PolicyService policyService,
            OpschortenZaakHelper opschortenZaakHelper,
            RESTMailGegevensConverter restMailGegevensConverter
    ) {
        this.zaakVariabelenService = zaakVariabelenService;
        this.cmmnService = cmmnService;
        this.zrcClientService = zrcClientService;
        this.brcClientService = brcClientService;
        this.zaakafhandelParameterService = zaakafhandelParameterService;
        this.planItemConverter = planItemConverter;
        this.zgwApiService = zgwApiService;
        this.indexeerService = indexeerService;
        this.mailService = mailService;
        this.configuratieService = configuratieService;
        this.mailTemplateService = mailTemplateService;
        this.policyService = policyService;
        this.opschortenZaakHelper = opschortenZaakHelper;
        this.restMailGegevensConverter = restMailGegevensConverter;
    }

    @GET
    @Path("zaak/{uuid}/humanTaskPlanItems")
    public List<RESTPlanItem> listHumanTaskPlanItems(@PathParam("uuid") final UUID zaakUUID) {
        final List<PlanItemInstance> humanTaskPlanItems = cmmnService.listHumanTaskPlanItems(zaakUUID);
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        return planItemConverter.convertPlanItems(humanTaskPlanItems, zaak).stream()
                .filter(restPlanItem -> restPlanItem.actief)
                .toList();
    }

    @GET
    @Path("zaak/{uuid}/processTaskPlanItems")
    public List<RESTPlanItem> listProcessTaskPlanItems(@PathParam("uuid") final UUID zaakUUID) {
        final var processTaskPlanItems = cmmnService.listProcessTaskPlanItems(zaakUUID);
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        return planItemConverter.convertPlanItems(processTaskPlanItems, zaak);
    }

    @GET
    @Path("zaak/{uuid}/userEventListenerPlanItems")
    public List<RESTPlanItem> listUserEventListenerPlanItems(@PathParam("uuid") final UUID zaakUUID) {
        final List<PlanItemInstance> userEventListenerPlanItems = cmmnService.listUserEventListenerPlanItems(zaakUUID);
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        return planItemConverter.convertPlanItems(userEventListenerPlanItems, zaak);
    }

    @GET
    @Path("humanTaskPlanItem/{id}")
    public RESTPlanItem readHumanTaskPlanItem(@PathParam("id") final String planItemId) {
        final PlanItemInstance humanTaskPlanItem = cmmnService.readOpenPlanItem(planItemId);
        final UUID zaakUUID = zaakVariabelenService.readZaakUUID(humanTaskPlanItem);
        final UUID zaaktypeUUID = zaakVariabelenService.readZaaktypeUUID(humanTaskPlanItem);
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                zaaktypeUUID);
        return planItemConverter.convertPlanItem(humanTaskPlanItem, zaakUUID, zaakafhandelParameters);
    }

    @GET
    @Path("processTaskPlanItem/{id}")
    public RESTPlanItem readProcessTaskPlanItem(@PathParam("id") final String planItemId) {
        final PlanItemInstance processTaskPlanItem = cmmnService.readOpenPlanItem(planItemId);
        final UUID zaakUUID = zaakVariabelenService.readZaakUUID(processTaskPlanItem);
        final UUID zaaktypeUUID = zaakVariabelenService.readZaaktypeUUID(processTaskPlanItem);
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                zaaktypeUUID);
        return planItemConverter.convertPlanItem(processTaskPlanItem, zaakUUID, zaakafhandelParameters);
    }

    @POST
    @Path("doHumanTaskPlanItem")
    public void doHumanTaskplanItem(@Valid final RESTHumanTaskData humanTaskData) {
        final PlanItemInstance planItem = cmmnService.readOpenPlanItem(humanTaskData.planItemInstanceId);
        final UUID zaakUUID = zaakVariabelenService.readZaakUUID(planItem);
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        final Map<String, String> taakdata = humanTaskData.taakdata;
        assertPolicy(policyService.readZaakRechten(zaak).startenTaak());
        final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                UriUtil.uuidFromURI(zaak.getZaaktype())
        );

        final LocalDate fatalDate = calculateFatalDate(humanTaskData, zaakafhandelParameters, planItem, zaak);
        if (fatalDate != null && isZaakOpschorten(taakdata)) {
            final long numberOfDays = ChronoUnit.DAYS.between(LocalDate.now(), fatalDate);
            opschortenZaakHelper.opschortenZaak(zaak, numberOfDays, REDEN_OPSCHORTING);
        }

        if (humanTaskData.taakStuurGegevens.sendMail) {
            final Mail mail = Mail.valueOf(humanTaskData.taakStuurGegevens.mail);

            final MailTemplate mailTemplate = zaakafhandelParameters.getMailtemplateKoppelingen().stream()
                    .map(MailtemplateKoppeling::getMailTemplate)
                    .filter(template -> template.getMail().equals(mail))
                    .findFirst()
                    .orElseGet(() -> mailTemplateService.readMailtemplate(mail));

            final String afzender = configuratieService.readGemeenteNaam();
            setMailBody(taakdata, mailService.sendMail(
                    new MailGegevens(
                            readMailFrom(taakdata)
                                    .map(email -> new MailAdres(email, afzender))
                                    .orElseGet(() -> mailService.getGemeenteMailAdres()),
                            readMailTo(taakdata)
                                    .map(MailAdres::new)
                                    .orElse(null),
                            readMailReplyTo(taakdata)
                                    .map(email -> new MailAdres(email, afzender))
                                    .orElse(null),
                            mailTemplate.getOnderwerp(),
                            readMailBody(taakdata).orElse(null),
                            readMailAttachments(taakdata).orElse(null),
                            true),
                    BronnenKt.getBronnenFromZaak(zaak)));
        }
        cmmnService.startHumanTaskPlanItem(
                humanTaskData.planItemInstanceId,
                humanTaskData.groep.getId(),
                humanTaskData.medewerker != null && !humanTaskData.medewerker.toString().isEmpty() ?
                        humanTaskData.medewerker.getId() :
                        null,
                DateTimeConverterUtil.convertToDate(fatalDate),
                humanTaskData.toelichting,
                taakdata,
                zaakUUID
        );
        indexeerService.addOrUpdateZaak(zaakUUID, false);
    }

    @POST
    @Path("doProcessTaskPlanItem")
    public void doProcessTaskplanItem(final RESTProcessTaskData processTaskData) {
        cmmnService.startProcessTaskPlanItem(processTaskData.planItemInstanceId, processTaskData.data);
    }

    @POST
    @Path("doUserEventListenerPlanItem")
    public void doUserEventListenerPlanItem(final RESTUserEventListenerData userEventListenerData) {
        final Zaak zaak = zrcClientService.readZaak(userEventListenerData.zaakUuid);
        final var zaakRechten = policyService.readZaakRechten(zaak);
        assertPolicy(zaakRechten.startenTaak());
        if (userEventListenerData.restMailGegevens != null) {
            assertPolicy(zaakRechten.versturenEmail());
        }
        switch (userEventListenerData.actie) {
            case INTAKE_AFRONDEN -> {
                final PlanItemInstance planItemInstance = cmmnService.readOpenPlanItem(
                        userEventListenerData.planItemInstanceId
                );
                zaakVariabelenService.setOntvankelijk(planItemInstance, userEventListenerData.zaakOntvankelijk);
                if (!userEventListenerData.zaakOntvankelijk) {
                    policyService.checkZaakAfsluitbaar(zaak);
                    final ZaakafhandelParameters zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                            UriUtil.uuidFromURI(zaak.getZaaktype())
                    );
                    zgwApiService.createResultaatForZaak(
                            zaak,
                            zaakafhandelParameters.getNietOntvankelijkResultaattype(),
                            userEventListenerData.resultaatToelichting
                    );
                }
            }
            case ZAAK_AFHANDELEN -> {
                policyService.checkZaakAfsluitbaar(zaak);
                if (!brcClientService.listBesluiten(zaak).isEmpty()) {
                    final Resultaat resultaat = zrcClientService.readResultaat(zaak.getResultaat());
                    resultaat.setToelichting(userEventListenerData.resultaatToelichting);
                    zrcClientService.updateResultaat(resultaat);
                } else {
                    zgwApiService.createResultaatForZaak(
                            zaak,
                            userEventListenerData.resultaattypeUuid,
                            userEventListenerData.resultaatToelichting
                    );
                }
            }
        }
        cmmnService.startUserEventListenerPlanItem(userEventListenerData.planItemInstanceId);
        if (userEventListenerData.restMailGegevens != null) {
            mailService.sendMail(
                    restMailGegevensConverter.convert(userEventListenerData.restMailGegevens),
                    BronnenKt.getBronnenFromZaak(zaak)
            );
        }
    }

    private LocalDate calculateFatalDate(
            RESTHumanTaskData humanTaskData,
            ZaakafhandelParameters zaakafhandelParameters,
            PlanItemInstance planItem,
            Zaak zaak
    ) {
        final Optional<HumanTaskParameters> humanTaskParameters = zaakafhandelParameters.findHumanTaskParameter(planItem
                .getPlanItemDefinitionId());
        final LocalDate zaakFatalDate = zaak.getUiterlijkeEinddatumAfdoening();

        if (humanTaskData.fataledatum != null) {
            validateFatalDate(humanTaskData.fataledatum, zaakFatalDate);
            return humanTaskData.fataledatum;
        } else {
            if (humanTaskParameters.isPresent() && humanTaskParameters.get().getDoorlooptijd() != null) {
                LocalDate calculatedFinalDate = LocalDate.now().plusDays(humanTaskParameters.get().getDoorlooptijd());
                if (calculatedFinalDate.isAfter(zaakFatalDate)) {
                    calculatedFinalDate = zaakFatalDate;
                }
                return calculatedFinalDate;
            }
        }

        return null;
    }

    private static void validateFatalDate(LocalDate taskFatalDate, LocalDate zaakFatalDate) {
        if (taskFatalDate.isAfter(zaakFatalDate)) {
            throw new InputValidationFailedException(
                    String.format(
                            "Fatal date of a task (%s) cannot be later than the fatal date of the zaak (%s)",
                            taskFatalDate,
                            zaakFatalDate
                    )
            );
        }
    }
}
