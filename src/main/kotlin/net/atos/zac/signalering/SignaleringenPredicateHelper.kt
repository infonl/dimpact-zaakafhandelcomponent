/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringInstellingenZoekParameters
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringVerzonden
import net.atos.zac.signalering.model.SignaleringVerzondenZoekParameters
import net.atos.zac.signalering.model.SignaleringZoekParameters
import java.util.stream.Collectors

class SignaleringenPredicateHelper {
    fun getSignaleringWhere(
        parameters: SignaleringZoekParameters,
        builder: CriteriaBuilder,
        root: Root<Signalering>
    ): Predicate {
        val where: MutableList<Predicate> = ArrayList()
        where.add(builder.equal(root.get<Any>("targettype"), parameters.targettype))
        if (parameters.target != null) {
            where.add(builder.equal(root.get<Any>("target"), parameters.target))
        }
        if (parameters.types.isNotEmpty()) {
            where.add(
                root.get<Any>("type").get<Any>("id")
                    .`in`(
                        parameters.types.stream().map { obj: SignaleringType.Type -> obj.toString() }
                            .collect(Collectors.toList())
                    )
            )
        }
        if (parameters.subjecttype != null) {
            where.add(builder.equal(root.get<Any>("type").get<Any>("subjecttype"), parameters.subjecttype))
            if (parameters.subject != null) {
                where.add(builder.equal(root.get<Any>("subject"), parameters.subject))
            }
        }
        @Suppress("SpreadOperator")
        return builder.and(*where.toTypedArray<Predicate>())
    }

    fun getSignaleringInstellingenWhere(
        parameters: SignaleringInstellingenZoekParameters,
        builder: CriteriaBuilder,
        root: Root<SignaleringInstellingen>
    ): Predicate {
        val where: MutableList<Predicate> = ArrayList()
        if (parameters.owner != null) {
            when (parameters.ownertype!!) {
                SignaleringTarget.GROUP -> {
                    where.add(builder.equal(root.get<Any>("groep"), parameters.owner))
                }

                SignaleringTarget.USER -> {
                    where.add(builder.equal(root.get<Any>("medewerker"), parameters.owner))
                }
            }
        }
        if (parameters.type != null) {
            where.add(builder.equal(root.get<Any>("type").get<Any>("id"), parameters.type.toString()))
        }
        if (parameters.dashboard) {
            where.add(builder.isTrue(root.get("dashboard")))
        }
        if (parameters.mail) {
            where.add(builder.isTrue(root.get("mail")))
        }
        @Suppress("SpreadOperator")
        return builder.and(*where.toTypedArray<Predicate>())
    }

    fun getSignaleringVerzondenWhere(
        parameters: SignaleringVerzondenZoekParameters,
        builder: CriteriaBuilder,
        root: Root<SignaleringVerzonden>
    ): Predicate {
        val where: MutableList<Predicate> = ArrayList()
        where.add(builder.equal(root.get<Any>("targettype"), parameters.targettype))
        if (parameters.target != null) {
            where.add(builder.equal(root.get<Any>("target"), parameters.target))
        }
        if (parameters.types.isNotEmpty()) {
            where.add(
                root.get<Any>("type").get<Any>("id")
                    .`in`(
                        parameters.types.stream().map { obj: SignaleringType.Type -> obj.toString() }
                            .collect(Collectors.toList())
                    )
            )
        }
        if (parameters.subjecttype != null) {
            where.add(builder.equal(root.get<Any>("type").get<Any>("subjecttype"), parameters.subjecttype))
            if (parameters.subject != null) {
                where.add(builder.equal(root.get<Any>("subject"), parameters.subject))
            }
        }
        if (parameters.detail != null) {
            where.add(builder.equal(root.get<Any>("detail"), parameters.detail.toString()))
        }
        @Suppress("SpreadOperator")
        return builder.and(*where.toTypedArray<Predicate>())
    }
}
