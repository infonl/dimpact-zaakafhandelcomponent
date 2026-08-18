/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum as DrcVertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.zrc.model.generated.VertrouwelijkheidaanduidingEnum as ZrcVertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum as ZtcVertrouwelijkheidaanduidingEnum

class RestVertrouwelijkheidaanduidingTest : BehaviorSpec({

    context("Converting ZRC VertrouwelijkheidaanduidingEnum to RestVertrouwelijkheidaanduiding") {
        given("all ZRC enum values") {
            `when`("each value is converted") {
                then("it should map to the matching RestVertrouwelijkheidaanduiding value") {
                    ZrcVertrouwelijkheidaanduidingEnum.entries.forEach { zrcValue ->
                        val restValue = zrcValue.toRestVertrouwelijkheidaanduiding()
                        restValue.name shouldBe zrcValue.name
                    }
                }
            }
        }
    }

    context("Converting ZTC VertrouwelijkheidaanduidingEnum to RestVertrouwelijkheidaanduiding") {
        given("all ZTC enum values") {
            `when`("each value is converted") {
                then("it should map to the matching RestVertrouwelijkheidaanduiding value") {
                    ZtcVertrouwelijkheidaanduidingEnum.entries.forEach { ztcValue ->
                        val restValue = ztcValue.toRestVertrouwelijkheidaanduiding()
                        restValue.name shouldBe ztcValue.name
                    }
                }
            }
        }
    }

    context("Converting DRC VertrouwelijkheidaanduidingEnum to RestVertrouwelijkheidaanduiding") {
        given("all DRC enum values except EMPTY") {
            `when`("each value is converted") {
                then("it should map to the matching RestVertrouwelijkheidaanduiding value") {
                    DrcVertrouwelijkheidaanduidingEnum.entries
                        .filter { it != DrcVertrouwelijkheidaanduidingEnum.EMPTY }
                        .forEach { drcValue ->
                            val restValue = drcValue.toRestVertrouwelijkheidaanduiding()
                            restValue?.name shouldBe drcValue.name
                        }
                }
            }
        }

        given("the DRC EMPTY value") {
            `when`("it is converted") {
                then("it should return null") {
                    DrcVertrouwelijkheidaanduidingEnum.EMPTY.toRestVertrouwelijkheidaanduiding() shouldBe null
                }
            }
        }
    }

    context("Converting RestVertrouwelijkheidaanduiding to DRC VertrouwelijkheidaanduidingEnum") {
        given("all RestVertrouwelijkheidaanduiding values") {
            `when`("each value is converted") {
                then("it should map to the matching DRC enum value") {
                    RestVertrouwelijkheidaanduiding.entries.forEach { restValue ->
                        val drcValue = restValue.toDrcVertrouwelijkheidaanduidingEnum()
                        drcValue.name shouldBe restValue.name
                    }
                }
            }
        }

        given("a null RestVertrouwelijkheidaanduiding") {
            `when`("it is converted") {
                then("it should return DRC EMPTY") {
                    val nullValue: RestVertrouwelijkheidaanduiding? = null
                    nullValue.toDrcVertrouwelijkheidaanduidingEnum() shouldBe DrcVertrouwelijkheidaanduidingEnum.EMPTY
                }
            }
        }
    }

    context("Converting RestVertrouwelijkheidaanduiding to ZRC VertrouwelijkheidaanduidingEnum") {
        given("all RestVertrouwelijkheidaanduiding values") {
            `when`("each value is converted") {
                then("it should map to the matching ZRC enum value") {
                    RestVertrouwelijkheidaanduiding.entries.forEach { restValue ->
                        val zrcValue = restValue.toZrcVertrouwelijkheidaanduidingEnum()
                        zrcValue.name shouldBe restValue.name
                    }
                }
            }
        }
    }

    context("RestVertrouwelijkheidaanduiding completeness") {
        given("all ZRC enum values") {
            then("RestVertrouwelijkheidaanduiding should have the same number of values") {
                RestVertrouwelijkheidaanduiding.entries.size shouldBe ZrcVertrouwelijkheidaanduidingEnum.entries.size
            }
        }

        given("all ZTC enum values") {
            then("RestVertrouwelijkheidaanduiding should have the same number of values") {
                RestVertrouwelijkheidaanduiding.entries.size shouldBe ZtcVertrouwelijkheidaanduidingEnum.entries.size
            }
        }

        given("all DRC enum values") {
            then("RestVertrouwelijkheidaanduiding should have one less value than DRC (no EMPTY)") {
                RestVertrouwelijkheidaanduiding.entries.size shouldBe DrcVertrouwelijkheidaanduidingEnum.entries.size - 1
            }
        }
    }
})
