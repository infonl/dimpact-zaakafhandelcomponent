/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.exception

class CMMNModelAlreadyMappedException(override val message: String) : IllegalArgumentException(message)
