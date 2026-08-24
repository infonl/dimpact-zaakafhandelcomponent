/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import java.util.regex.Pattern

private const val ID = "A-Za-z\\d"
private const val LCL = "[" + ID + "!#\$%&'*+\\-/=?^_`{|}~]+"
private const val LBL = "[" + ID + "]([" + ID + "\\-]*[" + ID + "])?"
private const val EMAIL = LCL + "(\\." + LCL + ")*@" + LBL + "(\\." + LBL + ")+"

private val emailRegex = Pattern.compile("^" + EMAIL + "\$")

// Building a ValidatorFactory bootstraps the whole Bean Validation provider (classpath
// scanning, constraint metadata parsing); it is designed to be built once and reused for
// the application's lifetime, not per validation call. It is lazy so that isValidEmail(),
// which needs no Bean Validation provider, never triggers this bootstrap.
private val validatorFactory by lazy { Validation.buildDefaultValidatorFactory() }

/**
 * Validates an object using Jakarta Validation annotations defined in the object class.
 * Only use this when the `@Valid` annotation cannot be used on the object.
 *
 * @throws ConstraintViolationException if the object is not valid
 */
fun validateObject(target: Any, vararg validationGroups: Class<*>) {
    val violations = validatorFactory.validator.validate(target, *validationGroups)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations.toSet())
    }
}

fun isValidEmail(email: String): Boolean = emailRegex.matcher(email).matches()
