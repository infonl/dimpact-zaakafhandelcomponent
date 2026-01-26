/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.jetbrains.annotations.NotNull
import java.util.UUID
import kotlin.reflect.KClass

@AllOpen
@NoArgConstructor
@ValidBetrokkeneIdentificatie
data class BetrokkeneIdentificatie(
    @field:NotNull
    var type: IdentificatieType,
    var personId: UUID? = null,
    var kvkNummer: String? = null,
    var rsin: String? = null,
    var vestigingsnummer: String? = null
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [BetrokkeneIdentificatieValidator::class])
annotation class ValidBetrokkeneIdentificatie(
    val message: String = "Invalid BetrokkeneIdentificatie",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class BetrokkeneIdentificatieValidator : ConstraintValidator<ValidBetrokkeneIdentificatie, BetrokkeneIdentificatie> {
    override fun isValid(value: BetrokkeneIdentificatie?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return false
        return when (value.type) {
            IdentificatieType.BSN ->
                value.personId != null &&
                    value.kvkNummer.isNullOrBlank() &&
                    value.vestigingsnummer.isNullOrBlank()
            IdentificatieType.VN -> !value.kvkNummer.isNullOrBlank() &&
                !value.vestigingsnummer.isNullOrBlank() &&
                value.personId == null
            IdentificatieType.RSIN -> !value.kvkNummer.isNullOrBlank() &&
                value.personId == null &&
                value.vestigingsnummer.isNullOrBlank()
        }
    }
}
