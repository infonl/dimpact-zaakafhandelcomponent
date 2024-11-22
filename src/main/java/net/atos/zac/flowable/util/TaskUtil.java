/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.util;

import static net.atos.zac.app.task.model.TaakStatus.AFGEROND;
import static net.atos.zac.app.task.model.TaakStatus.NIET_TOEGEKEND;
import static net.atos.zac.app.task.model.TaakStatus.TOEGEKEND;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;

import net.atos.zac.app.task.model.TaakStatus;

public class TaskUtil {
    public static boolean isOpen(final TaskInfo taskInfo) {
        return getTaakStatus(taskInfo) != AFGEROND;
    }

    public static boolean isCmmnTask(final TaskInfo taskInfo) {
        return ScopeTypes.CMMN.equals(taskInfo.getScopeType());
    }

    public static TaakStatus getTaakStatus(final TaskInfo taskInfo) {
        return taskInfo instanceof Task ? (taskInfo.getAssignee() == null ? NIET_TOEGEKEND : TOEGEKEND) : AFGEROND;
    }
}
