/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../utils/generated-types";
import { PersoonIndicatiesComponent } from "./persoon-indicaties.component";

const persoonIndicatieMetadata: {
  indicatie: GeneratedType<"RestPersoonIndicaties">;
  expectedIcon: string;
}[] = [
  {
    indicatie: "GEHEIMHOUDING_OP_PERSOONSGEGEVENS",
    expectedIcon: "passkey",
  },
  { indicatie: "NIET_INGEZETENE", expectedIcon: "person_off" },
  { indicatie: "IN_ONDERZOEK", expectedIcon: "person_search" },
  { indicatie: "ONDER_CURATELE", expectedIcon: "account_child_invert" },
  { indicatie: "OPSCHORTING_BIJHOUDING", expectedIcon: "person_alert" },
  { indicatie: "OVERLEDEN", expectedIcon: "deceased" },
  { indicatie: "MINISTERIELE_REGELING", expectedIcon: "order_approve" },
  { indicatie: "EMIGRATIE", expectedIcon: "travel" },
];

describe(PersoonIndicatiesComponent.name, () => {
  let component: PersoonIndicatiesComponent;

  beforeEach(() => {
    component = new PersoonIndicatiesComponent();
  });

  it("toont geen indicaties wanneer persoon geen indicaties heeft", () => {
    component.persoon = {
      indicaties: [],
    } as unknown as GeneratedType<"RestPersoon">;
    component.ngOnInit();

    expect(component.indicaties).toHaveLength(0);
  });

  it.each(persoonIndicatieMetadata)(
    "$indicatie → icon '$expectedIcon', primary=true",
    ({ indicatie, expectedIcon }) => {
      component.persoon = {
        indicaties: [indicatie],
      } as unknown as GeneratedType<"RestPersoon">;
      component.ngOnInit();

      expect(component.indicaties).toHaveLength(1);
      expect(component.indicaties[0].naam).toBe(indicatie);
      expect(component.indicaties[0].icon).toBe(expectedIcon);
      expect(component.indicaties[0].primary).toBe(true);
    },
  );

  it("ngOnChanges herberekent indicaties bij gewijzigd persoon", () => {
    component.persoon = {
      indicaties: ["NIET_INGEZETENE"],
    } as unknown as GeneratedType<"RestPersoon">;
    component.ngOnChanges({});

    expect(component.indicaties).toHaveLength(1);
    expect(component.indicaties[0].naam).toBe("NIET_INGEZETENE");
  });

  it("meerdere indicaties worden allemaal weergegeven", () => {
    component.persoon = {
      indicaties: ["GEHEIMHOUDING_OP_PERSOONSGEGEVENS", "OVERLEDEN"],
    } as unknown as GeneratedType<"RestPersoon">;
    component.ngOnInit();

    expect(component.indicaties.map((i) => i.naam)).toEqual([
      "GEHEIMHOUDING_OP_PERSOONSGEGEVENS",
      "OVERLEDEN",
    ]);
  });
});
