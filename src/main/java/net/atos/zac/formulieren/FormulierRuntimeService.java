package net.atos.zac.formulieren;

import static jakarta.json.JsonValue.ValueType.STRING;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;
import static net.atos.zac.util.UriUtil.uuidFromURI;
import static net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate;
import static org.apache.commons.lang3.StringUtils.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.json.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.task.api.Task;

import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.zac.admin.ReferenceTableService;
import net.atos.zac.admin.model.ReferenceTableValue;
import net.atos.zac.app.formulieren.model.FormulierData;
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService;
import net.atos.zac.app.task.model.RestTask;
import net.atos.zac.flowable.ZaakVariabelenService;
import net.atos.zac.flowable.task.FlowableTaskService;
import net.atos.zac.flowable.task.TaakVariabelenService;
import net.atos.zac.formulieren.model.FormulierVeldtype;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.shared.helper.OpschortenZaakHelper;
import net.atos.zac.util.time.DateTimeConverterUtil;

public class FormulierRuntimeService {

    private static final DateTimeFormatter DATUM_FORMAAT = ofPattern("dd-MM-yyy");

    private static final String REDEN_ZAAK_HERVATTEN = "Zaak hervat vanuit proces";

    private static final String REFERENCE_TABLE_SEPARATOR = ";";

    private static final String DOCUMENT_SEPARATOR = ";";

    private static final String FORMIO_DEFAULT_VALUE = "defaultValue";

    private static final String FORMIO_TITLE = "title";

    private static final String AANTAL_DAGEN_VANAF_HEDEN_FORMAAT = "^[+-]\\d{1,4}$";

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

    public void renderFormulierDefinitie(final RestTask restTask) {
        final ResolveDefaultValueContext resolveDefaultValueContext = new ResolveDefaultValueContext(restTask, zrcClientService,
                zaakVariabelenService);
        restTask.getFormulierDefinitie().veldDefinities.forEach(veldDefinitie -> {
            if (isNotBlank(veldDefinitie.defaultWaarde)) {
                veldDefinitie.defaultWaarde = resolveDefaultValue(veldDefinitie.defaultWaarde, resolveDefaultValueContext);
                veldDefinitie.defaultWaarde = formatDefaultValue(veldDefinitie.defaultWaarde, veldDefinitie.veldtype);
            }
            veldDefinitie.meerkeuzeOpties = resolveMultipleChoiceOptions(veldDefinitie.meerkeuzeOpties);
        });
    }

    public JsonObject renderFormioFormulier(final RestTask restTask) {
        return copyJsonObject(restTask.getFormioFormulier(),
                new ResolveDefaultValueContext(restTask, zrcClientService, zaakVariabelenService));
    }

    public Task submit(final RestTask restTask, Task task, final Zaak zaak) {
        taakVariabelenService.setTaskinformation(task, restTask.getTaakinformatie());
        taakVariabelenService.setTaskData(task, restTask.getTaakdata());

        final var formulierData = new FormulierData(restTask.getTaakdata());

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
            opschortenZaakHelper.opschortenZaak(
                    zaak,
                    DAYS.between(LocalDate.now(), convertToLocalDate(task.getDueDate())),
                    restTask.getFormulierDefinitie() != null ?
                            restTask.getFormulierDefinitie().naam :
                            restTask.getFormioFormulier().getString(FORMIO_TITLE));
        }
        if (formulierData.zaakHervatten && zaak.isOpgeschort()) {
            opschortenZaakHelper.hervattenZaak(zaak, REDEN_ZAAK_HERVATTEN);
        }
        markDocumentAsSent(formulierData);
        markDocumentAsSigned(formulierData);

        final Map<String, Object> zaakVariablen = zaakVariabelenService.readProcessZaakdata(zaak.getUuid());
        zaakVariablen.putAll(formulierData.zaakVariabelen);
        zaakVariabelenService.setZaakdata(zaak.getUuid(), zaakVariablen);

