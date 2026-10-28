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
            val dashboardPath = mockk<Path<Boolean>>()
            val mailPath = mockk<Path<Boolean>>()
            val groepPredicate = mockk<Predicate>()
            val dashboardPredicate = mockk<Predicate>()
            val mailPredicate = mockk<Predicate>()
            val wherePredicate = mockk<Predicate>()
            every { root.get<Any>("groep") } returns groepPath
            every { root.get<Boolean>("isDashboard") } returns dashboardPath
            every { root.get<Boolean>("isMail") } returns mailPath
            every { criteriaBuilder.equal(groepPath, "fakeGroupId") } returns groepPredicate
            every { criteriaBuilder.isTrue(dashboardPath) } returns dashboardPredicate
            every { criteriaBuilder.isTrue(mailPath) } returns mailPredicate
            every {
                criteriaBuilder.and(groepPredicate, dashboardPredicate, mailPredicate)
            } returns wherePredicate

            `when`("the where predicate is created") {
                val predicate = getSignaleringInstellingenWhere(parameters, criteriaBuilder, root)

                then("it combines the group, dashboard and mail predicates") {
                    predicate shouldBe wherePredicate
                }
            }
        }
    }
})
