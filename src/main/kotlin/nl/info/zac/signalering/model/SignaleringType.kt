/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotNull
import nl.info.zac.database.flyway.FlywayIntegrator.Companion.SCHEMA
import nl.info.zac.signalering.model.SignaleringTarget.GROUP
import nl.info.zac.signalering.model.SignaleringTarget.USER
import nl.info.zac.util.AllOpen

@Entity
@Table(schema = SCHEMA, name = "signaleringtype")
@AllOpen
class SignaleringType : Comparable<SignaleringType> {
    companion object {
        const val SEPARATOR = ";"
    }

    @Id
    @Column(name = "signaleringtype_enum", updatable = false, insertable = false)
    lateinit var id: String

    @NotNull
    @Column(name = "subjecttype_enum", updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    lateinit var subjecttype: SignaleringSubject

    /** Workaround, Hibernate does not support primary keys of type enum. */
    @Transient
    private var cachedType: Type? = null

    var type: Type
        get() = cachedType ?: Type.valueOf(id).also { cachedType = it }
        set(value) {
            cachedType = value
        }

    /**
     * Maps to signalering-type.ts
     * !! Don't forget to add a new enum value to signaleringtype table and to add the translation !!
     */
    enum class Type(val naam: String, val bericht: String, vararg targets: SignaleringTarget) {
        ZAAK_DOCUMENT_TOEGEVOEGD("Zaakdocument toegevoegd", "Er is een document aan uw zaak toegevoegd.", USER),
        ZAAK_OP_NAAM("Zaak op naam", "Er is een zaak op uw naam gezet.", USER, GROUP),
        ZAAK_VERLOPEND(
            "Zaak verloopt",
            "Uw zaak nadert de streefdatum.${SEPARATOR}Uw zaak nadert de fatale datum.",
            USER
        ),
        TAAK_OP_NAAM("Taak op naam", "Er is een taak op uw naam gezet.", USER),
        TAAK_VERLOPEN("Taak verloopt", "Uw taak heeft de fatale datum bereikt.", USER) {
            // This one has no dashboard card
            override val isDashboard = false
        };

        private val targets: Set<SignaleringTarget> = targets.toSet()

        fun isTarget(target: SignaleringTarget) = target in targets

        open val isDashboard = true

        val isMail = true
    }

    override fun compareTo(other: SignaleringType): Int =
        subjecttype.compareTo(other.subjecttype).takeIf { it != 0 } ?: type.compareTo(other.type)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return type == (other as SignaleringType).type
    }

    override fun hashCode() = type.hashCode()

    override fun toString() = type.toString()
}
