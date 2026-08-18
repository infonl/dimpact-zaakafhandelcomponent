/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.model.generated.ArchiefstatusEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum

class ZaakListParametersTest : BehaviorSpec({
    afterEach { checkUnnecessaryStub() }

    given("a ZaakListParameters with archiefnominatie set") {
        val zaakListParameters = ZaakListParameters().apply {
            setArchiefnominatie(ArchiefnominatieEnum.BLIJVEND_BEWAREN)
        }

        `when`("getArchiefnominatie is called") {
            then("it returns the enum's toString") {
                zaakListParameters.archiefnominatie shouldBe ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString()
            }
        }
    }

    given("a ZaakListParameters without archiefnominatie set") {
        val zaakListParameters = ZaakListParameters()

        `when`("getArchiefnominatie is called") {
            then("it returns null") {
                zaakListParameters.archiefnominatie shouldBe null
            }
        }
    }

    given("a ZaakListParameters with a non-empty archiefnominatieIn set") {
        val zaakListParameters = ZaakListParameters().apply {
            setArchiefnominatieIn(setOf(ArchiefnominatieEnum.BLIJVEND_BEWAREN, ArchiefnominatieEnum.VERNIETIGEN))
        }

        `when`("getArchiefnominatieIn is called") {
            val result = zaakListParameters.archiefnominatieIn

            then("it returns a comma-joined string of the enum values") {
                result?.split(",")?.toSet() shouldBe setOf(
                    ArchiefnominatieEnum.BLIJVEND_BEWAREN.toString(),
                    ArchiefnominatieEnum.VERNIETIGEN.toString()
                )
            }
        }
    }

    given("a ZaakListParameters with an empty archiefnominatieIn set") {
        val zaakListParameters = ZaakListParameters().apply {
            setArchiefnominatieIn(emptySet())
        }

        `when`("getArchiefnominatieIn is called") {
            then("it returns null") {
                zaakListParameters.archiefnominatieIn shouldBe null
            }
        }
    }

    given("a ZaakListParameters without archiefstatus set") {
        val zaakListParameters = ZaakListParameters()

        `when`("getArchiefstatus is called") {
            then("it returns null") {
                zaakListParameters.archiefstatus shouldBe null
            }
        }
    }

    given("a ZaakListParameters with archiefstatus set") {
        val zaakListParameters = ZaakListParameters().apply {
            setArchiefstatus(ArchiefstatusEnum.GEARCHIVEERD)
        }

        `when`("getArchiefstatus is called") {
            then("it returns the enum's toString") {
                zaakListParameters.archiefstatus shouldBe ArchiefstatusEnum.GEARCHIVEERD.toString()
            }
        }
    }

    given("a ZaakListParameters with a non-empty archiefstatusIn set") {
        val zaakListParameters = ZaakListParameters().apply {
            setArchiefstatusIn(setOf(ArchiefstatusEnum.GEARCHIVEERD))
        }

        `when`("getArchiefstatusIn is called") {
            then("it returns a comma-joined string of the enum values") {
                zaakListParameters.archiefstatusIn shouldBe ArchiefstatusEnum.GEARCHIVEERD.toString()
            }
        }
    }

    given("a ZaakListParameters with an empty archiefstatusIn set") {
        val zaakListParameters = ZaakListParameters().apply {
            setArchiefstatusIn(emptySet())
        }

        `when`("getArchiefstatusIn is called") {
            then("it returns null") {
                zaakListParameters.archiefstatusIn shouldBe null
            }
        }
    }

    given("a ZaakListParameters without rolBetrokkeneType set") {
        val zaakListParameters = ZaakListParameters()

        `when`("getRolBetrokkeneType is called") {
            then("it returns null") {
                zaakListParameters.rolBetrokkeneType shouldBe null
            }
        }
    }

    given("a ZaakListParameters with rolBetrokkeneType set") {
        val zaakListParameters = ZaakListParameters().apply {
            setRolBetrokkeneType(BetrokkeneTypeEnum.MEDEWERKER)
        }

        `when`("getRolBetrokkeneType is called") {
            then("it returns the enum's toString") {
                zaakListParameters.rolBetrokkeneType shouldBe BetrokkeneTypeEnum.MEDEWERKER.toString()
            }
        }
    }

    given("a ZaakListParameters without rolOmschrijvingGeneriek set") {
        val zaakListParameters = ZaakListParameters()

        `when`("getRolOmschrijvingGeneriek is called") {
            then("it returns null") {
                zaakListParameters.rolOmschrijvingGeneriek shouldBe null
            }
        }
    }

    given("a ZaakListParameters with rolOmschrijvingGeneriek set") {
        val zaakListParameters = ZaakListParameters().apply {
            setRolOmschrijvingGeneriek(OmschrijvingGeneriekEnum.BEHANDELAAR)
        }

        `when`("getRolOmschrijvingGeneriek is called") {
            then("it returns the lowercased enum name") {
                zaakListParameters.rolOmschrijvingGeneriek shouldBe "behandelaar"
            }
        }
    }

    given("a ZaakListParameters without maximaleVertrouwelijkheidaanduiding set") {
        val zaakListParameters = ZaakListParameters()

        `when`("getMaximaleVertrouwelijkheidaanduiding is called") {
            then("it returns null") {
                zaakListParameters.maximaleVertrouwelijkheidaanduiding shouldBe null
            }
        }
    }

    given("a ZaakListParameters with maximaleVertrouwelijkheidaanduiding set") {
        val zaakListParameters = ZaakListParameters().apply {
            setMaximaleVertrouwelijkheidaanduiding(VertrouwelijkheidaanduidingEnum.OPENBAAR)
        }

        `when`("getMaximaleVertrouwelijkheidaanduiding is called") {
            then("it returns the enum's toString") {
                zaakListParameters.maximaleVertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR.toString()
            }
        }
    }
})
