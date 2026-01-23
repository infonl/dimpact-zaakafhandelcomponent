/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { Observable, of } from "rxjs";

import { Signal } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSnackBar } from "@angular/material/snack-bar";
import { Title } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { ProgressDialogComponent } from "src/app/shared/progress-dialog/progress-dialog.component";
import { UtilService } from "./util.service";

class MatSnackBarRefMock {
  onAction = jest.fn().mockReturnValue(of(0));
}

describe(UtilService.name, () => {
  let service: UtilService;
  let titleService: Title;
  let snackbar: MatSnackBar;
  let dialog: MatDialog;
  let translateService: TranslateService;

  beforeEach(() => {
    const titleServiceMock = {
      setTitle: jest.fn(),
    };

    const snackbarMock = {
      open: jest.fn().mockReturnValue(new MatSnackBarRefMock()),
    };

    const dialogMock = {
      open: jest.fn(),
    };

    const translateServiceMock = {
      instant: jest.fn((message: string) => message),
      get: jest.fn().mockReturnValue(of("")), // Mocking the get method to return an observable
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: MatSnackBar, useValue: snackbarMock },
        { provide: MatDialog, useValue: dialogMock },
        { provide: TranslateService, useValue: translateServiceMock },
        { provide: Title, useValue: titleServiceMock },
      ],
      imports: [],
    });

    service = TestBed.inject(UtilService);
    titleService = TestBed.inject(Title);
    snackbar = TestBed.inject(MatSnackBar);
    dialog = TestBed.inject(MatDialog);
    translateService = TestBed.inject(TranslateService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should set the title correctly using translate and update headerTitle", () => {
    // Arrange
    const title = "pageTitle";
    const translatedTitle = "Translated Page Title";
    const translatedFullTitle = "Translated Full Title";

    // Mock the behavior of the translation service
    (translateService.instant as jest.Mock).mockImplementation(
      (key: string) => {
        if (key === title) return translatedTitle;
        if (key === "title") return `Translated Full Title`;
        return key;
      },
    );

    const headerTitleSpy = jest.spyOn(service["headerTitle"], "next");

    // Act
    service.setTitle(title);

    // Assert
    expect(translateService.instant).toHaveBeenCalledWith(title, undefined);
    expect(translateService.instant).toHaveBeenCalledWith("title", {
      title: translatedTitle,
    });
    expect(titleService.setTitle).toHaveBeenCalledWith(translatedFullTitle);
    expect(headerTitleSpy).toHaveBeenCalledWith(translatedTitle);
  });

  it("should return an array of objects with labels and values from enum", () => {
    // Arrange
    const enumValue = { A: "FIRST", B: "SECOND" };
    const prefix = "enumPrefix";

    // Mocking translate.get to return an observable with a mock translation
    (translateService.get as jest.Mock).mockImplementation((key: string) => {
      switch (key) {
        case `${prefix}.FIRST`:
          return of("First Value");
        case `${prefix}.SECOND`:
          return of("Second Value");
        default:
          return of("");
      }
    });

    // Act
    const result = service.getEnumAsSelectList(prefix, enumValue);

    // Assert
    expect(translateService.get).toHaveBeenCalledTimes(2);
    expect(translateService.get).toHaveBeenCalledWith(`${prefix}.FIRST`);
    expect(translateService.get).toHaveBeenCalledWith(`${prefix}.SECOND`);

    // Expected result
    const expectedResult = [
      { label: "First Value", value: "A" },
      { label: "Second Value", value: "B" },
    ];

    expect(result).toEqual(expectedResult);
  });

  it("should return an array of objects with labels and values from enum, excluding specified values", () => {
    // Arrange
    const enumValue = { A: "FIRST", B: "SECOND", C: "THIRD" };
    const prefix = "enumPrefix";
    const exceptEnumValues: [string] = ["SECOND"]; // Use a tuple with one string

    // Mocking translate.get to return an observable with a mock translation
    (translateService.get as jest.Mock).mockImplementation((key: string) => {
      switch (key) {
        case `${prefix}.FIRST`:
          return of("First Value");
        case `${prefix}.THIRD`:
          return of("Third Value");
        default:
          return of("");
      }
    });

    // Act
    const result = service.getEnumAsSelectListExceptFor(
      prefix,
      enumValue,
      exceptEnumValues,
    );

    // Assert
    expect(translateService.get).toHaveBeenCalledTimes(2);
    expect(translateService.get).toHaveBeenCalledWith(`${prefix}.FIRST`);
    expect(translateService.get).toHaveBeenCalledWith(`${prefix}.THIRD`);
    expect(translateService.get).not.toHaveBeenCalledWith(`${prefix}.SECOND`);

    // Expected result
    const expectedResult = [
      { label: "First Value", value: "A" },
      { label: "Third Value", value: "C" },
    ];

    expect(result).toEqual(expectedResult);
  });

  it("should return the correct key for a given value from the enum", () => {
    // Arrange
    const enumValue = {
      A: "FIRST",
      B: "SECOND",
      C: "THIRD",
    };

    // Act & Assert
    expect(service.getEnumKeyByValue(enumValue, "FIRST")).toBe("A");
    expect(service.getEnumKeyByValue(enumValue, "SECOND")).toBe("B");
    expect(service.getEnumKeyByValue(enumValue, "THIRD")).toBe("C");
  });

  it("should return undefined for a value not in the enum", () => {
    // Arrange
    const enumValue = {
      A: "FIRST",
      B: "SECOND",
      C: "THIRD",
    };

    // Act & Assert
    expect(
      service.getEnumKeyByValue(enumValue, "NON_EXISTENT"),
    ).toBeUndefined();
  });

  it("should open snackbar with correct message and action and return onAction observable", () => {
    // Arrange
    const message = "test.message";
    const action = "test.action";
    const params = { key: "value" };
    const duration = 5;

    // Mock the translation results
    (translateService.instant as jest.Mock).mockImplementation(
      (key: string) => key,
    );

    // Act
    const result = service.openSnackbarAction(
      message,
      action,
      params,
      duration,
    );

    // Assert
    expect(snackbar.open).toHaveBeenCalledWith("test.message", "test.action", {
      panelClass: ["mat-snackbar"],
      duration: duration * 1000,
    });

    // Ensure the result is an instance of Observable
    expect(result).toBeInstanceOf(Observable);

    // Verify the onAction method was called
    result.subscribe({
      complete: () => {
        const snackbarRef = snackbar.open("test.message", "test.action", {
          panelClass: ["mat-snackbar"],
          duration: duration * 1000,
        });
        expect(snackbarRef.onAction).toHaveBeenCalled();
      },
    });
  });

  it("should open the progress dialog with correct data and return afterClosed observable", () => {
    // Arrange
    const progressPercentage: Signal<number> = jest.fn(
      () => 50,
    ) as unknown as Signal<number>; // Mock Signal<number>
    const dialogRefMock = {
      afterClosed: jest.fn().mockReturnValue(of(void 0)), // Mocking afterClosed() observable
    };
    (dialog.open as jest.Mock).mockReturnValue(dialogRefMock);

    const data = {
      progressPercentage,
      message: "Loading...",
    };

    // Act
    const result = service.openProgressDialog(data);

    // Assert
    expect(dialog.open).toHaveBeenCalledWith(ProgressDialogComponent, {
      data: {
        message: "Loading...", // Assuming no translation
        progressPercentage: data.progressPercentage,
      },
      disableClose: true,
      panelClass: "full-screen-dialog",
    });

    expect(result).toBe(dialogRefMock.afterClosed());
  });

  describe(UtilService.prototype.compare.name, () => {
    it.each([
      [1, 2, [], false],
      [2, 1, [], false],
      [1, 1, [], true],
      ["1", 1, [], false],
      [{ a: 1 }, { a: 1 }, ["a"], true],
      [{ a: 1 }, { a: 1 }, ["b"], false],
      [{ a: 1, b: "foo" }, { a: 1, b: "foo" }, ["b"], true],
      [{ a: 1, b: "foo" }, { a: 1, b: "bar" }, ["b"], false],
      [{ a: 1, b: "foo" }, { a: 1 }, ["b"], false],
      [{ a: 1 }, { a: 1, b: "bar" }, ["b"], false],
      [{ a: 20, b: "foo" }, { a: 18, b: "foo" }, ["a", "b"], true],
    ])(
      "when comparing %p with %p and looking for keys %p it should return %p",
      (a, b, keysToCheck, expected) => {
        expect(service.compare(a, b, keysToCheck)).toBe(expected);
      },
    );
  });
});
