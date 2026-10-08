/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { TextIcon } from "../edit/text-icon";
import { StaticTextComponent } from "./static-text.component";

const makeIcon = (
  showIcon: (control: FormControl) => boolean = () => true,
  outlined?: boolean,
) =>
  new TextIcon(
    showIcon,
    "fakeIconName",
    "fakeIconId",
    "fakeIconTitle",
    "fakeIconStyleClass",
    outlined,
  );

const showWhenOverdue = (control: FormControl) =>
  control.value === "fakeOverdueValue";

describe(StaticTextComponent.name, () => {
  const user = userEvent.setup();

  const setup = async (
    inputs: {
      label?: string;
      value?: string | number | null;
      icon?: TextIcon | null;
      maxLength?: number;
    } = {},
  ) => {
    const iconClicked = jest.fn();
    const { fixture } = await render(StaticTextComponent, {
      inputs,
      on: { iconClicked },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
    });

    return { fixture, iconClicked };
  };

  describe("label", () => {
    it("shows the label", async () => {
      await setup({ label: "fakeLabel" });

      expect(screen.getByText("fakeLabel")).toBeInTheDocument();
    });

    it("translates the label", async () => {
      const { fixture } = await setup({ label: "fakeLabel" });

      const translateService = TestBed.inject(TranslateService);
      translateService.setTranslation("nl", { fakeLabel: "Fake label" });
      translateService.use("nl");
      fixture.detectChanges();

      expect(screen.getByText("Fake label")).toBeInTheDocument();
    });

    it("shows the new label once the label changes", async () => {
      const { fixture } = await setup({ label: "fakeLabel" });

      fixture.componentRef.setInput("label", "fakeOtherLabel");
      fixture.detectChanges();

      expect(screen.getByText("fakeOtherLabel")).toBeInTheDocument();
    });
  });

  describe("value", () => {
    it("shows a dash when there is no value", async () => {
      await setup({ label: "fakeLabel" });

      expect(screen.getByText("-")).toBeInTheDocument();
    });

    it("shows the whole value when no maxLength is given", async () => {
      await setup({ value: "fakeVeryLongValue".repeat(10) });

      expect(
        screen.getByText("fakeVeryLongValue".repeat(10)),
      ).toBeInTheDocument();
    });

    it("shows a number value", async () => {
      await setup({ value: 42 });

      expect(screen.getByText("42")).toBeInTheDocument();
    });

    it("shows the new value once the value changes", async () => {
      const { fixture } = await setup({ value: "fakeValue" });

      fixture.componentRef.setInput("value", "fakeOtherValue");
      fixture.detectChanges();

      expect(screen.getByText("fakeOtherValue")).toBeInTheDocument();
    });
  });

  describe("maxLength", () => {
    it("cuts a value longer than maxLength", async () => {
      await setup({ value: "fakeLongValue", maxLength: 8 });

      expect(screen.getByText("fakeL...")).toHaveAccessibleDescription(
        "fakeLongValue",
      );
    });

    it("does not cut a value that fits maxLength", async () => {
      await setup({ value: "fakeValue", maxLength: 9 });

      expect(screen.getByText("fakeValue")).toBeInTheDocument();
    });

    it("accepts maxLength as a numeric string attribute", async () => {
      await render(
        `<zac-static-text value="fakeLongValue" maxLength="8"></zac-static-text>`,
        {
          imports: [
            StaticTextComponent,
            NoopAnimationsModule,
            TranslateModule.forRoot(),
          ],
        },
      );

      expect(screen.getByText("fakeL...")).toBeInTheDocument();
    });

    it("shows a dash when there is no value", async () => {
      await setup({ maxLength: 8 });

      expect(screen.getByText("-")).toBeInTheDocument();
    });

    it("does not cut the value when maxLength is 0", async () => {
      await setup({ value: "fakeLongValue", maxLength: 0 });

      expect(screen.getByText("fakeLongValue")).toBeInTheDocument();
    });

    it("starts cutting the value once maxLength is given", async () => {
      const { fixture } = await setup({ value: "fakeLongValue" });

      fixture.componentRef.setInput("maxLength", 8);
      fixture.detectChanges();

      expect(screen.getByText("fakeL...")).toBeInTheDocument();
    });

    it("stops cutting the value once maxLength is removed", async () => {
      const { fixture } = await setup({ value: "fakeLongValue", maxLength: 8 });

      fixture.componentRef.setInput("maxLength", undefined);
      fixture.detectChanges();

      expect(screen.getByText("fakeLongValue")).toBeInTheDocument();
    });

    it("cuts a changed value with the same maxLength", async () => {
      const { fixture } = await setup({ value: "fakeValue", maxLength: 8 });

      fixture.componentRef.setInput("value", "fakeLongValue");
      fixture.detectChanges();

      expect(screen.getByText("fakeL...")).toBeInTheDocument();
    });
  });

  describe("icon", () => {
    it("shows the icon when its condition holds for the value", async () => {
      await setup({
        value: "fakeOverdueValue",
        icon: makeIcon(showWhenOverdue),
      });

      expect(screen.getByTitle("fakeIconTitle")).toHaveTextContent(
        "fakeIconName",
      );
    });

    it("hides the icon when its condition does not hold for the value", async () => {
      await setup({ value: "fakeValue", icon: makeIcon(showWhenOverdue) });

      expect(screen.queryByTitle("fakeIconTitle")).not.toBeInTheDocument();
    });

    it("shows no icon when the icon is null", async () => {
      await setup({ value: "fakeOverdueValue", icon: null });

      expect(screen.queryByText("fakeIconName")).not.toBeInTheDocument();
    });

    it("passes the value to the icon condition as a form control", async () => {
      const showIcon = jest.fn().mockReturnValue(true);

      await setup({ value: "fakeValue", icon: makeIcon(showIcon) });

      expect(showIcon).toHaveBeenCalledWith(
        expect.objectContaining({ value: "fakeValue" }),
      );
      expect(showIcon.mock.lastCall?.[0]).toBeInstanceOf(FormControl);
    });

    it("shows the icon once the value changes into one that meets its condition", async () => {
      const { fixture } = await setup({
        value: "fakeValue",
        icon: makeIcon(showWhenOverdue),
      });

      fixture.componentRef.setInput("value", "fakeOverdueValue");
      fixture.detectChanges();

      expect(screen.getByTitle("fakeIconTitle")).toBeInTheDocument();
    });

    it("hides the icon once the value changes into one that does not meet its condition", async () => {
      const { fixture } = await setup({
        value: "fakeOverdueValue",
        icon: makeIcon(showWhenOverdue),
      });

      fixture.componentRef.setInput("value", "fakeValue");
      fixture.detectChanges();

      expect(screen.queryByTitle("fakeIconTitle")).not.toBeInTheDocument();
    });

    it("shows the icon once an icon is given after the first render", async () => {
      const { fixture } = await setup({ value: "fakeOverdueValue" });

      fixture.componentRef.setInput("icon", makeIcon(showWhenOverdue));
      fixture.detectChanges();

      expect(screen.getByTitle("fakeIconTitle")).toBeInTheDocument();
    });

    it("hides the icon once the icon is removed", async () => {
      const { fixture } = await setup({
        value: "fakeOverdueValue",
        icon: makeIcon(),
      });

      fixture.componentRef.setInput("icon", null);
      fixture.detectChanges();

      expect(screen.queryByTitle("fakeIconTitle")).not.toBeInTheDocument();
    });

    it("gives the icon its style class", async () => {
      await setup({ icon: makeIcon() });

      expect(screen.getByTitle("fakeIconTitle")).toHaveClass(
        "fakeIconStyleClass",
      );
    });

    it("marks an outlined icon as outlined", async () => {
      await setup({ icon: makeIcon(() => true, true) });

      expect(screen.getByTitle("fakeIconTitle")).toHaveAttribute(
        "outlined",
        "true",
      );
    });

    it("translates the icon title", async () => {
      const { fixture } = await setup({ icon: makeIcon() });

      const translateService = TestBed.inject(TranslateService);
      translateService.setTranslation("nl", {
        fakeIconTitle: "Fake icon title",
      });
      translateService.use("nl");
      fixture.detectChanges();

      expect(screen.getByTitle("Fake icon title")).toBeInTheDocument();
    });

    it("emits iconClicked when the icon is clicked", async () => {
      const { iconClicked } = await setup({ icon: makeIcon() });

      await user.click(screen.getByTitle("fakeIconTitle"));

      expect(iconClicked).toHaveBeenCalledTimes(1);
    });
  });
});
