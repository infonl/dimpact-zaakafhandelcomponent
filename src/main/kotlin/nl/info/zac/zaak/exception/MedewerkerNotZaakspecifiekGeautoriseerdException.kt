/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class MedewerkerNotZaakspecifiekGeautoriseerdException : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_MEDEWERKER_NOT_ZAAKSPECIFIEK_GEAUTORISEERD
)
