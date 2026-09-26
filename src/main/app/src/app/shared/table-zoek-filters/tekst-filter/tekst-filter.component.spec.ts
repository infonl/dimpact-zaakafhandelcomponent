/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { TekstFilterComponent } from "./tekst-filter.component";

describe(TekstFilterComponent.name, () => {
  const user = userEvent.setup({ delay: null });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TekstFilterComponent, NoopAnimationsModule],
    }).compileComponents();
  });

  const setup = async (inputs: { value?: string | null } = {}) => {
    const fixture = TestBed.createComponent(TekstFilterComponent);
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    const changed = jest.fn<void, [string]>();
    fixture.componentInstance.changed.subscribe(changed);
    fixture.autoDetectChanges();
    await fixture.whenStable();
    return { fixture, changed };
  };

  const input = () => screen.getByRole("textbox");

  const replaceText = async (text: string) => {
    await user.clear(input());
    await user.type(input(), text);
  };

  describe("the text field", () => {
    it("is pre-filled with the bound value", async () => {
      await setup({ value: "fakeValue" });

      expect(input()).toHaveValue("fakeValue");
    });

    it.each([
      ["no value is bound", {}],
      ["the bound value is null", { value: null }],
    ])("is empty when %s", async (_description, inputs) => {
      await setup(inputs);

      expect(input()).toHaveValue("");
    });

    it("keeps its text when the parent binds a different value after initialisation", async () => {
      const { fixture } = await setup({ value: "fakeInitialValue" });

      fixture.componentRef.setInput("value", "fakeParentValue");
      fixture.detectChanges();
      await fixture.whenStable();

      expect(input()).toHaveValue("fakeInitialValue");
    });
  });

  describe("emitting the text", () => {
    it("emits the changed text when the text field loses focus", async () => {
      const { changed } = await setup({ value: "fakeOldValue" });

      await replaceText("fakeNewValue");
      await user.tab();

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith("fakeNewValue");
    });

    it("emits the changed text when Enter is pressed", async () => {
      const { changed } = await setup({ value: "fakeOldValue" });

      await replaceText("fakeNewValue");
      await user.keyboard("{Enter}");

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith("fakeNewValue");
    });

    it("emits the changed text when the search icon is clicked", async () => {
      const { changed } = await setup({ value: "fakeOldValue" });

      await replaceText("fakeNewValue");
      await user.click(screen.getByText("search"));

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith("fakeNewValue");
    });

    it("emits an empty string when the text is erased", async () => {
      const { changed } = await setup({ value: "fakeOldValue" });

      await user.clear(input());
      await user.tab();

      expect(changed).toHaveBeenCalledTimes(1);
      expect(changed).toHaveBeenCalledWith("");
    });

    it("does not emit when the text equals the bound value", async () => {
      const { changed } = await setup({ value: "fakeValue" });

      await user.click(input());
      await user.tab();

      expect(changed).not.toHaveBeenCalled();
    });

    it("emits each changed text only once, without the parent binding it back", async () => {
      const { changed } = await setup({ value: "fakeOldValue" });

      await replaceText("fakeNewValue");
      await user.keyboard("{Enter}");
      await user.tab();
      await user.click(screen.getByText("search"));
      await replaceText("fakeNewerValue");
      await user.tab();

      expect(changed.mock.calls).toEqual([
        ["fakeNewValue"],
        ["fakeNewerValue"],
      ]);
    });

    it("emits an empty string once when the empty text field first loses focus while no value is bound", async () => {
      const { changed } = await setup();

      await user.click(input());
      await user.tab();
      await user.click(input());
      await user.tab();

      expect(changed.mock.calls).toEqual([[""]]);
    });

    it("emits the text again when the user returns to the bound value after a change", async () => {
      const { changed } = await setup({ value: "fakeOldValue" });

      await replaceText("fakeNewValue");
      await user.tab();
      await replaceText("fakeOldValue");
      await user.tab();

      expect(changed.mock.calls).toEqual([["fakeNewValue"], ["fakeOldValue"]]);
    });
  });
});
