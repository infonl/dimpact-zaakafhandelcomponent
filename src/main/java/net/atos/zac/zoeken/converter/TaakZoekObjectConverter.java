package net.atos.zac.zoeken.converter;

import static net.atos.zac.flowable.task.TaakVariabelenService.readTaskData;
import static net.atos.zac.flowable.task.TaakVariabelenService.readTaskInformation;
import static net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie;
import static net.atos.zac.flowable.task.TaakVariabelenService.readZaakUUID;
import static net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeUUID;
import static net.atos.zac.flowable.util.TaskUtil.getTaakStatus;
import static net.atos.zac.identity.model.UserKt.getFullName;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.TaskInfo;

import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.flowable.task.FlowableTaskService;
import net.atos.zac.flowable.task.TaakVariabelenService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;
import net.atos.zac.zoeken.model.index.ZoekObjectType;
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject;

public class TaakZoekObjectConverter extends AbstractZoekObjectConverter<TaakZoekObject> {

    @Inject
    private IdentityService identityService;

    @Inject
    private FlowableTaskService flowableTaskService;

    @Inject
    private TaakVariabelenService taakVariabelenService;

    @Inject
    private ZtcClientService ztcClientService;

    @Inject
    private ZrcClientService zrcClientService;

    @Override
    public TaakZoekObject convert(final String taskID) {
        final TaskInfo taskInfo = flowableTaskService.readTask(taskID);
        final TaakZoekObject taakZoekObject = new TaakZoekObject();

        taakZoekObject.setNaam(taskInfo.getName());
        taakZoekObject.setId(taskInfo.getId());
        taakZoekObject.setType(ZoekObjectType.TAAK);
        taakZoekObject.setCreatiedatum(taskInfo.getCreateTime());
        taakZoekObject.setToekenningsdatum(taskInfo.getClaimTime());
        taakZoekObject.setFataledatum(taskInfo.getDueDate());
        taakZoekObject.setToelichting(taskInfo.getDescription());

        if (taskInfo.getAssignee() != null) {
            final User user = identityService.readUser(taskInfo.getAssignee());
            taakZoekObject.setBehandelaarNaam(getFullName(user));
            taakZoekObject.setBehandelaarGebruikersnaam(user.getId());
            taakZoekObject.setToegekend(true);
        }

        taakZoekObject.setStatus(getTaakStatus(taskInfo));
        final String groupID = extractGroupId(taskInfo.getIdentityLinks());
        if (groupID != null) {
            final Group group = identityService.readGroup(groupID);
            taakZoekObject.setGroepID(group.getId());
            taakZoekObject.setGroepNaam(group.getName());
        }

        final ZaakType zaaktype = ztcClientService.readZaaktype(readZaaktypeUUID(taskInfo));
        taakZoekObject.setZaaktypeIdentificatie(zaaktype.getIdentificatie());
        taakZoekObject.setZaaktypeOmschrijving(zaaktype.getOmschrijving());
        taakZoekObject.setZaaktypeUuid(URIUtil.parseUUIDFromResourceURI(zaaktype.getUrl()).toString());

        final UUID zaakUUID = readZaakUUID(taskInfo);
        taakZoekObject.setZaakUUID(zaakUUID.toString());
        taakZoekObject.setZaakIdentificatie(readZaakIdentificatie(taskInfo));

        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        taakZoekObject.setZaakOmschrijving(zaak.getOmschrijving());
        taakZoekObject.setZaakToelichting(zaak.getToelichting());

        taakZoekObject.setTaakData(
                readTaskData(taskInfo).entrySet().stream()
                        .map(data -> "%s|%s".formatted(data.getKey(), data.getValue()))
                        .toList()
        );

        taakZoekObject.setTaakInformatie(
                readTaskInformation(taskInfo).entrySet().stream()
                        .map(informatie -> "%s|%s".formatted(informatie.getKey(), informatie.getValue()))
                        .toList()
        );

        return taakZoekObject;
    }

    @Override
    public boolean supports(final ZoekObjectType objectType) {
        return objectType == ZoekObjectType.TAAK;
    }

    private String extractGroupId(final List<? extends IdentityLinkInfo> identityLinks) {
        return identityLinks.stream()
                .filter(identityLinkInfo -> IdentityLinkType.CANDIDATE.equals(identityLinkInfo.getType()))
                .findAny()
                .map(IdentityLinkInfo::getGroupId)
                .orElse(null);
    }
}
