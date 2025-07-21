/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.util.AllOpen
import org.jetbrains.annotations.NotNull

@AllOpen
@ValidBetrokkeneIdentificatie
data class BetrokkeneIdentificatie(
    @NotNull
    val type: IdentificatieType = IdentificatieType.BSN,
    val bsnNummer: String? = null,
    val kvkNummer: String? = null,
    val vestigingsnummer: String? = null,
    val rsinNummer: String? = null
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
        if (value == null) return true // Use @NotNull on the class if needed

        return when (value.type) {
            IdentificatieType.BSN -> !value.bsnNummer.isNullOrBlank()
                && value.kvkNummer.isNullOrBlank()
                && value.vestigingsnummer.isNullOrBlank()
                && value.rsinNummer.isNullOrBlank()
            IdentificatieType.VN -> !value.kvkNummer.isNullOrBlank()
                && !value.vestigingsnummer.isNullOrBlank()
                && value.bsnNummer.isNullOrBlank()
                && value.rsinNummer.isNullOrBlank()
            IdentificatieType.RSIN -> !value.rsinNummer.isNullOrBlank()
                && value.bsnNummer.isNullOrBlank()
                && value.kvkNummer.isNullOrBlank()
                && value.vestigingsnummer.isNullOrBlank()
        }
    }
}
