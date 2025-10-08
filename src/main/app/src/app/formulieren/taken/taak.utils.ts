/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AbstractControl, FormGroup } from "@angular/forms";
import { isMoment } from "moment";
import { GeneratedType } from "../../shared/utils/generated-types";
type ControlMapOptions = {
  documentKey: keyof GeneratedType<"RestEnkelvoudigInformatieobject">;
  documentSeparator?: string;
};

function mapControlToTaskDataValue(
  control: AbstractControl,
  options: ControlMapOptions = {
    documentKey: "uuid",
    documentSeparator: ";",
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
        return value
          .map((document) => document[options.documentKey])
          .join(options.documentSeparator);
      }
      if ("key" in value && "value" in value) {
        // Options should have a `key` and `value` property
        return `${value.value}`;
      }

      if ("body" in value) {
        // html-text editor
        return value.body;
      }
      console.log(value);
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

type ToelichtingMapping<Data extends Record<string, string>> = {
  uitkomst: keyof Data;
  bijlagen?: keyof Data;
  opmerking?: keyof Data;
};

function getToelichtingMapping(
  taak: GeneratedType<"RestTask">,
): ToelichtingMapping<Record<string, string>> {
  switch (taak.formulierDefinitieId) {
    case "GOEDKEUREN":
      return {
        uitkomst: "goedkeuren",
        opmerking: "toelichting",
        bijlagen: "bijlagen",
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
