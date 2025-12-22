/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractControl, FormGroup } from "@angular/forms";
import { isMoment } from "moment";
import {
  DOCUMENT_STRING_SPLITTER,
  mapDocumentenToString,
} from "../../documenten/document-utils";
import { GeneratedType } from "../../shared/utils/generated-types";

type ControlMapOptions = {
  documentKey: keyof GeneratedType<"RestEnkelvoudigInformatieobject">;
  documentSeparator?: string;
};

export type OptionValue = {
  key: unknown;
  value: unknown;
};

function mapControlToTaskDataValue(
  control: AbstractControl,
  options: ControlMapOptions = {
    documentKey: "uuid",
    documentSeparator: DOCUMENT_STRING_SPLITTER,
  },
): string {
  const { value } = control;
  if (value === null || value === undefined) return "";
  if (isMoment(value)) return value.toISOString();
  switch (typeof value) {
    case "boolean":
      return `${value}`;
    case "string":
    case "number":
    case "bigint":
      return String(value);
    case "object":
      if (Array.isArray(value)) {
        // For now, we can assume it is an array of documents
        const documents = value.map(
          (document) => document[options.documentKey],
        );
        return mapDocumentenToString(documents);
      }

      // Options which have a `key` and `value` property
      if ("key" in value && "value" in value) return `${value.value}`;

      // html-text editor
      if ("body" in value) return value.body;
      return JSON.stringify(value); // Fallback
    default:
      return value;
  }
}

type MapFormGroupToTaskDataOptions = {
  ignoreKeys?: string[];
  mapControlOptions?: ControlMapOptions;
};

export function mapFormGroupToTaskData(
  formGroup: FormGroup,
  options: MapFormGroupToTaskDataOptions,
) {
  return Object.entries(formGroup.controls).reduce(
    (acc, [key, control]) => {
      if (options.ignoreKeys?.includes(key)) return acc; // Not task data
      acc[key] = mapControlToTaskDataValue(control, options.mapControlOptions);
      return acc;
    },
    {} as Record<string, string>,
  );
}

type ToelichtingMapping<
  Data extends Record<string, string> = Record<string, string>,
> = {
  uitkomst: keyof Data;
  bijlagen?: keyof Data;
  opmerking?: keyof Data;
};

const DEFAULT_TOELICHTING_MAPPING: ToelichtingMapping = {
  uitkomst: "uitkomst",
  bijlagen: "bijlagen",
  opmerking: "toelichting",
};

function getToelichtingMapping(
  taak: GeneratedType<"RestTask">,
): ToelichtingMapping {
  switch (taak.formulierDefinitieId) {
    case "GOEDKEUREN":
      return {
        ...DEFAULT_TOELICHTING_MAPPING,
        uitkomst: "goedkeuren",
      };
    case "AANVULLENDE_INFORMATIE":
      return {
        ...DEFAULT_TOELICHTING_MAPPING,
        uitkomst: "aanvullendeInformatie",
      };
    case "ADVIES":
      return {
        ...DEFAULT_TOELICHTING_MAPPING,
        uitkomst: "advies",
      };
    default:
      throw new Error(`Onbekend formulier: ${taak.formulierDefinitieId}`);
  }
}

export function mapTaskdataToTaskInformation(
  taskData: Record<string, string>,
  taak: GeneratedType<"RestTask">,
) {
  const mapping = getToelichtingMapping(taak);
  return {
    uitkomst: taskData[mapping.uitkomst] ?? "",
    bijlagen: mapping.bijlagen ? (taskData[mapping.bijlagen] ?? "") : "",
    opmerking: mapping.opmerking ? (taskData[mapping.opmerking] ?? "") : "",
  };
}
