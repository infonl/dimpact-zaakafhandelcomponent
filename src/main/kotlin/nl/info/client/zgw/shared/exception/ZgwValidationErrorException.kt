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

    override val message: String
        get() = "%s [%d %s] %s: %s (%s %s)".format(
            Locale.ROOT,
            validatieFout.title,
            validatieFout.status,
            validatieFout.code,
            validatieFout.detail,
            validatieFout.invalidParams.joinToString(", ") { "${it.name} [${it.code}] ${it.reason}" },
            validatieFout.type,
            validatieFout.instance
        )
}
