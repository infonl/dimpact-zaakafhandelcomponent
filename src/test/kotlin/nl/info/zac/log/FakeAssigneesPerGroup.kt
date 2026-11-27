/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

class FakeAssigneesPerGroup(
    val assigneesPerGroup: Map<String, List<Employee>>,
    val groupPerAssignee: Map<Employee, String>
) : LogEvent("Fake assignees per group")
