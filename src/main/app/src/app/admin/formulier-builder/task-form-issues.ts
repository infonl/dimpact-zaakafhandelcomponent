/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormioForm } from "@formio/angular";
import {
  KNOWN_ZAC_FIELDS,
  ZAC_FIELD_ATTRIBUTE,
} from "../../taken/taak-view/formio/formio-setup-service";
import { flattenComponents, FormioComponent } from "./form-components";

/**
 * Keys that ZAC reads out of the task data itself, see `TaakVariabelenService`. A component carrying
 * one of these is not wrong — the mail and signing forms rely on it — but its value is handed to ZAC
 * instead of being stored as a plain answer.
 */
const TASK_DATA_KEYS_USED_BY_ZAC = [
  "bijlagen",
  "body",
  "documentenVerzendenPost",
  "emailadres",
  "ondertekenen",
  "replyTo",
  "taakStuurGegevens.mail",
  "taakStuurGegevens.sendMail",
  "toelichting",
  "verzenddatum",
  "verzender",
  "zaakHervatten",
  "zaakOpschorten",
];

/** Carried by the buttons, and dropped from the task data on submit by `taak-view.component`. */
const SUBMIT_KEYS = ["submit", "save"];

const KNOWN_ZAC_FIELD_TYPES: string[] = Object.values(KNOWN_ZAC_FIELDS);

export type TaskFormIssue = {
  messageKey: string;
  args?: Record<string, string>;
};

function zacFieldTypeOf(component: FormioComponent) {
  return component.attributes?.[ZAC_FIELD_ATTRIBUTE];
}

/** Reported rather than blocked: a form that trips a check is unusual, not necessarily wrong. */
export function findTaskFormIssues(
  form: Pick<FormioForm, "components">,
): TaskFormIssue[] {
  const components = flattenComponents(form.components);
  const groupFieldKeys = components
    .filter((component) => zacFieldTypeOf(component) === KNOWN_ZAC_FIELDS.GROEP)
    .map((component) => component.key);
  const issues: TaskFormIssue[] = [];

  if (!components.some((component) => component.type === "button")) {
    issues.push({ messageKey: "msg.bpmn.task-forms.issue.no-button" });
  }

  components.forEach((component) => {
    const zacFieldType = zacFieldTypeOf(component);
    const key = component.key ?? "";

    if (zacFieldType && !KNOWN_ZAC_FIELD_TYPES.includes(zacFieldType)) {
      issues.push({
        messageKey: "msg.bpmn.task-forms.issue.unknown-zac-field",
        args: { zacType: zacFieldType, sleutel: key },
      });
    }
    if (
      zacFieldType === KNOWN_ZAC_FIELDS.MEDEWERKER &&
      !groupFieldKeys.includes(component.refreshOn as string)
    ) {
      issues.push({
        messageKey: "msg.bpmn.task-forms.issue.employee-field-without-group",
        args: { sleutel: key },
      });
    }
    if (
      zacFieldType === KNOWN_ZAC_FIELDS.REFERENTIE_TABEL &&
      !component.properties?.["ReferenceTable_Code"]
    ) {
      issues.push({
        messageKey: "msg.bpmn.task-forms.issue.reference-table-without-code",
        args: { sleutel: key },
      });
    }
    if (
      key &&
      !SUBMIT_KEYS.includes(key) &&
      TASK_DATA_KEYS_USED_BY_ZAC.includes(key)
    ) {
      issues.push({
        messageKey: "msg.bpmn.task-forms.issue.reserved-key",
        args: { sleutel: key },
      });
    }
  });

  return issues;
}
