/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import org.flowable.engine.repository.ProcessDefinition

/**
 * Process definitions deployed before the upload check on the process name may have no name or a blank one.
 */
fun ProcessDefinition.nameOrKey(): String = name?.ifBlank { null } ?: key
