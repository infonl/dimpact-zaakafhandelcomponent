/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.cmmn.exception

class OpenTaskItemNotFoundException(override val message: String) : RuntimeException(message)

class CaseDefinitionNotFoundException(override val message: String) : RuntimeException(message)
