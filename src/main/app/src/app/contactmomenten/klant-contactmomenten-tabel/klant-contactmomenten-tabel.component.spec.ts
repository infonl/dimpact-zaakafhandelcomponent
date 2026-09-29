/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { Subject } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";
import { KlantContactmomentenTabelComponent } from "./klant-contactmomenten-tabel.component";

const makeContactmoment = (
  fields: Partial<GeneratedType<"RestContactmoment">> = {},
) =>
  fromPartial<GeneratedType<"RestContactmoment">>({
    registratiedatum: null,
    kanaal: null,
    tekst: null,
    initiatiefnemer: null,
    medewerker: null,
    ...fields,
  });

const makeResultaat = (
  resultaten: GeneratedType<"RestContactmoment">[] = [],
  totaal = 0,
) =>
  fromPartial<GeneratedType<"RESTResultaatRestContactmoment">>({
    resultaten,
    totaal,
  });

const contactmomenten = [
  makeContactmoment({
    kanaal: "telefoon",
    initiatiefnemer: "burger",
    medewerker: "fakeMedewerker1",
    tekst: "fakeTekst1",
  }),
  makeContactmoment({
    kanaal: "email",
    initiatiefnemer: "gemeente",
    medewerker: "fakeMedewerker2",
    tekst: "fakeTekst2",
  }),
];

