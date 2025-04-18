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
    fun listFormulierDefinities(): List<FormulierDefinitie> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(FormulierDefinitie::class.java)
        val root = query.from(FormulierDefinitie::class.java)
        query.orderBy(builder.asc(root.get<Any?>("naam")))
        query.select(root)
        return entityManager.createQuery(query).getResultList()
    }

    fun readFormulierDefinitie(id: Long?) =
        entityManager.find(FormulierDefinitie::class.java, id)
            ?: throw RuntimeException(
                "${FormulierDefinitie::class.java.simpleName} with id=$id not found"
            )

    fun readFormulierDefinitie(systeemnaam: String) =
        findFormulierDefinitie(systeemnaam)
            ?: throw RuntimeException(
                "${FormulierDefinitie::class.java.simpleName} with code='$systeemnaam' not found"
            )

    fun findFormulierDefinitie(systeemnaam: String): FormulierDefinitie? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(FormulierDefinitie::class.java)
        val root = query.from(FormulierDefinitie::class.java)
        query.select(root).where(builder.equal(root.get<Any?>("systeemnaam"), systeemnaam))
        val resultList = entityManager.createQuery(query).getResultList()
        return if (resultList.isEmpty()) null else resultList.first()
    }

    fun createFormulierDefinitie(formulierDefinitie: FormulierDefinitie): FormulierDefinitie {
        val now = ZonedDateTime.now()
        formulierDefinitie.creatiedatum = now
        formulierDefinitie.wijzigingsdatum = now
        ValidationUtil.valideerObject(formulierDefinitie)
        formulierDefinitie.systeemnaam?.let { systemName ->
            findFormulierDefinitie(systemName)?.let {
                if (it.id != formulierDefinitie.id) {
                    throw RuntimeException("Er bestaat al een formulier definitie met systeemnaam '$systemName'")
                }
            }
        }
        return entityManager.merge(formulierDefinitie)
    }

    fun updateFormulierDefinitie(formulierDefinitie: FormulierDefinitie): FormulierDefinitie {
        val bestaandeDefinitie = readFormulierDefinitie(formulierDefinitie.id)
        formulierDefinitie.systeemnaam = bestaandeDefinitie.systeemnaam
        formulierDefinitie.creatiedatum = bestaandeDefinitie.creatiedatum
        formulierDefinitie.wijzigingsdatum = ZonedDateTime.now()
        ValidationUtil.valideerObject(formulierDefinitie)
        return entityManager.merge(formulierDefinitie)
    }

    fun deleteFormulierDefinitie(id: Long) =
        entityManager.remove(entityManager.find(FormulierDefinitie::class.java, id))
}
