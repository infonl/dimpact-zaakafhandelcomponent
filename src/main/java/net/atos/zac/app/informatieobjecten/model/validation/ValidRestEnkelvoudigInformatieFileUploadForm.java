package net.atos.zac.app.informatieobjecten.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = { ValidRestEnkelvoudigInformatieFileUploadFormValidator.class })
public @interface ValidRestEnkelvoudigInformatieFileUploadForm {

  String INVALID_FILE_UPLOAD_FORM = "Uploaded file is empty";

  String message() default INVALID_FILE_UPLOAD_FORM;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
