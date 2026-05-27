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
        val bestandsnaam = value.bestandsnaam ?: return true
        val file = value.file
        return file != null && file.isNotEmpty() && AllowedFileType.isAllowed(bestandsnaam, value.formaat)
    }
}
