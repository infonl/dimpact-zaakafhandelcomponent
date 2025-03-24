/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn.exception

class ProcessDefinitionNotFoundException(override val message: String) : RuntimeException(message)