describe(KlantContactmomentenTabelComponent.name, () => {
  const user = userEvent.setup();

  let fixture: ComponentFixture<KlantContactmomentenTabelComponent>;
  let listContactmomenten: jest.SpyInstance;
  let setLoading: jest.SpyInstance;
  let pendingResultaat: Subject<
    GeneratedType<"RESTResultaatRestContactmoment">
  >;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        KlantContactmomentenTabelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    listContactmomenten = jest
      .spyOn(TestBed.inject(ContactmomentenService), "listContactmomenten")
      .mockImplementation(() => {
        pendingResultaat = new Subject();
        return pendingResultaat.asObservable();
      });
    setLoading = jest
      .spyOn(TestBed.inject(UtilService), "setLoading")
      .mockImplementation(() => undefined);

    fixture = TestBed.createComponent(KlantContactmomentenTabelComponent);
  });

  function initialiseWith(inputs: { bsn?: string; vestigingsnummer?: string }) {
    Object.entries(inputs).forEach(([name, value]) =>
      fixture.componentRef.setInput(name, value),
    );
    fixture.detectChanges();
  }

  function respondWith(
    resultaat: GeneratedType<"RESTResultaatRestContactmoment">,
  ) {
    pendingResultaat.next(resultaat);
    fixture.detectChanges();
    fixture.detectChanges();
  }

  describe("the request for the contactmomenten", () => {
    it("requests the first page of contactmomenten of the persoon with the given bsn", () => {
      initialiseWith({ bsn: "999993896" });

      expect(listContactmomenten).toHaveBeenCalledTimes(1);
      expect(listContactmomenten).toHaveBeenLastCalledWith({
        page: 0,
        bsn: "999993896",
        vestigingsnummer: undefined,
      });
    });

    it("requests the first page of contactmomenten of the vestiging with the given vestigingsnummer", () => {
      initialiseWith({ vestigingsnummer: "000099998888" });

      expect(listContactmomenten).toHaveBeenCalledTimes(1);
      expect(listContactmomenten).toHaveBeenLastCalledWith({
        page: 0,
        bsn: undefined,
        vestigingsnummer: "000099998888",
      });
    });

    it("requests the contactmomenten without a bsn or vestigingsnummer when neither is given", () => {
      initialiseWith({});

      expect(listContactmomenten).toHaveBeenCalledTimes(1);
      expect(listContactmomenten).toHaveBeenLastCalledWith({
        page: 0,
        bsn: undefined,
        vestigingsnummer: undefined,
      });
    });

    it("requests the next page when the user pages forward", async () => {
      initialiseWith({ bsn: "999993896" });
      respondWith(makeResultaat(contactmomenten, 12));

      await user.click(screen.getByRole("button", { name: "Next page" }));

      expect(listContactmomenten).toHaveBeenCalledTimes(2);
      expect(listContactmomenten).toHaveBeenLastCalledWith({
        page: 1,
        bsn: "999993896",
        vestigingsnummer: undefined,
      });
    });
  });

  describe("while the contactmomenten load", () => {
    beforeEach(() => initialiseWith({ bsn: "999993896" }));

    it("shows a loading message", () => {
      expect(screen.getByText("msg.loading")).toBeInTheDocument();
    });

    it("turns on the loading indicator", () => {
      expect(setLoading).toHaveBeenCalledWith(true);
      expect(setLoading).not.toHaveBeenCalledWith(false);
    });
  });

  describe("once the contactmomenten are loaded", () => {
    beforeEach(() => {
      initialiseWith({ bsn: "999993896" });
      respondWith(makeResultaat(contactmomenten, 12));
    });

    it("shows a column for each field of a contactmoment", () => {
      for (const column of [
        "contactmoment.registratiedatum",
        "contactmoment.kanaal",
        "contactmoment.initiatiefnemer",
        "contactmoment.medewerker",
        "contactmoment.tekst",
      ]) {
        expect(
          screen.getByRole("columnheader", { name: column }),
        ).toBeInTheDocument();
      }
    });

    it("lists every returned contactmoment", () => {
      expect(
        screen.getByRole("row", {
          name: /telefoon burger fakeMedewerker1 fakeTekst1/,
        }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("row", {
          name: /email gemeente fakeMedewerker2 fakeTekst2/,
        }),
      ).toBeInTheDocument();
    });

    it("shows the total number of contactmomenten in the paginator", () => {
      expect(screen.getByText("1 – 5 of 12")).toBeInTheDocument();
    });

    it("hides the loading message", () => {
      expect(screen.queryByText("msg.loading")).not.toBeInTheDocument();
    });

    it("turns off the loading indicator", () => {
      expect(setLoading).toHaveBeenLastCalledWith(false);
    });
  });

  describe("when no contactmomenten are returned", () => {
    beforeEach(() => {
      initialiseWith({ bsn: "999993896" });
      respondWith(makeResultaat([], 0));
    });

    it("shows that there is no data", () => {
      expect(
        screen.getByText("msg.geen.gegevens.gevonden"),
      ).toBeInTheDocument();
    });

    it("hides the loading message", () => {
      expect(screen.queryByText("msg.loading")).not.toBeInTheDocument();
    });
  });

  describe("when a resultaat has no resultaten and no totaal", () => {
    beforeEach(() => {
      initialiseWith({ bsn: "999993896" });
      respondWith(
        fromPartial<GeneratedType<"RESTResultaatRestContactmoment">>({}),
      );
    });

    it("shows that there is no data", () => {
      expect(
        screen.getByText("msg.geen.gegevens.gevonden"),
      ).toBeInTheDocument();
    });

    it("shows a total of zero in the paginator", () => {
      expect(screen.getByText("0 of 0")).toBeInTheDocument();
    });
  });

  describe("when an input changes after the first render", () => {
    beforeEach(async () => {
      initialiseWith({ bsn: "999993896", vestigingsnummer: "000099998888" });
      respondWith(makeResultaat(contactmomenten, 12));
      await user.click(screen.getByRole("button", { name: "Next page" }));
      respondWith(makeResultaat(contactmomenten, 12));
      listContactmomenten.mockClear();
    });

    it("reloads the first page, still for the bsn it was first rendered with, when the bsn changes", () => {
      fixture.componentRef.setInput("bsn", "999990408");
      fixture.detectChanges();

      expect(listContactmomenten).toHaveBeenCalledTimes(1);
      expect(listContactmomenten).toHaveBeenLastCalledWith({
        page: 0,
        bsn: "999993896",
        vestigingsnummer: "000099998888",
      });
    });

    it("reloads the first page, still for the vestigingsnummer it was first rendered with, when the vestigingsnummer changes", () => {
      fixture.componentRef.setInput("vestigingsnummer", "000011112222");
      fixture.detectChanges();

      expect(listContactmomenten).toHaveBeenCalledTimes(1);
      expect(listContactmomenten).toHaveBeenLastCalledWith({
        page: 0,
        bsn: "999993896",
        vestigingsnummer: "000099998888",
      });
    });

    it("shows the first page in the paginator after the reload", () => {
      fixture.componentRef.setInput("bsn", "999990408");
      fixture.detectChanges();
      respondWith(makeResultaat(contactmomenten, 12));

      expect(screen.getByText("1 – 5 of 12")).toBeInTheDocument();
    });
  });
});
