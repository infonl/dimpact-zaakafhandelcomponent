/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormioForm } from "@formio/angular";
import { KNOWN_ZAC_FIELDS } from "../../taken/taak-view/formio/formio-setup-service";
import { findTaskFormIssues } from "./task-form-issues";

const submitButton = {
  type: "button",
  key: "submit",
  label: "Afronden",
};

const groupField = {
  type: "select",
  key: "groep",
  attributes: { ZAC_TYPE: KNOWN_ZAC_FIELDS.GROEP },
};

function employeeField(refreshOn?: string) {
  return {
    type: "select",
    key: "medewerker",
    attributes: { ZAC_TYPE: KNOWN_ZAC_FIELDS.MEDEWERKER },
    ...(refreshOn === undefined ? {} : { refreshOn }),
  };
}

function formWith(
  ...components: NonNullable<FormioForm["components"]>
): FormioForm {
  return { display: "form", components };
}

describe(findTaskFormIssues.name, () => {
  it("should report that a form without any button cannot complete its task", () => {
    const issues = findTaskFormIssues(
      formWith({ type: "textfield", key: "opmerking" }),
    );

    expect(issues).toEqual([
      { messageKey: "msg.bpmn.task-forms.issue.no-button" },
    ]);
  });

  it("should report nothing for a form with a button and a plain field", () => {
    const issues = findTaskFormIssues(
      formWith({ type: "textfield", key: "opmerking" }, submitButton),
    );

    expect(issues).toEqual([]);
  });

  it("should report a ZAC field whose type is not one ZAC fills", () => {
    const issues = findTaskFormIssues(
      formWith(
        {
          type: "select",
          key: "groep",
          attributes: { ZAC_TYPE: "ZAC_groepen" },
        },
        submitButton,
      ),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.unknown-zac-field",
        args: { zacType: "ZAC_groepen", sleutel: "groep" },
      },
    ]);
  });

  it("should report nothing for a ZAC field ZAC knows how to fill", () => {
    const issues = findTaskFormIssues(formWith(groupField, submitButton));

    expect(issues).toEqual([]);
  });

  it("should report an employee field that points at no group field", () => {
    const issues = findTaskFormIssues(
      formWith(groupField, employeeField(), submitButton),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.employee-field-without-group",
        args: { sleutel: "medewerker" },
      },
    ]);
  });

  it("should report an employee field that points at a key which is not a group field", () => {
    const issues = findTaskFormIssues(
      formWith(groupField, employeeField("groepje"), submitButton),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.employee-field-without-group",
        args: { sleutel: "medewerker" },
      },
    ]);
  });

  it("should report nothing for an employee field that points at its group field", () => {
    const issues = findTaskFormIssues(
      formWith(groupField, employeeField("groep"), submitButton),
    );

    expect(issues).toEqual([]);
  });

  it("should report a reference table field that names no table", () => {
    const issues = findTaskFormIssues(
      formWith(
        {
          type: "select",
          key: "afdeling",
          attributes: { ZAC_TYPE: KNOWN_ZAC_FIELDS.REFERENTIE_TABEL },
          properties: { ReferenceTable_Code: "" },
        },
        submitButton,
      ),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.reference-table-without-code",
        args: { sleutel: "afdeling" },
      },
    ]);
  });

  it("should report nothing for a reference table field that names a table", () => {
    const issues = findTaskFormIssues(
      formWith(
        {
          type: "select",
          key: "afdeling",
          attributes: { ZAC_TYPE: KNOWN_ZAC_FIELDS.REFERENTIE_TABEL },
          properties: { ReferenceTable_Code: "AFDELING" },
        },
        submitButton,
      ),
    );

    expect(issues).toEqual([]);
  });

  it("should report a key that ZAC reads out of the task data itself", () => {
    const issues = findTaskFormIssues(
      formWith({ type: "textfield", key: "toelichting" }, submitButton),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.reserved-key",
        args: { sleutel: "toelichting" },
      },
    ]);
  });

  it("should not report the submit button itself as a reserved key", () => {
    const issues = findTaskFormIssues(formWith(submitButton));

    expect(issues).toEqual([]);
  });

  it("should look inside a panel for the button", () => {
    const issues = findTaskFormIssues(
      formWith({ type: "panel", key: "form", components: [submitButton] }),
    );

    expect(issues).toEqual([]);
  });

  it("should report a field nested in a panel", () => {
    const issues = findTaskFormIssues(
      formWith({
        type: "panel",
        key: "form",
        components: [
          {
            type: "select",
            key: "groep",
            attributes: { ZAC_TYPE: "typefout" },
          },
          submitButton,
        ],
      }),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.unknown-zac-field",
        args: { zacType: "typefout", sleutel: "groep" },
      },
    ]);
  });

  it("should report a field nested in a column", () => {
    const issues = findTaskFormIssues(
      formWith(
        {
          type: "columns",
          key: "kolommen",
          columns: [
            { components: [{ type: "textfield", key: "toelichting" }] },
          ],
        },
        submitButton,
      ),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.reserved-key",
        args: { sleutel: "toelichting" },
      },
    ]);
  });

  it("should report a field nested in a table row", () => {
    const issues = findTaskFormIssues(
      formWith(
        {
          type: "table",
          key: "tabel",
          rows: [[{ components: [{ type: "textfield", key: "toelichting" }] }]],
        },
        submitButton,
      ),
    );

    expect(issues).toEqual([
      {
        messageKey: "msg.bpmn.task-forms.issue.reserved-key",
        args: { sleutel: "toelichting" },
      },
    ]);
  });

  it("should handle a textarea, whose 'rows' is a line count rather than nested cells", () => {
    const issues = findTaskFormIssues(
      formWith({ type: "textarea", key: "opmerking", rows: 3 }, submitButton),
    );

    expect(issues).toEqual([]);
  });

  it("should handle a component whose nested keys hold something other than components", () => {
    const issues = findTaskFormIssues(
      formWith(
        {
          type: "select",
          key: "keuze",
          components: undefined,
          columns: 2,
          rows: "auto",
        },
        submitButton,
      ),
    );

    expect(issues).toEqual([]);
  });
});
