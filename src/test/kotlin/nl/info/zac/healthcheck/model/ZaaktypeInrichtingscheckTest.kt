/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck

class ZaaktypeInrichtingscheckTest : BehaviorSpec({

    afterEach {
        checkUnnecessaryStub()
    }

    given("zaak type without one required status") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false
        )

        `when`("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            then("it is reported as invalid") {
                validity shouldBe false
            }
        }
    }

    given("zaak type without two required status") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false,
            statustypeHeropendAanwezig = false
        )

        `when`("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            then("it is reported as invalid") {
                validity shouldBe false
            }
        }
    }

    given("zaak type with all required statuses present") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck()

        `when`("health checks are performed") {
            val validity = zaaktypeInrichtingscheck.isValide

            then("it is reported as valid") {
                validity shouldBe true
            }
        }
    }

    given("a zaaktype with neither the zaakspecifieke autorisatie eigenschap nor the roltype") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            zaakspecifiekeAutorisatieEigenschapAanwezig = false,
            zaakspecifiekeAutorisatieRoltypeAanwezig = false
        )

        `when`("health checks are performed") {
            val heeftWaarschuwingen = zaaktypeInrichtingscheck.heeftWaarschuwingen

            then("no warning is reported because the zaaktype simply does not use zaakspecifieke autorisatie") {
                heeftWaarschuwingen shouldBe false
            }
            and("the zaakspecifieke autorisatie configuration is reported as complete") {
                zaaktypeInrichtingscheck.isZaakspecifiekeAutorisatieOnvolledig shouldBe false
            }
            and("it is reported as valid") {
                zaaktypeInrichtingscheck.isValide shouldBe true
            }
        }
    }

    given("a zaaktype with both the zaakspecifieke autorisatie eigenschap and the roltype") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            zaakspecifiekeAutorisatieEigenschapAanwezig = true,
            zaakspecifiekeAutorisatieRoltypeAanwezig = true
        )

        `when`("health checks are performed") {
            val heeftWaarschuwingen = zaaktypeInrichtingscheck.heeftWaarschuwingen

            then("no warning is reported because zaakspecifieke autorisatie is fully configured") {
                heeftWaarschuwingen shouldBe false
            }
            and("the zaakspecifieke autorisatie configuration is reported as complete") {
                zaaktypeInrichtingscheck.isZaakspecifiekeAutorisatieOnvolledig shouldBe false
            }
            and("it is reported as valid") {
                zaaktypeInrichtingscheck.isValide shouldBe true
            }
        }
    }

    given("a zaaktype with the zaakspecifieke autorisatie eigenschap but without the roltype") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            zaakspecifiekeAutorisatieEigenschapAanwezig = true,
            zaakspecifiekeAutorisatieRoltypeAanwezig = false
        )

        `when`("health checks are performed") {
            val heeftWaarschuwingen = zaaktypeInrichtingscheck.heeftWaarschuwingen

            then("a warning is reported") {
                heeftWaarschuwingen shouldBe true
            }
            and("the zaakspecifieke autorisatie configuration is reported as incomplete") {
                zaaktypeInrichtingscheck.isZaakspecifiekeAutorisatieOnvolledig shouldBe true
            }
            and("it is still reported as valid because a warning does not stop the zaaktype from being used") {
                zaaktypeInrichtingscheck.isValide shouldBe true
            }
        }
    }

    given("a zaaktype with the zaakspecifieke autorisatie roltype but without the eigenschap") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            zaakspecifiekeAutorisatieEigenschapAanwezig = false,
            zaakspecifiekeAutorisatieRoltypeAanwezig = true
        )

        `when`("health checks are performed") {
            val heeftWaarschuwingen = zaaktypeInrichtingscheck.heeftWaarschuwingen

            then("a warning is reported") {
                heeftWaarschuwingen shouldBe true
            }
            and("the zaakspecifieke autorisatie configuration is reported as incomplete") {
                zaaktypeInrichtingscheck.isZaakspecifiekeAutorisatieOnvolledig shouldBe true
            }
            and("it is still reported as valid because a warning does not stop the zaaktype from being used") {
                zaaktypeInrichtingscheck.isValide shouldBe true
            }
        }
    }

    given("an invalid zaaktype with the zaakspecifieke autorisatie roltype but without the eigenschap") {
        val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
            statustypeAanvullendeInformatieVereist = false,
            zaakspecifiekeAutorisatieEigenschapAanwezig = false,
            zaakspecifiekeAutorisatieRoltypeAanwezig = true
        )

        `when`("health checks are performed") {
            val heeftWaarschuwingen = zaaktypeInrichtingscheck.heeftWaarschuwingen

            then("a warning is reported") {
                heeftWaarschuwingen shouldBe true
            }
            and("it is reported as invalid for a reason unrelated to the warning") {
                zaaktypeInrichtingscheck.isValide shouldBe false
            }
        }
    }
})
