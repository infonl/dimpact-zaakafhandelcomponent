/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_RELEASED
)
