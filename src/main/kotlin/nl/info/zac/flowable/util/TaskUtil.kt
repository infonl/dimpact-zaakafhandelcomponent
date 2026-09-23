/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.util

import nl.info.zac.app.task.model.TaakStatus.AFGEROND
import nl.info.zac.app.task.model.TaakStatus.NIET_TOEGEKEND
import nl.info.zac.app.task.model.TaakStatus.TOEGEKEND
import org.flowable.common.engine.api.scope.ScopeTypes
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo

fun TaskInfo.isOpen() = taakStatus() != AFGEROND

fun TaskInfo.isCmmnTask() = ScopeTypes.CMMN == scopeType

fun TaskInfo.taakStatus() = if (this is Task) (if (assignee == null) NIET_TOEGEKEND else TOEGEKEND) else AFGEROND
