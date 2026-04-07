/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import nl.info.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieFileUpload

class ValidRestEnkelvoudigInformatieFileUploadFormValidator :
    ConstraintValidator<ValidRestEnkelvoudigInformatieFileUploadForm, RestEnkelvoudigInformatieFileUpload> {

    override fun isValid(value: RestEnkelvoudigInformatieFileUpload, context: ConstraintValidatorContext?): Boolean {
        if (value.bestandsnaam != null) {
            return value.file != null && value.file!!.isNotEmpty()
        }
        return true
    }
}
