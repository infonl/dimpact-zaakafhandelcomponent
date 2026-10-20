/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.gebruikersvoorkeuren

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardInstelling
import nl.info.zac.gebruikersvoorkeuren.model.TabelInstellingen
import nl.info.zac.gebruikersvoorkeuren.model.Werklijst
import nl.info.zac.gebruikersvoorkeuren.model.Zoekopdracht
import nl.info.zac.gebruikersvoorkeuren.model.ZoekopdrachtListParameters
import nl.info.zac.gebruikersvoorkeuren.exception.ZoekopdrachtWithSameNameAlreadyExistsException
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Locale

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class GebruikersvoorkeurenService @Inject constructor(
    private val entityManager: EntityManager,
    private val signaleringService: SignaleringService
) {
    fun createZoekopdracht(zoekopdracht: Zoekopdracht): Zoekopdracht {
        if (zoekopdracht.id != null) {
            return entityManager.merge(zoekopdracht)
        }
        if (existsZoekopdracht(zoekopdracht)) {
            throw ZoekopdrachtWithSameNameAlreadyExistsException(
                "Er bestaat al een zoekopdracht met naam '${zoekopdracht.naam}' en lijst '${zoekopdracht.lijstID}' " +
                    "en medewerker: '${zoekopdracht.medewerkerID}'"
            )
        }
        zoekopdracht.isActief = true
        entityManager.persist(zoekopdracht)
        setActief(zoekopdracht)
        return zoekopdracht
    }

    fun findZoekopdracht(id: Long): Zoekopdracht? = entityManager.find(Zoekopdracht::class.java, id)

    @Suppress("SpreadOperator")
    fun listZoekopdrachten(listParameters: ZoekopdrachtListParameters): List<Zoekopdracht> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Zoekopdracht::class.java)
        val root = query.from(Zoekopdracht::class.java)
        val predicates = mutableListOf(builder.equal(root.get<Werklijst>(Zoekopdracht.LIJST_ID), listParameters.lijstID))
        if (listParameters.medewerkerID.isNotBlank()) {
            predicates.add(builder.equal(root.get<String>(Zoekopdracht.MEDEWERKER_ID), listParameters.medewerkerID))
        }
        query.where(builder.and(*predicates.toTypedArray()))
        return entityManager.createQuery(query).resultList
    }

    @Suppress("SpreadOperator")
    fun readTabelInstellingen(lijstID: Werklijst, medewerkerID: String): TabelInstellingen {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(TabelInstellingen::class.java)
        val root = query.from(TabelInstellingen::class.java)
        val predicates = arrayOf(
            builder.equal(root.get<Werklijst>(TabelInstellingen::lijstID.name), lijstID),
            builder.equal(root.get<String>(TabelInstellingen::medewerkerID.name), medewerkerID)
        )
        query.where(builder.and(*predicates))
        return entityManager.createQuery(query).resultList.firstOrNull() ?: TabelInstellingen().apply {
            this.lijstID = lijstID
            this.medewerkerID = medewerkerID
            aantalPerPagina = TabelInstellingen.AANTAL_PER_PAGINA_DEFAULT
        }
    }

    fun updateTabelInstellingen(tabelInstellingen: TabelInstellingen) {
        val bestaandeTabelInstellingen = readTabelInstellingen(tabelInstellingen.lijstID, tabelInstellingen.medewerkerID)
        bestaandeTabelInstellingen.aantalPerPagina = tabelInstellingen.aantalPerPagina
        entityManager.merge(bestaandeTabelInstellingen)
    }

    @Suppress("SpreadOperator")
    private fun existsZoekopdracht(zoekopdracht: Zoekopdracht): Boolean {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Zoekopdracht::class.java)
        val root = query.from(Zoekopdracht::class.java)
        val predicates = arrayOf(
            builder.equal(root.get<Werklijst>(Zoekopdracht.LIJST_ID), zoekopdracht.lijstID),
            builder.equal(root.get<String>(Zoekopdracht.MEDEWERKER_ID), zoekopdracht.medewerkerID),
            builder.equal(builder.lower(root.get(Zoekopdracht.NAAM)), zoekopdracht.naam.lowercase(Locale.ROOT))
        )
        query.where(builder.and(*predicates))
        return entityManager.createQuery(query).resultList.isNotEmpty()
    }

    fun deleteZoekopdracht(id: Long) {
        findZoekopdracht(id)?.let(entityManager::remove)
    }

    fun setActief(zoekopdracht: Zoekopdracht) {
        listZoekopdrachten(ZoekopdrachtListParameters(zoekopdracht.lijstID, zoekopdracht.medewerkerID)).forEach {
            val isActief = it.id == zoekopdracht.id
            if (it.isActief != isActief) {
                it.isActief = isActief
                entityManager.merge(it)
            }
        }
    }

    fun removeActief(zoekopdrachtListParameters: ZoekopdrachtListParameters) {
        listZoekopdrachten(zoekopdrachtListParameters).filter { it.isActief }.forEach {
            it.isActief = false
            entityManager.merge(it)
        }
    }

    fun listDashboardCards(medewerkerId: String): List<DashboardCardInstelling> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(DashboardCardInstelling::class.java)
        val root = query.from(DashboardCardInstelling::class.java)
        query.where(builder.and(builder.equal(root.get<String>("medewerkerId"), medewerkerId)))
        query.orderBy(builder.asc(root.get<Int>("volgorde")))
        return entityManager.createQuery(query).resultList
    }

    fun updateDashboardCards(medewerkerId: String, cards: List<DashboardCardInstelling>) {
        listDashboardCards(medewerkerId).forEach { existingCard ->
            val updatedCard = cards.firstOrNull { it.cardId == existingCard.cardId }
            if (updatedCard != null) {
                existingCard.kolom = updatedCard.kolom
                existingCard.volgorde = updatedCard.volgorde
                entityManager.persist(existingCard)
            } else {
                entityManager.remove(existingCard)
            }
        }
        cards.filter { it.id == null }.forEach {
            it.medewerkerId = medewerkerId
            entityManager.persist(it)
        }
    }

    fun addDashboardCard(medewerkerId: String, card: DashboardCardInstelling) {
        card.signaleringType?.let { signaleringType ->
            signaleringService.readInstellingenUser(signaleringType, medewerkerId).let {
                it.isDashboard = true
                signaleringService.createUpdateOrDeleteInstellingen(it)
            }
        }
        if (card.id == null) {
            card.medewerkerId = medewerkerId
            entityManager.persist(card)
        }
    }

    fun deleteDashboardCard(medewerkerId: String, card: DashboardCardInstelling) {
        card.signaleringType?.let { signaleringType ->
            signaleringService.readInstellingenUser(signaleringType, medewerkerId).let {
                it.isDashboard = false
                signaleringService.createUpdateOrDeleteInstellingen(it)
            }
        }
        card.id?.let {
            entityManager.remove(entityManager.find(DashboardCardInstelling::class.java, it))
        }
    }
}
