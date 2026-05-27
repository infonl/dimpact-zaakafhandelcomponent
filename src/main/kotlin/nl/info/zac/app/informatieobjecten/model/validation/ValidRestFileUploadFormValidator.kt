/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import nl.info.zac.app.informatieobjecten.model.RestFileUpload
import nl.info.zac.configuration.AllowedFileType

class ValidRestFileUploadFormValidator :
    ConstraintValidator<ValidRestFileUploadForm, RestFileUpload> {

    override fun isValid(value: RestFileUpload, context: ConstraintValidatorContext?): Boolean {
        val filename = value.filename ?: return true
        val file = value.file
        if (file == null || file.isEmpty()) return false
        return AllowedFileType.isAllowed(filename, value.type)
    }
}
