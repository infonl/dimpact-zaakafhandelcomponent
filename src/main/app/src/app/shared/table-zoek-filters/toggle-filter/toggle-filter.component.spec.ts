/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { ToggleFilterComponent } from "./toggle-filter.component";
import { ToggleSwitchOptions } from "./toggle-switch-options";

type ToggleFilterInputs = {
  selected?: ToggleSwitchOptions;
  checkedIcon?: string;
  unCheckedIcon?: string;
  indeterminateIcon?: string;
};

describe(ToggleFilterComponent.name, () => {
  const user = userEvent.setup({ delay: null });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToggleFilterComponent, NoopAnimationsModule],
    }).compileComponents();
  });

  const setup = async (inputs: ToggleFilterInputs = {}) => {
    const fixture = TestBed.createComponent(ToggleFilterComponent);
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    const changed = jest.fn<void, [ToggleSwitchOptions]>();
    fixture.componentInstance.changed.subscribe(changed);
    fixture.autoDetectChanges();
    await fixture.whenStable();
    return { fixture, changed };
  };

  const toggleButton = () => screen.getByRole("button");

  describe("the icon", () => {
    it("is the indeterminate icon when no state is bound", async () => {
      await setup();

      expect(toggleButton()).toHaveTextContent("radio_button_unchecked");
    });

    it.each([
      [ToggleSwitchOptions.CHECKED, "check_circle"],
      [ToggleSwitchOptions.UNCHECKED, "cancel"],
      [ToggleSwitchOptions.INDETERMINATE, "radio_button_unchecked"],
    ])("for the bound state %s is %s", async (selected, icon) => {
      await setup({ selected });

      expect(toggleButton()).toHaveTextContent(icon);
    });

    it.each([
      [ToggleSwitchOptions.CHECKED, "fakeCheckedIcon"],
      [ToggleSwitchOptions.UNCHECKED, "fakeUnCheckedIcon"],
      [ToggleSwitchOptions.INDETERMINATE, "fakeIndeterminateIcon"],
    ])(
      "for the bound state %s is the bound custom icon %s",
      async (selected, icon) => {
        await setup({
          selected,
          checkedIcon: "fakeCheckedIcon",
          unCheckedIcon: "fakeUnCheckedIcon",
          indeterminateIcon: "fakeIndeterminateIcon",
        });

        expect(toggleButton()).toHaveTextContent(icon);
      },
    );

    it("follows a different state bound by the parent", async () => {
      const { fixture } = await setup({
        selected: ToggleSwitchOptions.CHECKED,
      });

      fixture.componentRef.setInput("selected", ToggleSwitchOptions.UNCHECKED);
      fixture.detectChanges();

      expect(toggleButton()).toHaveTextContent("cancel");
    });
  });

  describe("clicking the button", () => {
    it.each([
      [
        ToggleSwitchOptions.INDETERMINATE,
        ToggleSwitchOptions.CHECKED,
        "check_circle",
      ],
      [ToggleSwitchOptions.CHECKED, ToggleSwitchOptions.UNCHECKED, "cancel"],
      [
        ToggleSwitchOptions.UNCHECKED,
        ToggleSwitchOptions.INDETERMINATE,
        "radio_button_unchecked",
      ],
    ])(
      "moves the bound state %s on to %s, shows its icon and emits it",
      async (selected, nextState, nextIcon) => {
        const { changed } = await setup({ selected });

        await user.click(toggleButton());

        expect(toggleButton()).toHaveTextContent(nextIcon);
        expect(changed).toHaveBeenCalledTimes(1);
        expect(changed).toHaveBeenCalledWith(nextState);
      },
    );

    it("cycles through all states on consecutive clicks without the parent binding them back", async () => {
      const { changed } = await setup();

      await user.click(toggleButton());
      await user.click(toggleButton());
      await user.click(toggleButton());
      await user.click(toggleButton());

      expect(changed.mock.calls).toEqual([
        [ToggleSwitchOptions.CHECKED],
        [ToggleSwitchOptions.UNCHECKED],
        [ToggleSwitchOptions.INDETERMINATE],
        [ToggleSwitchOptions.CHECKED],
      ]);
      expect(toggleButton()).toHaveTextContent("check_circle");
    });

    it("continues from a state the parent binds after a click", async () => {
      const { fixture, changed } = await setup({
        selected: ToggleSwitchOptions.CHECKED,
      });

      await user.click(toggleButton());
      fixture.componentRef.setInput(
        "selected",
        ToggleSwitchOptions.INDETERMINATE,
      );
      fixture.detectChanges();

      expect(toggleButton()).toHaveTextContent("radio_button_unchecked");

      await user.click(toggleButton());

      expect(changed.mock.calls).toEqual([
        [ToggleSwitchOptions.UNCHECKED],
        [ToggleSwitchOptions.CHECKED],
      ]);
    });
  });
});
