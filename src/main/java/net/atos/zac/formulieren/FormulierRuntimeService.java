package net.atos.zac.formulieren;

import jakarta.inject.Inject;
import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.admin.ReferenceTableService;
import net.atos.zac.admin.model.ReferenceTableValue;
import net.atos.zac.app.formulieren.model.FormulierData;
import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie;
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService;
import net.atos.zac.app.task.model.RestTask;
import net.atos.zac.flowable.FlowableTaskService;
import net.atos.zac.flowable.TaakVariabelenService;
import net.atos.zac.flowable.ZaakVariabelenService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.shared.helper.OpschortenZaakHelper;
import net.atos.zac.util.DateTimeConverterUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.task.api.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;
import static net.atos.zac.util.DateTimeConverterUtil.convertToLocalDate;
import static net.atos.zac.util.UriUtil.uuidFromURI;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.*;

public class FormulierRuntimeService {

    private static final DateTimeFormatter DATUM_FORMAAT = ofPattern("dd-MM-yyy");

    private static final String REDEN_ZAAK_HERVATTEN = "Zaak hervat vanuit proces";

    @Inject
    private ZGWApiService zgwApiService;

    @Inject
    private ZrcClientService zrcClientService;

    @Inject
    private ZaakVariabelenService zaakVariabelenService;

    @Inject
    private IdentityService identityService;

    @Inject
    private ReferenceTableService referenceTableService;

    @Inject
    private OpschortenZaakHelper opschortenZaakHelper;

    @Inject
    private DrcClientService drcClientService;

    @Inject
    private EnkelvoudigInformatieObjectUpdateService enkelvoudigInformatieObjectUpdateService;

    @Inject
    private TaakVariabelenService taakVariabelenService;

    @Inject
    private FlowableTaskService flowableTaskService;

    private static final String REFERENCE_TABLE_SEPARATOR = ";";

    private static final String DOCUMENT_SEPARATOR = ";";

    public void render(final RestTask restTask) {
        final var zaak = zrcClientService.readZaak(restTask.getZaakUuid());
        final var zaakData = zaakVariabelenService.readProcessZaakdata(zaak.getUuid());
        restTask.getFormulierDefinitie().veldDefinities.forEach(veldDefenitie -> {
            resolveDefaultWaarde(veldDefenitie, zaak, restTask, zaakData);
            resolveMeerkeuzeOpties(veldDefenitie);
        });
    }

    public Task submit(final RestTask restTask, Task task, final Zaak zaak) {
        final FormulierData formulierData = new FormulierData(restTask.getTaakdata());
        if (formulierData.toelichting != null || formulierData.taakFataleDatum != null) {
            if (formulierData.toelichting != null) {
                task.setDescription(formulierData.toelichting);
            }
            if (formulierData.taakFataleDatum != null) {
                task.setDueDate(DateTimeConverterUtil.convertToDate(formulierData.taakFataleDatum));
            }
            task = flowableTaskService.updateTask(task);
        }
        if (formulierData.zaakOpschorten && !zaak.isOpgeschort()) {
            opschortenZaakHelper.opschortenZaak(zaak, DAYS.between(LocalDate.now(),
                    convertToLocalDate(task.getDueDate())),
                    restTask.getFormulierDefinitie().naam);
        }
        if (formulierData.zaakHervatten && zaak.isOpgeschort()) {
            opschortenZaakHelper.hervattenZaak(zaak, REDEN_ZAAK_HERVATTEN);
        }
        versturenDocumenten(formulierData);
        ondertekenDocumenten(formulierData);

        final Map<String, String> taskData = taakVariabelenService.readTaskData(task);
        if (taskData.isEmpty()) {
            taakVariabelenService.setTaskData(task, formulierData.formState);
        } else {
            taskData.putAll(formulierData.formState);
            taakVariabelenService.setTaskData(task, taskData);
        }

        final Map<String, Object> zaakVariablen = zaakVariabelenService.readProcessZaakdata(zaak.getUuid());
        zaakVariablen.putAll(formulierData.dataElementen);
        zaakVariabelenService.setZaakdata(zaak.getUuid(), zaakVariablen);
        
        return task;
    }

    private void resolveMeerkeuzeOpties(final RESTFormulierVeldDefinitie veldDefinitie) {
        final var referenceTableCode = substringAfter(veldDefinitie.meerkeuzeOpties, "REF:");
        if (isNotBlank(referenceTableCode)) {
            final var referenceTable = referenceTableService.readReferenceTable(referenceTableCode);
            veldDefinitie.meerkeuzeOpties = referenceTable.getValues()
                    .stream()
                    .sorted(Comparator.comparingInt(ReferenceTableValue::getSortOrder))
                    .map(ReferenceTableValue::getName)
                    .collect(Collectors.joining(REFERENCE_TABLE_SEPARATOR));
        }
    }

