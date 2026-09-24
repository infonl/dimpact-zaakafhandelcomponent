/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { ZoekenColumn } from "../model/zoeken-column";
import { ColumnPickerValue } from "./column-picker-value";
import { ColumnPickerComponent } from "./column-picker.component";

type Columns = Map<ZoekenColumn, ColumnPickerValue>;

const makeColumns = (
  entries: Partial<Record<ZoekenColumn, ColumnPickerValue>>,
): Columns =>
  new Map(Object.entries(entries) as [ZoekenColumn, ColumnPickerValue][]);

describe(ColumnPickerComponent.name, () => {
  const user = userEvent.setup({ delay: null });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ColumnPickerComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
    }).compileComponents();

    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation("nl", {
      [ZoekenColumn.NAAM]: "Zaaknaam",
      [ZoekenColumn.CREATIEDATUM]: "Aangemaakt op",
      [ZoekenColumn.STARTDATUM]: "Startdatum",
    });
    translateService.use("nl");
  });

  const setup = async (columnSrc: Columns) => {
    const fixture = TestBed.createComponent(ColumnPickerComponent);
    fixture.componentRef.setInput("columnSrc", columnSrc);
    const columnsChanged = jest.fn<void, [Columns]>();
    fixture.componentInstance.columnsChanged.subscribe(columnsChanged);
    fixture.autoDetectChanges();
    await fixture.whenStable();
    return { fixture, columnsChanged };
  };

  const menuButton = () =>
    screen.getByRole("button", { name: "actie.kolommen.wijzig" });

  const openMenu = async () => {
    await user.click(menuButton());
    return screen.getByRole("listbox");
  };

  const closeMenu = async () => {
    await user.click(menuButton());
  };

  const optionNames = () =>
    screen.getAllByRole("option").map((option) => option.textContent?.trim());

  const selectedOptionNames = () =>
    screen
      .getAllByRole("option", { selected: true })
      .map((option) => option.textContent?.trim());

  describe("the menu", () => {
    it("lists the translated non-sticky columns, sorted by their translation", async () => {
      await setup(
        makeColumns({
          [ZoekenColumn.SELECT]: ColumnPickerValue.STICKY,
          [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
          [ZoekenColumn.STARTDATUM]: ColumnPickerValue.HIDDEN,
          [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.VISIBLE,
        }),
      );

      await openMenu();

      expect(optionNames()).toEqual([
        "Aangemaakt op",
        "Startdatum",
        "Zaaknaam",
      ]);
    });

    it("selects the visible columns", async () => {
      await setup(
        makeColumns({
          [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
          [ZoekenColumn.STARTDATUM]: ColumnPickerValue.HIDDEN,
          [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.VISIBLE,
        }),
      );

      await openMenu();

      expect(selectedOptionNames()).toEqual(["Aangemaakt op", "Zaaknaam"]);
    });

    it("shows the columns and visible columns of a different map bound by the parent", async () => {
      const { fixture } = await setup(
        makeColumns({
          [ZoekenColumn.NAAM]: ColumnPickerValue.HIDDEN,
          [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.VISIBLE,
        }),
      );

      fixture.componentRef.setInput(
        "columnSrc",
        makeColumns({
          [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
          [ZoekenColumn.STARTDATUM]: ColumnPickerValue.HIDDEN,
          [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.VISIBLE,
        }),
      );
      fixture.detectChanges();
      await openMenu();

      expect(optionNames()).toEqual([
        "Aangemaakt op",
        "Startdatum",
        "Zaaknaam",
      ]);
      expect(selectedOptionNames()).toEqual(["Aangemaakt op", "Zaaknaam"]);
    });
  });

  describe("toggling columns", () => {
    it("emits, when the menu closes, the bound map with the toggled columns", async () => {
      const columns = makeColumns({
        [ZoekenColumn.SELECT]: ColumnPickerValue.STICKY,
        [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
        [ZoekenColumn.STARTDATUM]: ColumnPickerValue.HIDDEN,
        [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.VISIBLE,
      });
      const { columnsChanged } = await setup(columns);

      await openMenu();
      await user.click(screen.getByRole("option", { name: "Zaaknaam" }));
      await user.click(screen.getByRole("option", { name: "Startdatum" }));

      expect(columnsChanged).not.toHaveBeenCalled();

      await closeMenu();

      expect(columnsChanged).toHaveBeenCalledTimes(1);
      expect(columnsChanged.mock.calls[0][0]).toBe(columns);
      expect(columns).toEqual(
        makeColumns({
          [ZoekenColumn.SELECT]: ColumnPickerValue.STICKY,
          [ZoekenColumn.NAAM]: ColumnPickerValue.HIDDEN,
          [ZoekenColumn.STARTDATUM]: ColumnPickerValue.VISIBLE,
          [ZoekenColumn.CREATIEDATUM]: ColumnPickerValue.VISIBLE,
        }),
      );
    });

    it("writes a toggled column into the bound map before the menu closes", async () => {
      const columns = makeColumns({
        [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE,
      });
      await setup(columns);

      await openMenu();
      await user.click(screen.getByRole("option", { name: "Zaaknaam" }));

      expect(columns.get(ZoekenColumn.NAAM)).toBe(ColumnPickerValue.HIDDEN);
    });

    it("writes toggles into, and emits, the map the parent bound most recently", async () => {
      const { fixture, columnsChanged } = await setup(
        makeColumns({ [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE }),
      );
      const newColumns = makeColumns({
        [ZoekenColumn.STARTDATUM]: ColumnPickerValue.HIDDEN,
      });

      fixture.componentRef.setInput("columnSrc", newColumns);
      fixture.detectChanges();
      await openMenu();
      await user.click(screen.getByRole("option", { name: "Startdatum" }));
      await closeMenu();

      expect(columnsChanged).toHaveBeenCalledTimes(1);
      expect(columnsChanged.mock.calls[0][0]).toBe(newColumns);
      expect(newColumns.get(ZoekenColumn.STARTDATUM)).toBe(
        ColumnPickerValue.VISIBLE,
      );
    });

    it("does not emit when the menu closes without a toggled column", async () => {
      const { columnsChanged } = await setup(
        makeColumns({ [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE }),
      );

      await openMenu();
      await closeMenu();

      expect(columnsChanged).not.toHaveBeenCalled();
    });

    it("does not emit again when the menu is reopened and closed without a toggled column", async () => {
      const { columnsChanged } = await setup(
        makeColumns({ [ZoekenColumn.NAAM]: ColumnPickerValue.VISIBLE }),
      );

      await openMenu();
      await user.click(screen.getByRole("option", { name: "Zaaknaam" }));
      await closeMenu();
      await openMenu();
      await closeMenu();

      expect(columnsChanged).toHaveBeenCalledTimes(1);
    });
  });
});
