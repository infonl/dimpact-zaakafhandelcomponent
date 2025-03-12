/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { SessionStorageUtil } from "./session-storage.util";
const localStorageMock = (() => {
  let store = {};

  return {
    getItem(key) {
      return store[key] || null;
    },
    setItem(key, value) {
      store[key] = value.toString();
    },
    removeItem(key) {
      delete store[key];
    },
    clear() {
      store = {};
    },
  };
})();

describe("SessionStorageService", () => {
  beforeEach(() => {
    Object.defineProperty(window, "sessionStorage", {
      value: localStorageMock,
    });
    TestBed.configureTestingModule({
      providers: [SessionStorageUtil],
    }).compileComponents();
    TestBed.inject(SessionStorageUtil);
  });

  afterEach(() => {
    SessionStorageUtil.clearSessionStorage();
  });

  it("should return a gebruikersnaam with naam Jaap", () => {
    const gebruiker = { gebruikersnaam: "Jaap" };
    const gebruikerJSON = JSON.stringify(gebruiker);

    const getItemSpy = jest.spyOn(window.sessionStorage, "getItem");

    SessionStorageUtil.setItem("gebruikersnaam", gebruikerJSON);

    expect(SessionStorageUtil.getItem("gebruikersnaam")).toEqual(gebruikerJSON);
    expect(getItemSpy).toHaveBeenCalled();
  });

  it("key value v should be equal to v", () => {
    expect(SessionStorageUtil.getItem("k", "v")).toEqual("v");
  });

  it("should return Jaap", () => {
    const naam = SessionStorageUtil.setItem("gebruikersnaam", "Jaap");
    expect(naam).toBe("Jaap");
  });

  it("should break the reference", () => {
    jest.spyOn(JSON, "parse");
    jest.spyOn(JSON, "stringify");
    jest.spyOn(sessionStorage, "setItem");

    const gebruiker = SessionStorageUtil.getItem("", "Jaap");

    expect(JSON.parse).toHaveBeenCalled();
    expect(JSON.stringify).toHaveBeenCalled();
    expect(window.sessionStorage.setItem).toHaveBeenCalled();

    expect(gebruiker).toEqual("Jaap");
  });

  // session storage word momenteel niet leeggegooid. Echter, in de afterAll() gebeurt dit wel
  it("should call clear", () => {
    jest.spyOn(sessionStorage, "clear");
    SessionStorageUtil.setItem("testWaarde", "waarde");
    SessionStorageUtil.clearSessionStorage();
    expect(sessionStorage.clear).toHaveBeenCalled();
    expect(SessionStorageUtil.getItem("testWaarde")).toBe(null);
  });
});
