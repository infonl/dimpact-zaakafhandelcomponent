/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.admin.model.RestInrichtingscheckParameters
import nl.info.zac.app.admin.model.RestInrichtingscheckSortField
import nl.info.zac.app.admin.model.RestInrichtingscheckStatus
import nl.info.zac.app.admin.model.RestInrichtingscheckZaaktype
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.healthcheck.createBuildInformation
import nl.info.zac.healthcheck.createZaaktypeInrichtingscheck
import nl.info.zac.healthcheck.model.ZaaktypeInrichtingscheck
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.time.ZacTime
import java.time.LocalDate

class HealthCheckRestServiceTest : BehaviorSpec({
    val healthCheckService = mockk<HealthCheckService>()
    val policyService = mockk<PolicyService>()
    val healthCheckRestService = HealthCheckRestService(
        healthCheckService = healthCheckService,
        policyService = policyService
    )

    fun givenCheckedZaaktypes(zaaktypeInrichtingschecks: List<ZaaktypeInrichtingscheck>) {
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { healthCheckService.controleerZaaktypes() } returns zaaktypeInrichtingschecks
    }

    fun createValidZaaktypeInrichtingschecks(count: Int, omschrijvingPrefix: String = "fakeZaaktype") =
        (1..count).map {
            createZaaktypeInrichtingscheck(
                zaaktype = createZaakType(
                    omschrijving = "$omschrijvingPrefix ${it.toString().padStart(3, '0')}",
                    beginGeldigheid = LocalDate.of(2020, 1, 1).plusDays(it.toLong())
                )
            )
        }

    fun createInvalidZaaktypeInrichtingscheck(zaaktype: ZaakType = createZaakType()) =
        createZaaktypeInrichtingscheck(zaaktype = zaaktype, statustypeIntakeAanwezig = false)

    fun createZaaktypeInrichtingscheckWithWaarschuwing(zaaktype: ZaakType = createZaakType()) =
        createZaaktypeInrichtingscheck(zaaktype = zaaktype, zaakspecifiekeAutorisatieEigenschapAanwezig = true)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Listing the inrichtingschecks of the zaaktypes") {
        given("A zaaktype with a doel and a begin geldigheid, whose check reports a finding for every check") {
            val beginGeldigheid = ZacTime.today().minusDays(10)
            val zaaktype = createZaakType(
                omschrijving = "fakeOmschrijving",
                doel = "fakeDoel",
                beginGeldigheid = beginGeldigheid
            )
            val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(
                zaaktype = zaaktype,
                statustypeIntakeAanwezig = false,
                statustypeInBehandelingAanwezig = false,
                statustypeHeropendAanwezig = false,
                statustypeAfgerondAanwezig = false,
                statustypeAfgerondLaatsteVolgnummer = false,
                statustypeAanvullendeInformatieVereist = false,
                initiatorrollen = 2,
                behandelaarrollen = 3,
                rolOverigeAanwezig = false,
                informatieobjecttypeEmailAanwezig = false,
                resultaattypeAanwezig = false,
                zaakafhandelParametersValide = false,
                besluittypeAanwezig = false,
                brpInstellingenCorrect = false,
                zaakspecifiekeAutorisatieEigenschapAanwezig = true,
                zaakspecifiekeAutorisatieRoltypeAanwezig = false
            ).apply {
                addResultaattypesMetVerplichtBesluit("fakeResultaattypeMetVerplichtBesluit")
                addResultaattypesMetNietOndersteundeBrondatum("fakeResultaattypeMetNietOndersteundeBrondatum")
            }
            givenCheckedZaaktypes(listOf(zaaktypeInrichtingscheck))

            `when`("the inrichtingschecks are listed") {
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters()
                )

                then("the zaaktype of the check carries only the uuid, omschrijving, doel and begin geldigheid") {
                    restResultaat.results.single().zaaktype shouldBe RestInrichtingscheckZaaktype(
                        uuid = zaaktype.url.extractUuid(),
                        omschrijving = "fakeOmschrijving",
                        doel = "fakeDoel",
                        beginGeldigheid = beginGeldigheid
                    )
                }
                and("every check result is copied") {
                    with(restResultaat.results.single()) {
                        isStatustypeIntakeAanwezig shouldBe false
                        isStatustypeInBehandelingAanwezig shouldBe false
                        isStatustypeHeropendAanwezig shouldBe false
                        isStatustypeAfgerondAanwezig shouldBe false
                        isStatustypeAfgerondLaatsteVolgnummer shouldBe false
                        isStatustypeAanvullendeInformatieVereist shouldBe false
                        aantalInitiatorroltypen shouldBe 2
                        aantalBehandelaarroltypen shouldBe 3
                        isRolOverigeAanwezig shouldBe false
                        isInformatieobjecttypeEmailAanwezig shouldBe false
                        isResultaattypeAanwezig shouldBe false
                        isZaakafhandelParametersValide shouldBe false
                        isBesluittypeAanwezig shouldBe false
                        isBrpInstellingenCorrect shouldBe false
                        isZaakspecifiekeAutorisatieEigenschapAanwezig shouldBe true
                        isZaakspecifiekeAutorisatieRoltypeAanwezig shouldBe false
                        resultaattypesMetVerplichtBesluit shouldBe listOf("fakeResultaattypeMetVerplichtBesluit")
                        resultaattypesMetNietOndersteundeBrondatum shouldBe
                            listOf("fakeResultaattypeMetNietOndersteundeBrondatum")
                        hasWaarschuwingen shouldBe true
                        isValide shouldBe false
                    }
                }
            }
        }

        given("A CMMN zaaktype without a resultaattype") {
            val zaaktypeInrichtingscheck = createZaaktypeInrichtingscheck(resultaattypeAanwezig = false)
            givenCheckedZaaktypes(listOf(zaaktypeInrichtingscheck))

            `when`("the inrichtingschecks are listed") {
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters()
                )

                then("its item reports that the zaaktype is not valid and that no resultaattype is present") {
                    with(restResultaat.results.single()) {
                        isValide shouldBe false
                        isResultaattypeAanwezig shouldBe false
                    }
                }
            }
        }

        given("No zaaktypes are available") {
            givenCheckedZaaktypes(emptyList())

            `when`("the inrichtingschecks are listed") {
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters()
                )

                then("the page is empty and the total is 0") {
                    restResultaat.resultCount shouldBe 0
                    restResultaat.results.shouldBeEmpty()
                }
            }
        }

        given("The user does not have the right to beheren") {
            every { policyService.readOverigeRechten().canBeheren } returns false

            `when`("the inrichtingschecks are listed") {
                val policyException = shouldThrow<PolicyException> {
                    healthCheckRestService.listZaaktypeInrichtingschecks(RestInrichtingscheckParameters())
                }

                then("the request is refused") {
                    policyException shouldNotBe null
                }
            }
        }
    }

    context("Reading the inrichtingscheck list in pages") {
        given("60 checked zaaktypes in the default catalogue") {
            val zaaktypeInrichtingschecks = createValidZaaktypeInrichtingschecks(60)

            `when`("the list is requested with the default parameters") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters()
                )

                then("the response has totaal 60 and the first 25 zaaktypes") {
                    restResultaat.resultCount shouldBe 60
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        zaaktypeInrichtingschecks.take(25).map { it.zaaktype.omschrijving }
                }
            }

            `when`("page 2 is requested with 25 rows") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(page = 2, rows = 25)
                )

                then("the response has totaal 60 and the last 10 zaaktypes") {
                    restResultaat.resultCount shouldBe 60
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        zaaktypeInrichtingschecks.takeLast(10).map { it.zaaktype.omschrijving }
                }
            }

            `when`("page 5 is requested with 25 rows") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(page = 5, rows = 25)
                )

                then("the response has totaal 60 and no zaaktypes") {
                    restResultaat.resultCount shouldBe 60
                    restResultaat.results.shouldBeEmpty()
                }
            }
        }
    }

    context("Filtering the inrichtingscheck list before it is paged") {
        given("The checked zaaktypes 'Melding openbare ruimte', 'Melding evenement' and 'Aanvraag vergunning'") {
            val zaaktypeInrichtingschecks = listOf(
                "Melding openbare ruimte",
                "Melding evenement",
                "Aanvraag vergunning"
            ).map { createZaaktypeInrichtingscheck(zaaktype = createZaakType(omschrijving = it)) }
            givenCheckedZaaktypes(zaaktypeInrichtingschecks)

            `when`("the list is requested with the zoekterm 'MELDING ' in another case and with white space") {
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(zoekterm = "MELDING ")
                )

                then("the two melding zaaktypes are returned in order of omschrijving and totaal is 2") {
                    restResultaat.resultCount shouldBe 2
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        listOf("Melding evenement", "Melding openbare ruimte")
                }
            }
        }

        given("The checked zaaktype 'Aanvraag vergunning' with a doel about an evenement and 'Melding openbare ruimte'") {
            val zaaktypeInrichtingschecks = listOf(
                createZaakType(omschrijving = "Aanvraag vergunning", doel = "Vergunning voor een evenement"),
                createZaakType(omschrijving = "Melding openbare ruimte", doel = "Melding van een gebrek")
            ).map { createZaaktypeInrichtingscheck(zaaktype = it) }
            givenCheckedZaaktypes(zaaktypeInrichtingschecks)

            `when`("the list is requested with the zoekterm 'evenement'") {
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(zoekterm = "evenement")
                )

                then("only the zaaktype whose doel contains the zoekterm is returned and totaal is 1") {
                    restResultaat.resultCount shouldBe 1
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        listOf("Aanvraag vergunning")
                }
            }
        }

        given("A valid zaaktype without warnings, a valid zaaktype with a warning and a zaaktype that is not valid") {
            val validZaaktype = createZaakType(omschrijving = "fakeValid")
            val zaaktypeWithWaarschuwing = createZaakType(omschrijving = "fakeWithWaarschuwing")
            val invalidZaaktype = createZaakType(omschrijving = "fakeInvalid")
            val zaaktypeInrichtingschecks = listOf(
                createZaaktypeInrichtingscheck(zaaktype = validZaaktype),
                createZaaktypeInrichtingscheckWithWaarschuwing(zaaktype = zaaktypeWithWaarschuwing),
                createInvalidZaaktypeInrichtingscheck(zaaktype = invalidZaaktype)
            )

            `when`("the list is requested with status AANDACHT_NODIG") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(status = RestInrichtingscheckStatus.AANDACHT_NODIG)
                )

                then("the zaaktype with the warning and the zaaktype that is not valid are returned") {
                    restResultaat.resultCount shouldBe 2
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        listOf("fakeInvalid", "fakeWithWaarschuwing")
                }
            }

            `when`("the list is requested with status IN_ORDE") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(status = RestInrichtingscheckStatus.IN_ORDE)
                )

                then("only the valid zaaktype without warnings is returned") {
                    restResultaat.resultCount shouldBe 1
                    restResultaat.results.single().zaaktype.omschrijving shouldBe "fakeValid"
                }
            }

            `when`("the list is requested without a status") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(status = null)
                )

                then("all three zaaktypes are returned") {
                    restResultaat.resultCount shouldBe 3
                }
            }
        }

        given("30 zaaktypes that need attention and 10 that are in order") {
            val zaaktypeInrichtingschecks = createValidZaaktypeInrichtingschecks(10, "fakeInOrde") +
                (1..30).map {
                    createInvalidZaaktypeInrichtingscheck(
                        zaaktype = createZaakType(omschrijving = "fakeAandachtNodig ${it.toString().padStart(2, '0')}")
                    )
                }
            givenCheckedZaaktypes(zaaktypeInrichtingschecks)

            `when`("the second page of 25 is requested with status AANDACHT_NODIG") {
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(page = 1, status = RestInrichtingscheckStatus.AANDACHT_NODIG)
                )

                then("totaal counts after the filter and the page holds the last 5 zaaktypes that need attention") {
                    restResultaat.resultCount shouldBe 30
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        (26..30).map { "fakeAandachtNodig $it" }
                }
            }
        }
    }

    context("Sorting the inrichtingscheck list before it is paged") {
        given("30 checked zaaktypes with different begin geldigheid dates") {
            val zaaktypeInrichtingschecks = createValidZaaktypeInrichtingschecks(30).shuffled()

            `when`("page 0 and page 1 are requested with 25 rows, sorted on beginGeldigheid in descending order") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val parameters = RestInrichtingscheckParameters(
                    rows = 25,
                    sortField = RestInrichtingscheckSortField.BEGIN_GELDIGHEID,
                    sortOrder = SorteerRichting.DESC
                )
                val firstPage = healthCheckRestService.listZaaktypeInrichtingschecks(parameters.copy(page = 0))
                val secondPage = healthCheckRestService.listZaaktypeInrichtingschecks(parameters.copy(page = 1))

                then("the 30 zaaktypes over both pages are in descending order of begin geldigheid") {
                    val beginGeldigheidDates = (firstPage.results + secondPage.results).map {
                        it.zaaktype.beginGeldigheid
                    }
                    beginGeldigheidDates shouldHaveSize 30
                    beginGeldigheidDates shouldContainExactly beginGeldigheidDates.sortedDescending()
                }
            }
        }

        given("Checked zaaktypes with different omschrijvingen, doelen and validity") {
            val zaaktypeInrichtingschecks = listOf(
                createZaaktypeInrichtingscheck(zaaktype = createZaakType(omschrijving = "fakeB", doel = "fakeDoelA")),
                createInvalidZaaktypeInrichtingscheck(
                    zaaktype = createZaakType(omschrijving = "fakeC", doel = "fakeDoelC")
                ),
                createZaaktypeInrichtingscheck(zaaktype = createZaakType(omschrijving = "fakeA", doel = "fakeDoelB"))
            )

            `when`("the list is requested without sortField and sortOrder") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters()
                )

                then("the zaaktypes are in ascending order of omschrijving") {
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        listOf("fakeA", "fakeB", "fakeC")
                }
            }

            `when`("the list is sorted on doel in descending order") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(
                        sortField = RestInrichtingscheckSortField.DOEL,
                        sortOrder = SorteerRichting.DESC
                    )
                )

                then("the zaaktypes are in descending order of doel") {
                    restResultaat.results.map { it.zaaktype.doel } shouldContainExactly
                        listOf("fakeDoelC", "fakeDoelB", "fakeDoelA")
                }
            }

            `when`("the list is sorted on valide in ascending order") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(sortField = RestInrichtingscheckSortField.VALIDE)
                )

                then("the zaaktype that is not valid comes first, then the valid ones by omschrijving") {
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        listOf("fakeC", "fakeA", "fakeB")
                }
            }
        }

        given("Checked zaaktypes whose omschrijvingen and doelen differ in case and accents") {
            val zaaktypeInrichtingschecks = listOf("zebra", "Éénmalige subsidie", "beheer", "Aanvraag", "Ölie").map {
                createZaaktypeInrichtingscheck(zaaktype = createZaakType(omschrijving = it, doel = it))
            }

            `when`("the list is requested without sortField and sortOrder") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters()
                )

                then("the zaaktypes are in Dutch alphabetical order of omschrijving, with the accented letters among the others") {
                    restResultaat.results.map { it.zaaktype.omschrijving } shouldContainExactly
                        listOf("Aanvraag", "beheer", "Éénmalige subsidie", "Ölie", "zebra")
                }
            }

            `when`("the list is sorted on doel in ascending order") {
                givenCheckedZaaktypes(zaaktypeInrichtingschecks)
                val restResultaat = healthCheckRestService.listZaaktypeInrichtingschecks(
                    RestInrichtingscheckParameters(sortField = RestInrichtingscheckSortField.DOEL)
                )

                then("the zaaktypes are in Dutch alphabetical order of doel") {
                    restResultaat.results.map { it.zaaktype.doel } shouldContainExactly
                        listOf("Aanvraag", "beheer", "Éénmalige subsidie", "Ölie", "zebra")
                }
            }
        }
    }

    context("Reading the reference-table check") {
        listOf(true, false).forEach { isCommunicatiekanaalEformulierAanwezig ->
            given("The E-formulier communicatiekanaal is ${if (isCommunicatiekanaalEformulierAanwezig) "present" else "missing"}") {
                every { policyService.readOverigeRechten().canBeheren } returns true
                every {
                    healthCheckService.bestaatCommunicatiekanaalEformulier()
                } returns isCommunicatiekanaalEformulierAanwezig

                `when`("the referentietabellen resource is read") {
                    val restReferentietabellenInrichtingscheck =
                        healthCheckRestService.readReferentietabellenInrichtingscheck()

                    then("isCommunicatiekanaalEformulierAanwezig is $isCommunicatiekanaalEformulierAanwezig") {
                        restReferentietabellenInrichtingscheck.isCommunicatiekanaalEformulierAanwezig shouldBe
                            isCommunicatiekanaalEformulierAanwezig
                    }
                }
            }
        }

        given("The user does not have the right to beheren") {
            every { policyService.readOverigeRechten().canBeheren } returns false

            `when`("the referentietabellen resource is read") {
                val policyException = shouldThrow<PolicyException> {
                    healthCheckRestService.readReferentietabellenInrichtingscheck()
                }

                then("the request is refused") {
                    policyException shouldNotBe null
                }
            }
        }
    }

    context("Reading the build information") {
        given("build information is available") {
            val now = ZacTime.now()
            val buildInformation = createBuildInformation(
                commit = "fakeCommit",
                buildId = "fakeCommit",
                buildDatumTijd = now,
                versienummer = "fakeVersionNumber"
            )
            every { healthCheckService.readBuildInformatie() } returns buildInformation

            `when`("readBuildInformatie is called") {
                val result = healthCheckRestService.readBuildInformatie()

                then("it should return the correct build information object") {
                    result.commit shouldBe "fakeCommit"
                    result.buildId shouldBe "fakeCommit"
                    result.buildDatumTijd shouldBe now
                    result.versienummer shouldBe "fakeVersionNumber"
                }
            }
        }
    }
})
