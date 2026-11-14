/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.task;

import static net.atos.zac.flowable.ZaakVariabelenService.*;
import static net.atos.zac.flowable.util.TaskUtil.isCmmnTask;

import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;

@ApplicationScoped
@Transactional
public class TaakVariabelenService {
    public static final String TAAK_DATA_DOCUMENTEN_VERZENDEN_POST = "documentenVerzendenPost";
    public static final String TAAK_DATA_VERZENDDATUM = "verzenddatum";
    public static final String TAAK_DATA_TOELICHTING = "toelichting";
    public static final String TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER = ";";

    private static final String TAAK_DATA_ONDERTEKENEN = "ondertekenen";
    private static final String TAAK_DATA_ZAAK_OPSCHORTEN = "zaakOpschorten";
    private static final String TAAK_DATA_ZAAK_HERVATTEN = "zaakHervatten";
    private final static String TAAK_DATA_MAIL_FROM = "verzender";
    private final static String TAAK_DATA_MAIL_REPLYTO = "replyTo";
    private final static String TAAK_DATA_MAIL_TO = "emailadres";
    private final static String TAAK_DATA_MAIL_BODY = "body";
    private static final String TAAK_DATA_MAIL_BIJLAGEN = "bijlagen";
    private static final String VAR_TASK_TAAKDATA = "taakdata";
    private static final String VAR_TASK_TAAKDOCUMENTEN = "taakdocumenten";
    private static final String VAR_TASK_TAAKINFORMATIE = "taakinformatie";

    @Inject
    private TaskService taskService;

    public static Map<String, Object> readTaskData(final TaskInfo taskInfo) {
        return (Map<String, Object>) findTaskVariable(taskInfo, VAR_TASK_TAAKDATA).orElse(Collections.emptyMap());
    }

    public static Map<String, String> readTaskInformation(final TaskInfo taskInfo) {
        return (Map<String, String>) findTaskVariable(taskInfo, VAR_TASK_TAAKINFORMATIE).orElse(Collections.emptyMap());
    }

    public static List<UUID> readTaskDocuments(final TaskInfo taskInfo) {
        return (List<UUID>) findTaskVariable(taskInfo, VAR_TASK_TAAKDOCUMENTEN).orElse(Collections.emptyList());
    }

    public static Optional<String> readMailFrom(Map<String, String> taakData) {
        return findStringTaskDataElement(taakData, TAAK_DATA_MAIL_FROM);
    }

    public static Optional<String> readMailReplyTo(Map<String, String> taakData) {
        return findStringTaskDataElement(taakData, TAAK_DATA_MAIL_REPLYTO);
    }

    public static Optional<String> readMailTo(Map<String, String> taakData) {
        return findStringTaskDataElement(taakData, TAAK_DATA_MAIL_TO);
    }

    public static Optional<String> readMailBody(Map<String, String> taakData) {
        return findStringTaskDataElement(taakData, TAAK_DATA_MAIL_BODY);
    }

    public static void setMailBody(Map<String, String> taakData, final String body) {
        taakData.put(TAAK_DATA_MAIL_BODY, body);
    }

    public static Optional<String> readMailAttachments(Map<String, String> taakData) {
        return findStringTaskDataElement(taakData, TAAK_DATA_MAIL_BIJLAGEN);
    }

    public static Optional<String> readSignatures(Map<String, Object> taakData) {
        return findObjectTaskDataElement(taakData, TAAK_DATA_ONDERTEKENEN);
    }

    public static boolean isZaakOpschorten(Map<String, String> taakData) {
        Optional<String> zaakOpgeschort = findStringTaskDataElement(taakData, TAAK_DATA_ZAAK_OPSCHORTEN);
        return zaakOpgeschort.filter(BooleanUtils.TRUE::equals).isPresent();
    }

    public static boolean isZaakHervatten(Map<String, Object> taakData) {
        final Optional<String> zaakHervatten = findObjectTaskDataElement(taakData, TAAK_DATA_ZAAK_HERVATTEN);
        return zaakHervatten.filter(BooleanUtils.TRUE::equals).isPresent();
    }

    public static UUID readZaakUUID(final TaskInfo taskInfo) {
        return (UUID) readVariable(taskInfo, VAR_ZAAK_UUID);
    }

    public static String readZaakIdentificatie(final TaskInfo taskInfo) {
        return (String) readVariable(taskInfo, VAR_ZAAK_IDENTIFICATIE);
    }

    public static UUID readZaaktypeUUID(final TaskInfo taskInfo) {
        return (UUID) readVariable(taskInfo, VAR_ZAAKTYPE_UUUID);
    }

    public static String readZaaktypeOmschrijving(final TaskInfo taskInfo) {
        return (String) readVariable(taskInfo, VAR_ZAAKTYPE_OMSCHRIJVING);
    }

    public void setTaskData(final Task task, final Map<String, Object> taakdata) {
        setTaskVariable(task, VAR_TASK_TAAKDATA, taakdata);
    }

    public void setTaskinformation(final Task task, final Map<String, String> taakinformatie) {
        setTaskVariable(task, VAR_TASK_TAAKINFORMATIE, taakinformatie);
    }

    public void setTaakdocumenten(final Task task, final List<UUID> taakdocumenten) {
        setTaskVariable(task, VAR_TASK_TAAKDOCUMENTEN, taakdocumenten);
    }

    private static Map<String, Object> getVariables(final TaskInfo taskInfo) {
        return isCmmnTask(taskInfo) ? taskInfo.getCaseVariables() : taskInfo.getProcessVariables();
    }

    private static Optional<Object> findVariable(final TaskInfo taskInfo, final String variableName) {
        final Object value = getVariables(taskInfo).get(variableName);
        if (value != null) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    private static Object readVariable(final TaskInfo taskInfo, final String variableName) {
        return findVariable(taskInfo, variableName)
                .orElseThrow(() -> new RuntimeException(
                        "No variable found with name '%s' for task with name '%s' and id '%s'"
                                .formatted(variableName, taskInfo.getName(), taskInfo.getId())));
    }

    private static Optional<Object> findTaskVariable(final TaskInfo taskInfo, final String variableName) {
        final Object value = taskInfo.getTaskLocalVariables().get(variableName);
        if (value != null) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> findStringTaskDataElement(final Map<String, String> taakData, final String elementName) {
        final String value = taakData.get(elementName);
        if (StringUtils.isNotEmpty(value)) {
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> findObjectTaskDataElement(final Map<String, Object> taakData, final String elementName) {
        if (taakData.get(elementName)instanceof String stringValue && StringUtils.isNotEmpty(stringValue)) {
            return Optional.of(stringValue);
        } else {
            return Optional.empty();
        }
    }

    private void setTaskVariable(final Task task, final String variableName, final Object value) {
        taskService.setVariableLocal(task.getId(), variableName, value);
    }
}
