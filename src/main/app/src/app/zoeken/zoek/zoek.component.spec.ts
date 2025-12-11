import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ZoekComponent } from "./zoek.component";
import { MatSidenav } from "@angular/material/sidenav";
import {EventEmitter, NO_ERRORS_SCHEMA} from "@angular/core";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekVeld } from "../model/zoek-veld";
import { TranslateModule } from "@ngx-translate/core";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Subject } from "rxjs";
import {MatPaginator, PageEvent} from "@angular/material/paginator";

describe("ZoekComponent", () => {
    let component: ZoekComponent;
    let fixture: ComponentFixture<ZoekComponent>;

    const mockPaginator: Pick<MatPaginator, 'page' | 'pageIndex' | 'pageSize' | 'length'> = {
        page: new EventEmitter<PageEvent>(),
        pageIndex: 0,
        pageSize: 10,
        length: 0,
    };

    const mockSidenav: Pick<MatSidenav, 'open' | 'close' | 'openedStart'> = {
        open: jest.fn(),
        close: jest.fn(),
        openedStart: new Subject<void>(),
    };


    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot()],
            declarations: [ZoekComponent],
            providers: [
                { provide: MatSidenav, useValue: mockSidenav },
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(ZoekComponent);
        component = fixture.componentInstance;

        Object.defineProperty(component, "paginator", { get: () => () => mockPaginator });
        Object.defineProperty(component, "zoekenSideNav", { get: () => () => mockSidenav });

        fixture.detectChanges();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });

    it("should pass sidenav instance to child components", () => {
        component.zoekResultaat.resultaten = [
            { type: "ZAAK" } as GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
            { type: "TAAK" } as GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
            { type: "DOCUMENT" } as GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
        ];

        fixture.detectChanges();

        expect(component.zoekenSideNav()).toBe(mockSidenav);
    });
});
