import { AbstractControl, FormGroup } from "@angular/forms";
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
      // We assume options are being set as {key: string, value: string} objects
      if ("key" in value && "value" in value) {
        return `${value.value}`;
      }
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
  console.log({ taskData });
  return {
    uitkomst: taskData[mapping.uitkomst] ?? "",
    bijlagen: mapping.bijlagen ? (taskData[mapping.bijlagen] ?? "") : "",
    opmerking: mapping.opmerking ? (taskData[mapping.opmerking] ?? "") : "",
  };
}