        return task;
    }

    private JsonObject copyJsonObject(
            final JsonObject jsonObject,
            final ResolveDefaultValueContext resolveDefaultValueContext
    ) {
        final var objectBuilder = Json.createObjectBuilder();
        jsonObject.entrySet().forEach(stringJsonValueEntry -> objectBuilder.add(stringJsonValueEntry.getKey(),
                copyJsonObjectValue(stringJsonValueEntry, resolveDefaultValueContext)));
        return objectBuilder.build();
    }

    private JsonValue copyJsonObjectValue(
            final Map.Entry<String, JsonValue> stringJsonValueEntry,
            final ResolveDefaultValueContext resolveDefaultValueContext
    ) {
        return stringJsonValueEntry.getValue().getValueType() == STRING &&
               stringJsonValueEntry.getKey().equals(FORMIO_DEFAULT_VALUE) ?
                       Json.createValue(resolveDefaultValue(((JsonString) stringJsonValueEntry.getValue()).getString(),
                               resolveDefaultValueContext)) :
                       copyJsonValue(stringJsonValueEntry.getValue(), resolveDefaultValueContext);
    }

    private JsonValue copyJsonValue(
            final JsonValue jsonValue,
            final ResolveDefaultValueContext resolveDefaultValueContext
    ) {
        return switch (jsonValue.getValueType()) {
            case ARRAY -> copyJsonArray(jsonValue.asJsonArray(), resolveDefaultValueContext);
            case OBJECT -> copyJsonObject(jsonValue.asJsonObject(), resolveDefaultValueContext);
            default -> jsonValue;
        };
    }

    private JsonArray copyJsonArray(
            final JsonArray jsonArray,
            final ResolveDefaultValueContext resolveDefaultValueContext
    ) {
        final var arrayBuilder = Json.createArrayBuilder();
        jsonArray.forEach(jsonValue -> arrayBuilder.add(copyJsonValue(jsonValue, resolveDefaultValueContext)));
        return arrayBuilder.build();
    }

    private String resolveDefaultValue(final String defaultValue, final ResolveDefaultValueContext context) {
        return switch (defaultValue) {
            case "TAAK:STARTDATUM" -> context.getTask().getCreatiedatumTijd().format(DATUM_FORMAAT);
            case "TAAK:FATALE_DATUM" -> context.getTask().getFataledatum().format(DATUM_FORMAAT);
            case "TAAK:GROEP" -> context.getTask().getGroep() != null ? context.getTask().getGroep().getNaam() : null;
            case "TAAK:BEHANDELAAR" -> context.getTask().getBehandelaar() != null ? context.getTask().getBehandelaar().getNaam() : null;
            case "ZAAK:STARTDATUM" -> context.getZaak().getStartdatum().format(DATUM_FORMAAT);
            case "ZAAK:FATALE_DATUM" -> context.getZaak().getUiterlijkeEinddatumAfdoening().format(DATUM_FORMAAT);
            case "ZAAK:STREEFDATUM" -> context.getZaak().getEinddatumGepland().format(DATUM_FORMAAT);
            case "ZAAK:GROEP" -> getGroepForZaakDefaultValue(context.getZaak());
            case "ZAAK:BEHANDELAAR" -> getBehandelaarForZaakDefaultValue(context.getZaak());
            default -> defaultValue.startsWith(":") ?
                    context.getZaakData().getOrDefault(defaultValue.substring(1), "").toString() :
                    defaultValue;
        };
    }

    private String getGroepForZaakDefaultValue(final Zaak zaak) {
        return zgwApiService.findGroepForZaak(zaak)
                .map(groep -> identityService.readGroup(groep.getBetrokkeneIdentificatie().getIdentificatie()).getName())
                .orElse(null);
    }

    private String getBehandelaarForZaakDefaultValue(final Zaak zaak) {
        return zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
                .map(behandelaar -> identityService.readUser(behandelaar.getIdentificatienummer()).getFullName())
                .orElse(null);
    }

    private String formatDefaultValue(final String defaultWaarde, final FormulierVeldtype veldtype) {
        return switch (veldtype) {
            case CHECKBOX -> formatCheckboxDefaultValue(defaultWaarde);
            case DATUM -> formatDatumDefaultValue(defaultWaarde);
            default -> defaultWaarde;
        };
    }

    private String formatCheckboxDefaultValue(final String defaultWaarde) {
        return (StringUtils.equalsIgnoreCase("ja", defaultWaarde) ||
                StringUtils.equalsIgnoreCase("true", defaultWaarde) ||
                StringUtils.equals("1", defaultWaarde)) ? BooleanUtils.TRUE : BooleanUtils.FALSE;
    }

    private String formatDatumDefaultValue(final String defaultWaarde) {
        if (defaultWaarde.matches(AANTAL_DAGEN_VANAF_HEDEN_FORMAAT)) {
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

    private String resolveMultipleChoiceOptions(final String meerkeuzeOpties) {
        final var referenceTableCode = substringAfter(meerkeuzeOpties, "REF:");
        if (isNotBlank(referenceTableCode)) {
            final var referenceTable = referenceTableService.readReferenceTable(referenceTableCode);
            return referenceTable.getValues()
                    .stream()
                    .sorted(Comparator.comparingInt(ReferenceTableValue::getSortOrder))
                    .map(ReferenceTableValue::getName)
                    .collect(Collectors.joining(REFERENCE_TABLE_SEPARATOR));
        } else {
            return meerkeuzeOpties;
        }
    }

    private void markDocumentAsSent(final FormulierData formulierData) {
        if (formulierData.documentenVerzenden != null) {
            Arrays.stream(formulierData.documentenVerzenden.split(DOCUMENT_SEPARATOR))
                    .map(UUID::fromString)
                    .map(drcClientService::readEnkelvoudigInformatieobject)
                    .forEach(enkelvoudigInformatieObject -> enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                            uuidFromURI(enkelvoudigInformatieObject.getUrl()),
                            formulierData.documentenVerzendenDatum,
                            formulierData.toelichting));
        }
    }

    private void markDocumentAsSigned(final FormulierData formulierData) {
        if (formulierData.documentenOndertekenen != null) {
            Arrays.stream(formulierData.documentenOndertekenen.split(DOCUMENT_SEPARATOR))
                    .map(UUID::fromString)
                    .map(drcClientService::readEnkelvoudigInformatieobject)
                    .filter(enkelvoudigInformatieobject -> enkelvoudigInformatieobject.getOndertekening() == null)
                    .forEach(enkelvoudigInformatieobject -> enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                            uuidFromURI(enkelvoudigInformatieobject.getUrl())));
        }
    }
}
