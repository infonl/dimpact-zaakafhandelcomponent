/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public final class ValidationUtil {

    private static final String ID = "A-Za-z\\d";

    private static final String LCL = "[" + ID + "!#$%&'*+\\-/=?^_`{|}~]+";

    private static final String LBL = "[" + ID + "]([" + ID + "\\-]*[" + ID + "])?";

    private static final String EMAIL = LCL + "(\\." + LCL + ")*@" + LBL + "(\\." + LBL + ")+";

    private static final Pattern emailRegex = Pattern.compile("^" + EMAIL + "$");

    /**
     * Validates an object using Jakarta Validation annotations defined in the object class.
     * Only use this when the `@Valid` annotation cannot be used on the object.
     *
     * @param object the object to validatie
     * @param validationGroups option validation groups to use
     * @throws ConstraintViolationException if the object is not valid
     */
    public static void valideerObject(final Object object, final Class<?>... validationGroups) {
        final Set<ConstraintViolation<Object>> violations = valideer(object, validationGroups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<ConstraintViolation<?>>(violations));
        }
    }

    public static boolean isValidEmail(final String email) {
        return email.matches(emailRegex.pattern());
    }

    private static Set<ConstraintViolation<Object>> valideer(final Object object, final Class<?>... validationGroups) {
        try (final ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = factory.getValidator();
            return validator.validate(object, validationGroups);
        }
    }
}
