/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.exception

import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException

class ZaakspecifiekeAutorisatieCannotBeLiftedException : InputValidationFailedException(
    errorCode = ErrorCode.ERROR_CODE_ZAAKSPECIFIEKE_AUTORISATIE_CANNOT_BE_LIFTED
)
