/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.util.JsonbUtil
import nl.info.zac.app.configuration.model.createTaal
import nl.info.zac.configuration.BrpConfiguration
import nl.info.zac.configuration.ConfigurationService

class ConfigurationRestServiceTest : BehaviorSpec({
    val configurationService = mockk<ConfigurationService>()
    val configurationRestService = ConfigurationRestService(configurationService)

    Given("PABC integration is enabled") {
        every { configurationService.featureFlagPabcIntegration() } returns true

        When("featureFlagPabcIntegration is called") {
            val result = configurationRestService.featureFlagPabcIntegration()

            Then("it should return true") {
                result shouldBe true
            }
        }
    }

    Given("PABC integration is disabled") {
        every { configurationService.featureFlagPabcIntegration() } returns false

        When("featureFlagPabcIntegration is called") {
            val result = configurationRestService.featureFlagPabcIntegration()

            Then("it should return false") {
                result shouldBe false
            }
        }
    }

    Given("Multiple languages are available") {
        val taal1 = createTaal(1L, "nl", "Nederlands", "Dutch", "nl_NL")
        val taal2 = createTaal(2L, "en", "Engels", "English", "en_US")
        val talen = listOf(taal1, taal2)

        every { configurationService.listTalen() } returns talen

        When("listTalen is called") {
            val result = configurationRestService.listTalen()

            Then("it should return a list of RestTaal objects") {
                result.size shouldBe 2
                result[0].id shouldBe "1"
                result[0].code shouldBe "nl"
                result[0].naam shouldBe "Nederlands"
                result[0].name shouldBe "Dutch"
                result[0].local shouldBe "nl_NL"
                result[1].id shouldBe "2"
                result[1].code shouldBe "en"
                result[1].naam shouldBe "Engels"
                result[1].name shouldBe "English"
                result[1].local shouldBe "en_US"
            }
        }
    }

    Given("No languages are available") {
        every { configurationService.listTalen() } returns emptyList()

        When("listTalen is called") {
            val result = configurationRestService.listTalen()

            Then("it should return an empty list") {
                result shouldBe emptyList()
            }
        }
    }

    Given("A default language is available") {
        val defaultTaal = createTaal(1L, "nl", "Nederlands", "Dutch", "nl_NL")
        every { configurationService.findDefaultTaal() } returns defaultTaal

        When("readDefaultTaal is called") {
            val result = configurationRestService.readDefaultTaal()

            Then("it should return the default RestTaal") {
                result?.id shouldBe "1"
                result?.code shouldBe "nl"
                result?.naam shouldBe "Nederlands"
                result?.name shouldBe "Dutch"
                result?.local shouldBe "nl_NL"
            }
        }
    }

    Given("No default language is available") {
        every { configurationService.findDefaultTaal() } returns null

        When("readDefaultTaal is called") {
            val result = configurationRestService.readDefaultTaal()

            Then("it should return null") {
                result shouldBe null
            }
        }
    }

    Given("A maximum file size is configured") {
        every { configurationService.readMaxFileSizeMB() } returns 999L

        When("readMaxFileSizeMB is called") {
            val result = configurationRestService.readMaxFileSizeMB()

            Then("it should return the configured file size") {
                result shouldBe 999L
            }
        }
    }

    Given("Additional allowed file types are configured") {
        val fileTypes = listOf("fakeFileType1", "fakeFileType2")
        every { configurationService.readAdditionalAllowedFileTypes() } returns fileTypes

        When("readAdditionalAllowedFileTypes is called") {
            val result = configurationRestService.readAdditionalAllowedFileTypes()

            Then("it should return the list of file types") {
                result shouldBe fileTypes
            }
        }
    }

    Given("No additional file types are configured") {
        every { configurationService.readAdditionalAllowedFileTypes() } returns emptyList()

        When("readAdditionalAllowedFileTypes is called") {
            val result = configurationRestService.readAdditionalAllowedFileTypes()

            Then("it should return an empty list") {
                result shouldBe emptyList()
            }
        }
    }

    Given("A gemeente code is configured") {
        val gemeenteCode = "fakeGemeenteCode"
        every { configurationService.readGemeenteCode() } returns gemeenteCode

        When("readGemeenteCode is called") {
            val result = configurationRestService.readGemeenteCode()

            Then("it should return the gemeente code as JSON") {
                result shouldBe JsonbUtil.JSONB.toJson(gemeenteCode)
            }
        }
    }

    Given("A gemeente name is configured") {
        val gemeenteNaam = "fakeGemeenteNaam"
        every { configurationService.readGemeenteNaam() } returns gemeenteNaam

        When("readGemeenteNaam is called") {
            val result = configurationRestService.readGemeenteNaam()

            Then("it should return the gemeente name as JSON") {
                result shouldBe JsonbUtil.JSONB.toJson(gemeenteNaam)
            }
        }
    }

    Given("BRP protocollering is enabled with iConnect provider") {
        val brpConfiguration = mockk<BrpConfiguration>()
        every { brpConfiguration.readBrpProtocolleringProvider() } returns "iConnect"
        every { configurationService.readBrpConfiguration() } returns brpConfiguration

        When("isBrpDoelbindingSetupEnabled is called") {
            val result = configurationRestService.readBrpDoelbindingSetupEnabled()

            Then("it should return true") {
                result shouldBe true
            }
        }
    }

    Given("BRP protocollering is enabled with 2Secure provider") {
        val brpConfiguration = mockk<BrpConfiguration>()
        every { brpConfiguration.readBrpProtocolleringProvider() } returns "2Secure"
        every { configurationService.readBrpConfiguration() } returns brpConfiguration

        When("isBrpDoelbindingSetupEnabled is called") {
            val result = configurationRestService.readBrpDoelbindingSetupEnabled()

            Then("it should return false") {
                result shouldBe false
            }
        }
    }

    Given("BRP protocollering is disabled") {
        val brpConfiguration = mockk<BrpConfiguration>()
        every { brpConfiguration.readBrpProtocolleringProvider() } returns ""
        every { configurationService.readBrpConfiguration() } returns brpConfiguration

        When("isBrpDoelbindingSetupEnabled is called") {
            val result = configurationRestService.readBrpDoelbindingSetupEnabled()

            Then("it should return false") {
                result shouldBe false
            }
        }
    }

    Given("BRP protocollering has an unknown provider") {
        val brpConfiguration = mockk<BrpConfiguration>()
        every { brpConfiguration.readBrpProtocolleringProvider() } returns "UnknownProvider"
        every { configurationService.readBrpConfiguration() } returns brpConfiguration

        When("isBrpDoelbindingSetupEnabled is called") {
            val result = configurationRestService.readBrpDoelbindingSetupEnabled()

            Then("it should return false") {
                result shouldBe false
            }
        }
    }
})
