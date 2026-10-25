/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.signalering.model.Signalering
import nl.info.zac.signalering.model.SignaleringInstellingen
import nl.info.zac.signalering.model.SignaleringInstellingenZoekParameters
import nl.info.zac.signalering.model.SignaleringTarget
import nl.info.zac.signalering.model.SignaleringVerzonden
import nl.info.zac.signalering.model.SignaleringVerzondenZoekParameters
import nl.info.zac.signalering.model.SignaleringZoekParameters

fun getSignaleringWhere(
    parameters: SignaleringZoekParameters,
    builder: CriteriaBuilder,
    root: Root<Signalering>
): Predicate {
    val where = mutableListOf<Predicate>()
    parameters.targettype?.let {
        where.add(builder.equal(root.get<Any>("targettype"), it))
    }
    parameters.target?.let {
        where.add(builder.equal(root.get<Any>("target"), it))
    }
    if (parameters.types.isNotEmpty()) {
        where.add(
            root.get<Any>("type").get<Any>("id").`in`(parameters.types.map { it.toString() })
        )
    }
    parameters.subjecttype?.let { subjecttype ->
        where.add(builder.equal(root.get<Any>("type").get<Any>("subjecttype"), subjecttype))
        parameters.subject?.let { where.add(builder.equal(root.get<Any>("subject"), it)) }
    }
    parameters.detail?.let {
        where.add(builder.equal(root.get<Any>("detail"), parameters.detail))
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
    when (parameters.ownertype) {
        SignaleringTarget.GROUP -> where.add(builder.equal(root.get<Any>("groep"), parameters.owner))
        SignaleringTarget.USER -> where.add(builder.equal(root.get<Any>("medewerker"), parameters.owner))
    }
    parameters.type?.let {
        where.add(builder.equal(root.get<Any>("type").get<Any>("id"), it.toString()))
    }
    if (parameters.isDashboard) {
        where.add(builder.isTrue(root.get("isDashboard")))
    }
    if (parameters.isMail) {
        where.add(builder.isTrue(root.get("isMail")))
    }
    @Suppress("SpreadOperator")
    return builder.and(*where.toTypedArray<Predicate>())
}

fun getSignaleringVerzondenWhere(
    parameters: SignaleringVerzondenZoekParameters,
    builder: CriteriaBuilder,
    root: Root<SignaleringVerzonden>
): Predicate {
    val where = mutableListOf<Predicate>()
    parameters.targettype?.let {
        where.add(builder.equal(root.get<Any>("targettype"), it))
    }
    parameters.target?.let {
        where.add(builder.equal(root.get<Any>("target"), it))
    }
    if (parameters.types.isNotEmpty()) {
        where.add(
            root.get<Any>("type").get<Any>("id").`in`(parameters.types.map { it.toString() })
        )
    }
    parameters.subjecttype?.let { subjecttype ->
        where.add(builder.equal(root.get<Any>("type").get<Any>("subjecttype"), subjecttype))
        parameters.subject?.let { where.add(builder.equal(root.get<Any>("subject"), it)) }
    }
    parameters.detail?.let {
        where.add(builder.equal(root.get<Any>("detail"), it.toString()))
    }
    @Suppress("SpreadOperator")
    return builder.and(*where.toTypedArray<Predicate>())
}
