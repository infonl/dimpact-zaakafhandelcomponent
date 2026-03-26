/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SimpleChange } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../utils/generated-types";
import { BesluitIndicatiesComponent } from "./besluit-indicaties.component";

describe(BesluitIndicatiesComponent.name, () => {
  let component: BesluitIndicatiesComponent;
  let translateInstant: jest.Mock;

  beforeEach(() => {
    translateInstant = jest.fn((key: string) => key);
    component = new BesluitIndicatiesComponent({
      instant: translateInstant,
    } as unknown as TranslateService);
  });

  it("toont geen indicaties wanneer besluit niet ingetrokken is", () => {
    component.ngOnChanges({
      besluit: new SimpleChange(
        undefined,
        { isIngetrokken: false } as GeneratedType<"RestDecision">,
        true,
      ),
    });

    expect(component.indicaties).toHaveLength(0);
  });

  it("toont INGETROKKEN indicatie met 'stop' icon wanneer besluit ingetrokken is", () => {
    component.ngOnChanges({
      besluit: new SimpleChange(
        undefined,
        {
          isIngetrokken: true,
          vervalreden: "INGETROKKEN_OVERIG",
        } as GeneratedType<"RestDecision">,
        true,
      ),
    });

    expect(component.indicaties).toHaveLength(1);
    expect(component.indicaties[0].naam).toBe("INGETROKKEN");
    expect(component.indicaties[0].icon).toBe("stop");
    expect(component.indicaties[0].primary).toBe(false);
  });

  it("toelichting bevat vertaalde vervalreden", () => {
    translateInstant.mockImplementation((key: string) => `vertaald:${key}`);

    component.ngOnChanges({
      besluit: new SimpleChange(
        undefined,
        {
          isIngetrokken: true,
          vervalreden: "INGETROKKEN_OVERIG",
        } as GeneratedType<"RestDecision">,
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      "vertaald:besluit.vervalreden.INGETROKKEN_OVERIG",
    );
  });
});
