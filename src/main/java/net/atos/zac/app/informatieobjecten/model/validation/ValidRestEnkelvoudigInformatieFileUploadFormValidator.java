/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieFileUpload;


public class ValidRestEnkelvoudigInformatieFileUploadFormValidator implements
                                                                   ConstraintValidator<ValidRestEnkelvoudigInformatieFileUploadForm, RestEnkelvoudigInformatieFileUpload> {

    @Override
    public boolean isValid(RestEnkelvoudigInformatieFileUpload value, ConstraintValidatorContext context) {
        if (value.bestandsnaam != null) {
            return value.file != null && value.file.length != 0;
        }

        return true;
    }
}
