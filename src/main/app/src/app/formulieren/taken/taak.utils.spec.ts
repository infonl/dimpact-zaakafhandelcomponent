/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "../../../test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { mapTaskdataToTaskInformation } from "./taak.utils";

describe("mapTaskdataToTaskInformation", () => {
  it.each([
    {
      formulierDefinitieId: "DEFAULT_TAAKFORMULIER",
      uitkomstKey: "afhandeling",
    },
    { formulierDefinitieId: "GOEDKEUREN", uitkomstKey: "goedkeuren" },
    {
      formulierDefinitieId: "AANVULLENDE_INFORMATIE",
      uitkomstKey: "aanvullendeInformatie",
    },
    { formulierDefinitieId: "ADVIES", uitkomstKey: "advies" },
    {
      formulierDefinitieId: "EXTERN_ADVIES_VASTLEGGEN",
      uitkomstKey: "externAdvies",
    },
    { formulierDefinitieId: "EXTERN_ADVIES_MAIL", uitkomstKey: "externAdvies" },
  ] as const)(
    "maps uitkomst from the '$uitkomstKey' taakdata key for $formulierDefinitieId",
    ({ formulierDefinitieId, uitkomstKey }) => {
      const taak = fromPartial<GeneratedType<"RestTask">>({
        formulierDefinitieId,
      });

      expect(
        mapTaskdataToTaskInformation(
          {
            [uitkomstKey]: "de uitkomst",
            toelichting: "extra context",
            bijlagen: "doc-uuid-1",
          },
          taak,
        ),
      ).toEqual({
        uitkomst: "de uitkomst",
        bijlagen: "doc-uuid-1",
        opmerking: "extra context",
      });
    },
  );

  it("throws for an unknown formulierDefinitieId", () => {
    const taak = fromPartial<GeneratedType<"RestTask">>({
      formulierDefinitieId:
        "UNKNOWN" as GeneratedType<"RestTask">["formulierDefinitieId"],
    });

    expect(() => mapTaskdataToTaskInformation({}, taak)).toThrow(
      "Onbekend formulier: UNKNOWN",
    );
  });
});
