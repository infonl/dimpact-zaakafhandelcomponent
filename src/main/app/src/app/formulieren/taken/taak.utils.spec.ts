/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormControl, FormGroup } from "@angular/forms";
import { fromPartial } from "../../../test-helpers";
import { toPickedDate } from "../../date-time";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  mapFormGroupToTaskData,
  mapTaskdataToTaskInformation,
} from "./taak.utils";

describe("mapTaskdataToTaskInformation", () => {
  const ALL_UITKOMST_VALUES = {
    afhandeling: "waarde voor afhandeling",
    goedkeuren: "waarde voor goedkeuren",
    aanvullendeInformatie: "waarde voor aanvullendeInformatie",
    advies: "waarde voor advies",
    externAdvies: "waarde voor externAdvies",
    verzonden: "waarde voor verzonden",
  } as const;

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
    {
      formulierDefinitieId: "DOCUMENT_VERZENDEN_POST",
      uitkomstKey: "verzonden",
    },
  ] as const)(
    "maps uitkomst from the '$uitkomstKey' taakdata key for $formulierDefinitieId",
    ({ formulierDefinitieId, uitkomstKey }) => {
      const taak = fromPartial<GeneratedType<"RestTask">>({
        formulierDefinitieId,
      });

      expect(
        mapTaskdataToTaskInformation(
          {
            ...ALL_UITKOMST_VALUES,
            toelichting: "extra context",
            bijlagen: "doc-uuid-1",
          },
          taak,
        ),
      ).toEqual({
        uitkomst: ALL_UITKOMST_VALUES[uitkomstKey],
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

describe("mapFormGroupToTaskData", () => {
  it("puts a picked date in the task data as a date without a time", () => {
    const formGroup = new FormGroup({
      verzenddatum: new FormControl(toPickedDate("2026-01-30")),
      datumGeleverd: new FormControl(new Date(2026, 0, 31)),
    });

    expect(mapFormGroupToTaskData(formGroup, {})).toEqual({
      verzenddatum: "2026-01-30",
      datumGeleverd: "2026-01-31",
    });
  });

  it("keeps a date that the task data already held as text", () => {
    const formGroup = new FormGroup({
      datumGevraagd: new FormControl("2026-01-30T10:15:00.000+01:00"),
    });

    expect(mapFormGroupToTaskData(formGroup, {})).toEqual({
      datumGevraagd: "2026-01-30T10:15:00.000+01:00",
    });
  });
});
