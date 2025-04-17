/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.util.ValidationUtil
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooGenericExceptionThrown")
class FormulierDefinitieService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun listFormulierDefinities(): MutableList<FormulierDefinitie?> {
        val builder = entityManager.getCriteriaBuilder()
        val query = builder.createQuery<FormulierDefinitie?>(FormulierDefinitie::class.java)
        val root = query.from<FormulierDefinitie?>(FormulierDefinitie::class.java)
        query.orderBy(builder.asc(root.get<Any?>("naam")))
        query.select(root)
        return entityManager.createQuery<FormulierDefinitie?>(query).getResultList()
    }

    fun readFormulierDefinitie(id: Long?) =
        entityManager.find<FormulierDefinitie?>(FormulierDefinitie::class.java, id)
            ?: throw RuntimeException(
                "${FormulierDefinitie::class.java.simpleName} with id=$id not found"
            )

    fun readFormulierDefinitie(systeemnaam: String?) =
        findFormulierDefinitie(systeemnaam)
            ?: throw RuntimeException(
                "${FormulierDefinitie::class.java.simpleName} with code='$systeemnaam' not found"
            )

    fun findFormulierDefinitie(systeemnaam: String?): FormulierDefinitie? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery<FormulierDefinitie?>(FormulierDefinitie::class.java)
        val root = query.from<FormulierDefinitie?>(FormulierDefinitie::class.java)
        query.select(root).where(builder.equal(root.get<Any?>("systeemnaam"), systeemnaam))
        val resultList = entityManager.createQuery<FormulierDefinitie?>(query).getResultList()
        return if (resultList.isEmpty()) null else resultList.first()
    }

    fun createFormulierDefinitie(formulierDefinitie: FormulierDefinitie): FormulierDefinitie? {
        val now = ZonedDateTime.now()
        formulierDefinitie.creatiedatum = now
        formulierDefinitie.wijzigingsdatum = now
        ValidationUtil.valideerObject(formulierDefinitie)
        findFormulierDefinitie(formulierDefinitie.systeemnaam)?.let {
            if (it.id != formulierDefinitie.id) {
                throw RuntimeException(
                    "Er bestaat al een formulier definitie met systeemnaam '${formulierDefinitie.systeemnaam}'"
                )
            }
        }
        return entityManager.merge<FormulierDefinitie?>(formulierDefinitie)
    }

    fun updateFormulierDefinitie(formulierDefinitie: FormulierDefinitie): FormulierDefinitie? {
        val bestaandeDefinitie = readFormulierDefinitie(formulierDefinitie.id)
        formulierDefinitie.systeemnaam = bestaandeDefinitie.systeemnaam
        formulierDefinitie.creatiedatum = bestaandeDefinitie.creatiedatum
        formulierDefinitie.wijzigingsdatum = ZonedDateTime.now()
        ValidationUtil.valideerObject(formulierDefinitie)
        return entityManager.merge<FormulierDefinitie?>(formulierDefinitie)
    }

    fun deleteFormulierDefinitie(id: Long) =
        // controleren op gebruik
        entityManager.remove(
            entityManager.find<FormulierDefinitie?>(FormulierDefinitie::class.java, id)
        )
}
