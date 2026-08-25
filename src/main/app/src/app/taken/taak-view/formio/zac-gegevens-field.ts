/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { escapeHtml, renderFieldError } from "./formio-component-utils";

const ZAC_PATH_PROPERTY = "ZAC_VELD";

const ZAC_FORMAT_PROPERTY = "ZAC_FORMAAT";

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})/;

const NO_VALUE = "—";

class FormatMismatch extends Error {}

/**
 * A formatter throws when the value is not of the shape it formats, so a mismatched format is
 * reported instead of quietly showing the value unformatted.
 */
const VALUE_FORMATTERS: Record<
  string,
  (value: unknown, translate: TranslateService) => string
> = {
  datum: (value) => {
    const parts = ISO_DATE.exec(String(value));
    if (!parts) {
      throw new FormatMismatch(`"${String(value)}" is not a date`);
    }
    return `${parts[3]}-${parts[2]}-${parts[1]}`;
  },
  jaNee: (value, translate) => {
    if (typeof value !== "boolean" && value !== "true" && value !== "false") {
      throw new FormatMismatch(`"${String(value)}" is not a yes or a no`);
    }
    return translate.instant(
      value === true || value === "true" ? "actie.ja" : "actie.nee",
    );
  },
};

const VALUE_PARSING_TYPES = ["datetime", "date", "day", "time"];

const LIST_SEGMENT_SUFFIX = "[]";

const LIST_SEPARATOR = ", ";

/**
 * Only seeding is left here: reading a value for display is done with `{{ zaak.x }}` in a Form.io
 * content component, which Form.io interpolates itself. Seeding cannot move there because
 * `customDefaultValue` is skipped once the submission already carries the key, which taakdata does.
 */
export function initializeGegevensField(
  component: ExtendedComponentSchema,
  source: object,
  sourceLabel: string,
  taak: GeneratedType<"RestTask">,
  translateService: TranslateService,
) {
  if (component.input !== true) {
    renderFieldError(
      component,
      `A "${component.type}" field holds no value, so there is nothing to fill in. Read a value ` +
        `for display with "{{ ${sourceLabel.toLowerCase()}.` +
        `${component.properties?.[ZAC_PATH_PROPERTY] ?? "property"} }}" in a content component.`,
    );
    return;
  }

  seedInputField(
    component,
    source,
    sourceLabel,
    taak,
    component.properties?.[ZAC_PATH_PROPERTY] ?? "",
    translateService,
    component.properties?.[ZAC_FORMAT_PROPERTY],
  );
}

/**
 * The value goes into the task data too, because Form.io prefers submission data over
 * `defaultValue`. A value already stored there is left alone, so a saved answer survives.
 */
function seedInputField(
  component: ExtendedComponentSchema,
  source: object,
  sourceLabel: string,
  taak: GeneratedType<"RestTask">,
  path: string,
  translateService: TranslateService,
  format?: string,
) {
  try {
    if (format && parsesItsOwnValue(component)) {
      throw new Error(
        `A "${component.type}" field parses its own value and formats it for display, so a ` +
          `formatted value cannot be read. Drop ${ZAC_FORMAT_PROPERTY} "${format}".`,
      );
    }
    const resolved = resolvePath(source, path, sourceLabel);
    // NO_VALUE is a marker for a reader; seeding it would hand a date picker the string "—"
    if (isEmptyValue(resolved)) return;

    const value = formatValue(resolved, translateService, format);
    component.defaultValue = value;

    const stored = taak.taakdata?.[component.key];
    if (
      taak.taakdata &&
      (stored === undefined || stored === null || stored === "")
    ) {
      taak.taakdata[component.key] = value;
    }
  } catch (error) {
    renderFieldError(
      component,
      error instanceof Error ? error.message : String(error),
    );
  }
}

