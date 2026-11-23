/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

private const val LOG_PACKAGE = "nl.info.zac.log.."
private const val LOG_FILTER_PACKAGE = "nl.info.zac.logfilter.."
private val ZAC_PACKAGES = arrayOf("nl.info.zac..", "nl.info.client..", "net.atos..")

private val printStackTraceOfAThrowable = DescribedPredicate.describe<JavaMethodCall>(
    "printStackTrace() of a Throwable"
) { it.name == "printStackTrace" && it.targetOwner.isAssignableTo(Throwable::class.java) }

/**
 * All logging of ZAC goes through ZacLogger in the log package; see ADR-0020.
 */
internal object LoggingArchitectureRules {
    val noOtherLoggingApi: ArchRule = noClasses()
        .that().resideInAnyPackage(*ZAC_PACKAGES)
        .and().resideOutsideOfPackages(LOG_PACKAGE, LOG_FILTER_PACKAGE)
        .should().dependOnClassesThat().haveFullyQualifiedName(java.util.logging.Logger::class.java.name)
        .orShould().dependOnClassesThat().haveFullyQualifiedName(System.Logger::class.java.name)
        .orShould().dependOnClassesThat().resideInAnyPackage(
            "org.slf4j..",
            "org.jboss.logging..",
            "org.apache.logging.log4j..",
            "org.apache.log4j..",
            "org.apache.commons.logging..",
            "io.github.oshai.kotlinlogging..",
            "mu.."
        )
        .because("all logging goes through ZacLogger, which accepts no free text")

    val noConsoleOutput: ArchRule = noClasses()
        .that().resideInAnyPackage(*ZAC_PACKAGES)
        .and().resideOutsideOfPackages(LOG_PACKAGE, LOG_FILTER_PACKAGE)
        .should().accessField(System::class.java, "out")
        .orShould().accessField(System::class.java, "err")
        .orShould().callMethodWhere(printStackTraceOfAThrowable)
        .because("all logging goes through ZacLogger, which accepts no free text")
}
