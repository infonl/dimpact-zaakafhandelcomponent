/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import nl.info.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieFileUpload
import nl.info.zac.configuration.AllowedFileType

class ValidRestEnkelvoudigInformatieFileUploadFormValidator :
    ConstraintValidator<ValidRestEnkelvoudigInformatieFileUploadForm, RestEnkelvoudigInformatieFileUpload> {

    override fun isValid(value: RestEnkelvoudigInformatieFileUpload, context: ConstraintValidatorContext?): Boolean {
        val file = value.file
        val bestandsnaam = value.bestandsnaam
        val hasFile = file != null && file.isNotEmpty()
        val hasName = !bestandsnaam.isNullOrBlank()
        return when {
            !hasFile && !hasName -> true
            hasFile && hasName -> AllowedFileType.isAllowed(bestandsnaam, value.formaat)
            else -> false
        }
    }
}
