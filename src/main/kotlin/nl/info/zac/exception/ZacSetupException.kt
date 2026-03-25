/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.exception

/**
 * Indicates a problem in the setup of ZAC.
 * This should typically be solved by functional administrators of ZAC and should not be treated as a server error.
 * A setup problem differs from a configuration problem in that the setup of ZAC is managed through the ZAC user interface
 * (by a functional administrator), while a configuration problem is related to the technical configuration of ZAC,
 * which is managed by technical administrators and may require changes to configuration files or environment settings.
 */
open class ZacSetupException(override val message: String, open val errorCode: ErrorCode) : RuntimeException(message)
