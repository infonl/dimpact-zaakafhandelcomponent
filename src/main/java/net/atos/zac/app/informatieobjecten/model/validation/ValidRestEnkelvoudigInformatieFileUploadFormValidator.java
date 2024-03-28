package net.atos.zac.app.informatieobjecten.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieFileUpload;


public class ValidRestEnkelvoudigInformatieFileUploadFormValidator implements
                                                                   ConstraintValidator<ValidRestEnkelvoudigInformatieFileUploadForm, RESTEnkelvoudigInformatieFileUpload> {

    @Override
    public boolean isValid(RESTEnkelvoudigInformatieFileUpload value, ConstraintValidatorContext context) {
        if (value.bestandsnaam != null) {
            return value.file != null && value.file.length != 0;
        }

        return true;
    }
}
