/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { render, screen } from "@testing-library/angular";
import { ReadMoreComponent } from "./read-more.component";

describe(ReadMoreComponent.name, () => {
  const setup = async (inputs: { text?: string; maxLength?: number }) => {
    const { fixture, container } = await render(ReadMoreComponent, {
      inputs,
      imports: [NoopAnimationsModule],
    });

    return { fixture, container };
  };

  it("shows the whole text when it is exactly maxLength long", async () => {
    await setup({ text: "fakeText", maxLength: 8 });

    expect(screen.getByText("fakeText")).toBeInTheDocument();
  });

  it("cuts a text longer than maxLength to maxLength characters, ending in an ellipsis", async () => {
    await setup({ text: "fakeLongText", maxLength: 10 });

    expect(screen.getByText("fakeLon...")).toBeInTheDocument();
  });

  it("describes a cut text with the whole text, through its tooltip", async () => {
    await setup({ text: "fakeLongText", maxLength: 10 });

    expect(screen.getByText("fakeLon...")).toHaveAccessibleDescription(
      "fakeLongText",
    );
  });

  it("gives a text that fits no tooltip", async () => {
    await setup({ text: "fakeText", maxLength: 100 });

    expect(screen.getByText("fakeText")).not.toHaveAccessibleDescription();
  });

  it("cuts at 100 characters when no maxLength is given", async () => {
    await setup({ text: "x".repeat(101) });

    expect(screen.getByText(`${"x".repeat(97)}...`)).toBeInTheDocument();
  });

  it("does not cut a text of 100 characters when no maxLength is given", async () => {
    await setup({ text: "x".repeat(100) });

    expect(screen.getByText("x".repeat(100))).toBeInTheDocument();
  });

  it("accepts maxLength as a numeric string attribute", async () => {
    await render(`<read-more text="fakeLongText" maxLength="10"></read-more>`, {
      imports: [ReadMoreComponent, NoopAnimationsModule],
    });

    expect(screen.getByText("fakeLon...")).toBeInTheDocument();
  });

  it("renders the text as HTML", async () => {
    await setup({ text: "<em>fakeText</em>", maxLength: 100 });

    expect(
      screen.getByText("fakeText", { selector: "em" }),
    ).toBeInTheDocument();
  });

  it("renders nothing when there is no text", async () => {
    const { container } = await setup({ text: undefined });

    expect(container).toHaveTextContent("");
  });

  it("cuts the text once it changes into one longer than maxLength", async () => {
    const { fixture } = await setup({ text: "fakeText", maxLength: 10 });

    fixture.componentRef.setInput("text", "fakeLongText");
    fixture.detectChanges();

    expect(screen.getByText("fakeLon...")).toHaveAccessibleDescription(
      "fakeLongText",
    );
  });

  it("shows the whole text again once it changes into one that fits", async () => {
    const { fixture } = await setup({ text: "fakeLongText", maxLength: 10 });

    fixture.componentRef.setInput("text", "fakeText");
    fixture.detectChanges();

    expect(screen.getByText("fakeText")).not.toHaveAccessibleDescription();
  });

  it("cuts the text again once maxLength changes", async () => {
    const { fixture } = await setup({ text: "fakeLongText", maxLength: 10 });

    fixture.componentRef.setInput("maxLength", 8);
    fixture.detectChanges();

    expect(screen.getByText("fakeL...")).toBeInTheDocument();
  });

  it("shows the whole text once maxLength grows beyond the text length", async () => {
    const { fixture } = await setup({ text: "fakeLongText", maxLength: 10 });

    fixture.componentRef.setInput("maxLength", 12);
    fixture.detectChanges();

    expect(screen.getByText("fakeLongText")).toBeInTheDocument();
  });
});
