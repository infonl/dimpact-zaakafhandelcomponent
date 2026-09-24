/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.exception

import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SYSTEM_REFERENCE_TABLE_NOT_CONFIGURED
import nl.info.zac.exception.ServerErrorException

class SystemReferenceTableNotConfiguredException(systemReferenceTable: SystemReferenceTable) : ServerErrorException(
    errorCode = ERROR_CODE_SYSTEM_REFERENCE_TABLE_NOT_CONFIGURED,
    message = "No system reference table found for '${systemReferenceTable.name}'. It must be seeded by an administrator."
)
