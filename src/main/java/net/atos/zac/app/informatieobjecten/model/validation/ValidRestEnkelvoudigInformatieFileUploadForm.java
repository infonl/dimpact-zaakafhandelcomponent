/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.model.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = {ValidRestEnkelvoudigInformatieFileUploadFormValidator.class})
public @interface ValidRestEnkelvoudigInformatieFileUploadForm {

    String INVALID_FILE_UPLOAD_FORM = "Uploaded file is empty";

    String message() default INVALID_FILE_UPLOAD_FORM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
