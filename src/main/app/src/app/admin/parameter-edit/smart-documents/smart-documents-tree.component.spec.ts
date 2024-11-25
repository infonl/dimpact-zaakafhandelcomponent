/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  filterOutUnselected,
  getSelectableGroups,
} from "./smart-documents-tree.component";

describe("SmartDocumentsTree-getSelectableGroups", () => {
  it("should mark matching templates at the root level as selected", () => {
    const result = getSelectableGroups(
      //all
      [
        {
          id: "some-group",
          name: "",
          templates: [
            {
              id: "some-template",
              name: "",
            },
          ],
        },
      ],
      //selection
      [
        {
          id: "some-group",
          name: "",
          templates: [
            {
              id: "some-template",
              name: "",
              informatieObjectTypeUUID: "my-id",
            },
          ],
        },
      ],
    );

    expect(result.length).toBe(1);
    const group = result[0];
    expect(group.templates).toBeTruthy();
    expect(group.templates.length).toBe(1);
    const template = group.templates[0];
    expect(template.id).toBe("some-template");
    expect(template.informatieObjectTypeUUID).toBe("my-id");
  });

  it("should mark matching templates at a nested level as selected", () => {
    const result = getSelectableGroups(
      //all
      [
        {
          id: "some-group",
          name: "",
          groups: [
            {
              id: "inner-group",
              name: "",
              templates: [
                {
                  id: "some-template",
                  name: "",
                },
              ],
            },
          ],
        },
      ],
      //selection
      [
        {
          id: "some-group",
          name: "",
          groups: [
            {
              id: "inner-group",
              name: "",
              templates: [
                {
                  id: "some-template",
                  name: "",
                  informatieObjectTypeUUID: "my-id",
                },
              ],
            },
          ],
        },
      ],
    );

    expect(result.length).toBe(1);
    const group = result[0];
    expect(group.groups).toBeTruthy();
    expect(group.groups.length).toBe(1);
    const inner = group.groups[0];
    expect(inner.templates).toBeTruthy();
    expect(inner.templates.length).toBe(1);
    const template = inner.templates[0];
    expect(template.id).toBe("some-template");
    expect(template.informatieObjectTypeUUID).toBe("my-id");
  });

  it("should ignore old groups and templates that don't exist anymore", () => {
    const result = getSelectableGroups(
      //all
      [
        {
          id: "i-am-an-existing-group",
          name: "",
          templates: [
            {
              id: "i-am-a-new-template",
              name: "",
            },
          ],
        },
      ],
      // selection
      [
        {
          id: "i-am-an-old-group",
          name: "",
          templates: [
            {
              id: "i-am-an-old-template",
              name: "",
              informatieObjectTypeUUID: "my-id",
            },
          ],
        },
        {
          id: "i-am-an-existing-group",
          name: "",
          templates: [
            {
              id: "i-am-also-an-old-template",
              name: "",
              informatieObjectTypeUUID: "my-id",
            },
          ],
        },
      ],
    );
    expect(result.length).toBe(1);
    const firstGroup = result[0];
    expect(firstGroup.templates).toBeTruthy();
    expect(firstGroup.templates.length).toBe(1);
    const firstTemplate = firstGroup.templates[0];
    expect(firstTemplate.id).toBe("i-am-a-new-template");
    expect(firstTemplate.informatieObjectTypeUUID).toBeFalsy();
  });
});

describe("SmartDocumentsTree-filterOutUnselected", () => {
  it("should filter out unselected items at the highest level", () => {
    const result = filterOutUnselected({
      id: "",
      name: "",
      templates: [
        {
          id: "1",
          name: "",
          informatieObjectTypeUUID: "my-id",
        },
        {
          id: "2",
          name: "",
          informatieObjectTypeUUID: "",
        },
      ],
    });
    expect(result.templates).toBeTruthy();
    expect(result.templates.length).toBe(1);
    expect(result.templates[0].id).toBe("1");
  });

  it("should filter out unselected items at a lower level", () => {
    const result = filterOutUnselected({
      id: "",
      name: "",
      groups: [
        {
          id: "",
          name: "",
          templates: [
            {
              id: "1",
              name: "",
              informatieObjectTypeUUID: "my-id",
            },
            {
              id: "2",
              name: "",
              informatieObjectTypeUUID: "",
            },
          ],
        },
      ],
    });
    expect(result.groups).toBeTruthy();
    expect(result.groups.length).toBe(1);
    const inner = result.groups[0];
    expect(inner.templates).toBeTruthy();
    expect(inner.templates.length).toBe(1);
    expect(inner.templates[0].id).toBe("1");
  });

  it("should filter out groups with no selected items", () => {
    const result = filterOutUnselected({
      id: "",
      name: "",
      groups: [
        {
          id: "1",
          name: "",
          templates: [
            {
              id: "2",
              name: "",
              informatieObjectTypeUUID: "",
            },
          ],
        },
        {
          id: "3",
          name: "",
          templates: [
            {
              id: "4",
              name: "",
              informatieObjectTypeUUID: "my-id",
            },
          ],
        },
      ],
    });
    expect(result.groups).toBeTruthy();
    expect(result.groups.length).toBe(1);
    const inner = result.groups[0];
    expect(inner.id).toBe("3");
  });
});