function formatValue(
  value: unknown,
  translateService: TranslateService,
  format?: string,
): string {
  if (Array.isArray(value)) {
    const elements = value
      .filter(
        (element) =>
          element !== null && element !== undefined && element !== "",
      )
      .map((element) => formatValue(element, translateService, format));
    return elements.length ? elements.join(LIST_SEPARATOR) : NO_VALUE;
  }
  if (value === null || value === undefined || value === "") return NO_VALUE;
  if (!format) return String(value);

  const formatter = VALUE_FORMATTERS[format];
  if (!formatter) {
    throw new Error(
      `Unknown ${ZAC_FORMAT_PROPERTY} "${format}". Available: ` +
        Object.keys(VALUE_FORMATTERS).sort().join(", "),
    );
  }
  try {
    return formatter(value, translateService);
  } catch (error) {
    if (error instanceof FormatMismatch) {
      throw new FormatMismatch(
        `${ZAC_FORMAT_PROPERTY} "${format}" cannot be applied: ${error.message}.`,
      );
    }
    throw error;
  }
}

/**
 * An absent property resolves to null, not an error: the API omits empty properties, so absence
 * cannot be told from a typo. A `[]` segment reads on through every element.
 */
function resolvePath(
  source: object,
  path: string,
  sourceLabel: string,
): unknown {
  if (!path) {
    throw new Error(`Missing ${ZAC_PATH_PROPERTY} property.`);
  }

  const value = walkSegments(source, path.split("."), [], sourceLabel);

  if (Array.isArray(value)) {
    const objectElement = value.find(
      (element) => typeof element === "object" && element !== null,
    );
    if (objectElement) {
      throw new Error(
        `${sourceLabel} property "${path}" holds a list of objects. Address a property of ` +
          `each element, as in "${path}${LIST_SEGMENT_SUFFIX}.` +
          `${Object.keys(objectElement).sort()[0]}".`,
      );
    }
    return value;
  }

  if (typeof value === "object" && value !== null) {
    throw new Error(
      `${sourceLabel} property "${path}" holds an object, not a single value. Address a key of ` +
        `it, as in "${path}.${Object.keys(value).sort()[0]}".`,
    );
  }
  return value;
}

function walkSegments(
  start: unknown,
  segments: string[],
  walked: string[],
  sourceLabel: string,
): unknown {
  let value = start;
  for (let index = 0; index < segments.length; index++) {
    if (value === null || value === undefined) return null;

    const segment = segments[index];
    const isList = segment.endsWith(LIST_SEGMENT_SUFFIX);
    const name = isList
      ? segment.slice(0, -LIST_SEGMENT_SUFFIX.length)
      : segment;

    if (typeof value !== "object") {
      throw new Error(
        `${sourceLabel} property "${walked.join(".")}" holds a single value, ` +
          `so "${name}" cannot be read from it.`,
      );
    }
    value = (value as Record<string, unknown>)[name];
    walked.push(name);

    if (!isList) continue;

    if (value === null || value === undefined) return null;
    if (!Array.isArray(value)) {
      throw new Error(
        `${sourceLabel} property "${walked.join(".")}" is not a list, ` +
          `so "${LIST_SEGMENT_SUFFIX}" cannot be used on it.`,
      );
    }
    const rest = segments.slice(index + 1);
    return value.map((element) =>
      rest.length
        ? walkSegments(element, rest, [...walked], sourceLabel)
        : element,
    );
  }
  return value;
}

/** A calendar widget can sit on other types too, so check the widget as well as the type. */
function parsesItsOwnValue(component: ExtendedComponentSchema) {
  return (
    VALUE_PARSING_TYPES.includes(String(component.type)) ||
    (component.widget as { type?: string } | undefined)?.type === "calendar"
  );
}

function isEmptyValue(value: unknown): boolean {
  return (
    value === null ||
    value === undefined ||
    value === "" ||
    (Array.isArray(value) &&
      !value.filter((element) => !isEmptyValue(element)).length)
  );
}
