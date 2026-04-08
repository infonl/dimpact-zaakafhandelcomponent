/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import {
  ComponentFixture,
  fakeAsync,
  TestBed,
  tick,
} from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "src/test-helpers";
import { Subject } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";
import { KlantContactmomentenTabelComponent } from "./klant-contactmomenten-tabel.component";

const makeContactmoment = (
  fields: Partial<GeneratedType<"RestContactmoment">> = {},
): GeneratedType<"RestContactmoment"> =>
  ({
    registratiedatum: null,
    kanaal: null,
    tekst: null,
    initiatiefnemer: null,
    medewerker: null,
    ...fields,
  }) as Partial<
    GeneratedType<"RestContactmoment">
  > as unknown as GeneratedType<"RestContactmoment">;

const makeResultaat = (
  resultaten: GeneratedType<"RestContactmoment">[] = [],
  totaal = 0,
): GeneratedType<"RESTResultaatRestContactmoment"> =>
  fromPartial<GeneratedType<"RESTResultaatRestContactmoment">>({
    resultaten,
    totaal,
  });

describe(KlantContactmomentenTabelComponent.name, () => {
  let component: KlantContactmomentenTabelComponent;
  let fixture: ComponentFixture<KlantContactmomentenTabelComponent>;
  let contactmomentenService: ContactmomentenService;
  let utilService: UtilService;
  let listSubject: Subject<GeneratedType<"RESTResultaatRestContactmoment">>;

  beforeEach(async () => {
    listSubject = new Subject();

    await TestBed.configureTestingModule({
      imports: [
        KlantContactmomentenTabelComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    contactmomentenService = TestBed.inject(ContactmomentenService);
    utilService = TestBed.inject(UtilService);

    jest
      .spyOn(contactmomentenService, "listContactmomenten")
      .mockReturnValue(listSubject.asObservable());
    jest.spyOn(utilService, "setLoading").mockImplementation(() => undefined);

    fixture = TestBed.createComponent(KlantContactmomentenTabelComponent);
    component = fixture.componentInstance;
  });

  describe("ngOnInit", () => {
    it("sets bsn on listParameters from input", () => {
      component.bsn = "123456789";
      component.ngOnInit();

      expect(component["listParameters"].bsn).toBe("123456789");
    });

    it("sets vestigingsnummer on listParameters from input", () => {
      component.vestigingsnummer = "000099998888";
      component.ngOnInit();

      expect(component["listParameters"].vestigingsnummer).toBe("000099998888");
    });

    it("sets both bsn and vestigingsnummer to undefined when inputs are not provided", () => {
      component.ngOnInit();

      expect(component["listParameters"].bsn).toBeUndefined();
      expect(component["listParameters"].vestigingsnummer).toBeUndefined();
    });
  });

  describe("after detectChanges (AfterViewInit triggered)", () => {
    beforeEach(fakeAsync(() => {
      component.bsn = "999993896";
      fixture.detectChanges();
      tick(0);
    }));

    it("sets isLoadingResults to true while loading", () => {
      expect(component["isLoadingResults"]).toBe(true);
    });

    it("calls utilService.setLoading(true) when loading starts", () => {
      expect(utilService.setLoading).toHaveBeenCalledWith(true);
    });

    it("calls listContactmomenten with bsn from ngOnInit", () => {
      expect(contactmomentenService.listContactmomenten).toHaveBeenCalledWith(
        expect.objectContaining({ bsn: "999993896" }),
      );
    });

    describe("after data arrives", () => {
      const contactmoments = [
        makeContactmoment({
          kanaal: "telefoon",
          initiatiefnemer: "burger",
          medewerker: "jan.de.vries",
          tekst: "Vraag over aanvraag",
        }),
        makeContactmoment({
          kanaal: "email",
          initiatiefnemer: "gemeente",
          medewerker: "piet.pietersen",
          tekst: "Bevestiging ontvangen",
        }),
      ];

      beforeEach(fakeAsync(() => {
        listSubject.next(makeResultaat(contactmoments, 2));
        tick(0);
        fixture.detectChanges();
      }));

      it("populates dataSource with returned contactmomenten", () => {
        expect(component["dataSource"].data).toHaveLength(2);
        expect(component["dataSource"].data[0].kanaal).toBe("telefoon");
        expect(component["dataSource"].data[1].kanaal).toBe("email");
      });

      it("sets isLoadingResults to false after data arrives", () => {
        expect(component["isLoadingResults"]).toBe(false);
      });

      it("calls utilService.setLoading(false) after data arrives", () => {
        expect(utilService.setLoading).toHaveBeenCalledWith(false);
      });

      it("sets paginator.length to totaal from resultaat", () => {
        expect(component["paginator"].length).toBe(2);
      });
    });

    describe("when resultaat has no resultaten", () => {
      beforeEach(fakeAsync(() => {
        listSubject.next(makeResultaat([], 0));
        tick(0);
        fixture.detectChanges();
      }));

      it("sets dataSource.data to empty array", () => {
        expect(component["dataSource"].data).toHaveLength(0);
      });

      it("sets paginator.length to 0", () => {
        expect(component["paginator"].length).toBe(0);
      });

      it("shows geen-gegevens paragraph in no-data row when not loading", () => {
        expect(component["isLoadingResults"]).toBe(false);
        const paragraphs = Array.from(
          fixture.nativeElement.querySelectorAll(
            "td p",
          ) as NodeListOf<HTMLElement>,
        );
        const texts = paragraphs.map((p) => p.textContent?.trim());
        expect(texts).toContain("msg.geen.gegevens.gevonden");
      });

      it("does not show loading paragraph when not loading", () => {
        const paragraphs = Array.from(
          fixture.nativeElement.querySelectorAll(
            "td p",
          ) as NodeListOf<HTMLElement>,
        );
        const texts = paragraphs.map((p) => p.textContent?.trim());
        expect(texts).not.toContain("msg.loading");
      });
    });
  });

  describe("columns", () => {
    it("defines the expected set of columns", () => {
      expect(component["columns"]).toEqual([
        "registratiedatum",
        "kanaal",
        "initiatiefnemer",
        "medewerker",
        "tekst",
      ]);
    });
  });

  describe("ngOnChanges", () => {
    it("does not reset paginator when init is false", () => {
      component["init"] = false;
      // Should not throw even without paginator initialised
      expect(() => component.ngOnChanges()).not.toThrow();
    });

    it("re-triggers load when init is true", fakeAsync(() => {
      component.bsn = "111111111";
      fixture.detectChanges();
      tick(0);
      // Resolve initial load
      listSubject.next(makeResultaat([], 0));
      tick(0);

      const listSpy = jest.spyOn(contactmomentenService, "listContactmomenten");
      listSpy.mockReturnValue(new Subject().asObservable());

      component.ngOnChanges();

      expect(component["paginator"].pageIndex).toBe(0);
    }));
  });

  describe("listParameters initial state", () => {
    it("initialises page to 0", () => {
      expect(component["listParameters"].page).toBe(0);
    });
  });
});
