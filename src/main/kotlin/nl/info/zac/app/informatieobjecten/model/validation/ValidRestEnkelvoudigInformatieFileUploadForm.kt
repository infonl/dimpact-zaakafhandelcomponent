/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@MustBeDocumented
@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [ValidRestEnkelvoudigInformatieFileUploadFormValidator::class])
annotation class ValidRestEnkelvoudigInformatieFileUploadForm(
    val message: String = INVALID_FILE_UPLOAD_FORM,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
) {
    companion object {
        const val INVALID_FILE_UPLOAD_FORM = "Uploaded file is empty"
    }
}
