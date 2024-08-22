/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { of } from "rxjs";

import { MatSnackBar } from "@angular/material/snack-bar";
import { MatDialog } from "@angular/material/dialog";
import { Title } from "@angular/platform-browser";
import { UtilService } from "./util.service";
import { TranslateService } from "@ngx-translate/core";

describe("UtilService", () => {
  let service: UtilService;
  let titleService: Title;
  let dialog: MatDialog;
  let translateService: TranslateService;

  beforeEach(() => {
    const titleServiceMock = {
      setTitle: jest.fn(),
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
        MatSnackBar,
        { provide: MatDialog, useValue: dialogMock },
        { provide: TranslateService, useValue: translateServiceMock },
        { provide: Title, useValue: titleServiceMock },
      ],
      imports: [],
    });

    service = TestBed.inject(UtilService);
    titleService = TestBed.inject(Title);
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

    // Mock the behavior of the translate service
    (translateService.instant as jest.Mock).mockImplementation(
      (key: string, params?: {}) => {
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
});
