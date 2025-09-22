/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.exception

class BPMNModelAlreadyMappedException(override val message: String) : IllegalArgumentException(message)
