package net.atos.zac.app.signaleringen.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.TaakVariabelenService
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl
import java.time.Month
import java.util.Calendar

class RESTSignaleringTaakConverterTest : BehaviorSpec({
    val taakVariabelenService = mockk<TaakVariabelenService>()
    val restSignaleringTaakConverter = RESTSignaleringTaakConverter(taakVariabelenService)

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("A converter history with USER_TASK_CREATED item") {
        val zaakIdentificatie = "my-zaak-identificatie"
        val zaaktypeOmschrijving = "my-zaaktype-omschrijving"
        every {
            taakVariabelenService.readZaakIdentificatie(any())
        } returns zaakIdentificatie
        every {
            taakVariabelenService.readZaaktypeOmschrijving(any())
        } returns zaaktypeOmschrijving
        val task = TaskEntityImpl()
        task.id = "my-id"
        task.name = "my-name"
        val cal = Calendar.getInstance()
        cal[Calendar.YEAR] = 1988
        cal[Calendar.MONTH] = Calendar.JANUARY
        cal[Calendar.DAY_OF_MONTH] = 1
        task.createTime = cal.time

        When("convert is called") {
            val summary = restSignaleringTaakConverter.convert(task)

            Then("it returns correct history lines") {
                with(summary) {
                    id shouldBe task.id
                    naam shouldBe task.name
                    zaakIdentificatie shouldBe zaakIdentificatie
                    zaaktypeOmschrijving shouldBe zaaktypeOmschrijving
                    with(creatiedatumTijd) {
                        year shouldBe 1988
                        month shouldBe Month.JANUARY
                        dayOfMonth shouldBe 1
                    }
                }
            }
        }
    }
})
