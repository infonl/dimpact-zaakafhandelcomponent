/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.zac.signalering.model.SignaleringInstellingen
import nl.info.zac.signalering.model.SignaleringInstellingenZoekParameters
import nl.info.zac.signalering.model.SignaleringTarget
import nl.info.zac.signalering.model.SignaleringType

class SignaleringPredicateHelperTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    context("Creating the where predicate for signalering instellingen") {
        given("search parameters for the dashboard and mail instellingen of a group") {
            val parameters = SignaleringInstellingenZoekParameters(SignaleringTarget.GROUP, "fakeGroupId")
                .dashboard()
                .mail()
            val criteriaBuilder = mockk<CriteriaBuilder>()
            val root = mockk<Root<SignaleringInstellingen>>()
            val groepPath = mockk<Path<Any>>()
            val groepPredicate = mockk<Predicate>()
            every { root.get<Any>("groep") } returns groepPath
            every { criteriaBuilder.equal(groepPath, "fakeGroupId") } returns groepPredicate

            `when`("the where predicate is created") {
                val predicate = getSignaleringInstellingenWhere(parameters, criteriaBuilder, root)

                then("it only selects on the group, because dashboard and mail are applied after the defaults") {
                    predicate shouldBe groepPredicate
                }
            }
        }

        given("search parameters for one type of the instellingen of a user") {
            val parameters = SignaleringInstellingenZoekParameters(SignaleringTarget.USER, "fakeUsername")
                .type(SignaleringType.Type.TAAK_AFGEROND)
            val criteriaBuilder = mockk<CriteriaBuilder>()
            val root = mockk<Root<SignaleringInstellingen>>()
            val medewerkerPath = mockk<Path<Any>>()
            val typePath = mockk<Path<Any>>()
            val typeIdPath = mockk<Path<Any>>()
            val medewerkerPredicate = mockk<Predicate>()
            val typePredicate = mockk<Predicate>()
            val wherePredicate = mockk<Predicate>()
            every { root.get<Any>("medewerker") } returns medewerkerPath
            every { root.get<Any>("type") } returns typePath
            every { typePath.get<Any>("id") } returns typeIdPath
            every { criteriaBuilder.equal(medewerkerPath, "fakeUsername") } returns medewerkerPredicate
            every { criteriaBuilder.equal(typeIdPath, "TAAK_AFGEROND") } returns typePredicate
            every { criteriaBuilder.and(medewerkerPredicate, typePredicate) } returns wherePredicate

            `when`("the where predicate is created") {
                val predicate = getSignaleringInstellingenWhere(parameters, criteriaBuilder, root)

                then("it selects on the user and the type") {
                    predicate shouldBe wherePredicate
                }
            }
        }
    }
})