    private void resolveDefaultWaarde(final RESTFormulierVeldDefinitie veldDefinitie, final Zaak zaak,
                                      final RestTask restTask, final Map<String, Object> zaakData) {
        if (isNotEmpty(veldDefinitie.defaultWaarde)) {
            veldDefinitie.defaultWaarde = switch (veldDefinitie.defaultWaarde) {
                case "TAAK:STARTDATUM" -> restTask.getCreatiedatumTijd().format(DATUM_FORMAAT);
                case "TAAK:FATALE_DATUM" -> restTask.getFataledatum().format(DATUM_FORMAAT);
                case "TAAK:GROEP" -> restTask.getGroep() != null ? restTask.getGroep().naam : null;
                case "TAAK:BEHANDELAAR" -> restTask.getBehandelaar() != null ? restTask.getBehandelaar().naam : null;
                case "ZAAK:STARTDATUM" -> zaak.getStartdatum().format(DATUM_FORMAAT);
                case "ZAAK:FATALE_DATUM" -> zaak.getUiterlijkeEinddatumAfdoening().format(DATUM_FORMAAT);
                case "ZAAK:STREEFDATUM" -> zaak.getEinddatumGepland().format(DATUM_FORMAAT);
                case "ZAAK:GROEP" -> getGroepForZaakDefaultWaarde(zaak);
                case "ZAAK:BEHANDELAAR" -> getBehandelaarForZaakDefaultWaarde(zaak);
                default -> processDefaultWaarde(veldDefinitie, zaakData);
            };
        }
    }

    private String getGroepForZaakDefaultWaarde(final Zaak zaak) {
        return zgwApiService.findGroepForZaak(zaak)
                .map(groep -> identityService.readGroup(groep.getBetrokkeneIdentificatie().getIdentificatie()).getName())
                .orElse(null);
    }

    private String getBehandelaarForZaakDefaultWaarde(final Zaak zaak) {
        return zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
                .map(behandelaar -> identityService.readUser(behandelaar.getIdentificatienummer()).getFullName())
                .orElse(null);
    }

    private String processDefaultWaarde(final RESTFormulierVeldDefinitie veldDefinitie, final Map<String, Object> zaakData) {
        if (veldDefinitie.defaultWaarde.startsWith(":")) {
            veldDefinitie.defaultWaarde =
                    zaakData.getOrDefault(veldDefinitie.defaultWaarde.substring(1), "").toString();
        }
        return switch (veldDefinitie.veldtype) {
            case CHECKBOX -> processCheckboxDefaultWaarde(veldDefinitie.defaultWaarde);
            case DATUM -> processDatumDefaultwaarde(veldDefinitie.defaultWaarde);
            default -> veldDefinitie.defaultWaarde;
        };
    }

    private String processCheckboxDefaultWaarde(final String defaultWaarde) {
        return (StringUtils.equalsIgnoreCase("ja", defaultWaarde) ||
                StringUtils.equalsIgnoreCase("true", defaultWaarde) ||
                StringUtils.equals("1", defaultWaarde))
                ? BooleanUtils.TRUE : BooleanUtils.FALSE;
    }

    private String processDatumDefaultwaarde(final String defaultWaarde) {
        if (defaultWaarde.matches("^[+-]\\d{1,4}$")) {
            int dagen = Integer.parseInt(substring(defaultWaarde, 1));
            if (defaultWaarde.startsWith("+")) {
                return LocalDate.now().plusDays(dagen).format(DATUM_FORMAAT);
            } else {
                return LocalDate.now().minusDays(dagen).format(DATUM_FORMAAT);
            }
        } else {
            return defaultWaarde;
        }
    }

    private void versturenDocumenten(final FormulierData formulierData) {
        if (formulierData.documentenVerzenden != null) {
            Arrays.stream(formulierData.documentenVerzenden.split(DOCUMENT_SEPARATOR))
                    .map(UUID::fromString)
                    .map(drcClientService::readEnkelvoudigInformatieobject)
                    .forEach(enkelvoudigInformatieObject ->
                            enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                                    uuidFromURI(enkelvoudigInformatieObject.getUrl()),
                                    formulierData.documentenVerzendenDatum,
                                    formulierData.toelichting));
        }
    }

    private void ondertekenDocumenten(final FormulierData formulierData) {
        if (formulierData.documentenOndertekenen != null) {
            Arrays.stream(formulierData.documentenOndertekenen.split(DOCUMENT_SEPARATOR))
                    .map(UUID::fromString)
                    .map(drcClientService::readEnkelvoudigInformatieobject)
                    .filter(enkelvoudigInformatieobject -> enkelvoudigInformatieobject.getOndertekening() == null)
                    .forEach(enkelvoudigInformatieobject ->
                            enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                                    uuidFromURI(enkelvoudigInformatieobject.getUrl())));
        }
    }
}
