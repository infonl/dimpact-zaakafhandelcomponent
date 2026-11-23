/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import nl.info.client.zgw.shared.model.ZgwValidationError
import nl.info.zac.exception.InputValidationFailedException
import java.util.Locale

/**
 * Exception to indicate a validation error that occurred in when calling the ZGW API.
 */
class ZgwValidationErrorException(val validatieFout: ZgwValidationError) : InputValidationFailedException() {

    /**
     * Only the status, the error code and the name and code of each invalid parameter. Not the reasons, nor the title,
     * detail, type and instance of the remote error: those can echo a rejected value, which can be personal data, and
     * this message ends up in logs. The REST response reads them from [validatieFout].
     */
    override val message: String
        get() = "%d %s: %s".format(
            Locale.ROOT,
            validatieFout.status,
            validatieFout.code,
            validatieFout.invalidParams.joinToString(", ") { "${it.name} [${it.code}]" }
        )
}
