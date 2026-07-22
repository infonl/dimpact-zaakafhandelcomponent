/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgClass, NgFor, NgIf } from "@angular/common";
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from "@angular/material/autocomplete";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import * as proj from "ol/proj.js";
import * as style from "ol/style.js";
import { BehaviorSubject, Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { LocationUtil } from "../../shared/location/location-util";
import {
  AddressResult,
  LocationService,
  SuggestResult,
} from "../../shared/location/location.service";
import { OpenLayersLocationMap } from "../../shared/location/open-layers-location-map";
import { LocationPipe } from "../../shared/pipes/location.pipe";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GeometryGegevens } from "../model/geometry-gegevens";
import { GeometryType } from "../model/geometryType";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-case-location-edit",
  templateUrl: "./zaak-locatie-wijzigen.component.html",
  styleUrls: ["./zaak-locatie-wijzigen.component.less"],
  standalone: true,
  imports: [
    AsyncPipe,
    LocationPipe,
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatToolbarModule,
    NgClass,
    NgFor,
    NgIf,
    ReactiveFormsModule,
    StaticTextComponent,
    TranslateModule,
    ZacFormActions,
  ],
})
export class CaseLocationEditComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() locatie = new EventEmitter<GeometryGegevens | null>();

  @ViewChild("openLayersMap", { static: true }) openLayersMapRef!: ElementRef;

  private readonly zakenService = inject(ZakenService);
  private readonly locationService = inject(LocationService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  // markerLocatie?: GeneratedType<"RestGeometry">;
  markerLocatie$ = new BehaviorSubject<GeneratedType<"RestGeometry"> | null>(
    null,
  );
  nearestAddress!: AddressResult;
  searchControl = new FormControl();
  reasonControl = new FormControl();
  searchResults: SuggestResult[] = [];

  protected readonly form = new FormGroup({ reason: this.reasonControl });

  protected readonly mutation = injectMutation(() => ({
    ...this.zakenService.updateZaakLocatie(this.zaak.uuid),
    onSuccess: () => {
      this.locatie.emit();
      void this.sideNav.close();
    },
    onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
  }));

  private unsubscribe$: Subject<void> = new Subject<void>();
  protected readonly: boolean = false;

  private readonly pointStyle = new style.Style({
    text: new style.Text({
      text: "location_on",
      font: '640 32px "Material Symbols Outlined"',
      fill: new style.Fill({
        color: "#ff0000",
      }),
      offsetY: -15,
    }),
  });

  private readonly locationMap = new OpenLayersLocationMap(this.pointStyle);

  ngOnInit(): void {
    this.readonly = !this.zaak.rechten.wijzigenLocatie;
    this.reasonControl.disable();

    this.searchControl.valueChanges
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.searchAddresses(value);
      });

    this.markerLocatie$
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((geometry) => {
        if (LocationUtil.isSameGeometry(geometry, this.zaak.zaakgeometrie)) {
          this.disableReasonControl();
        } else {
          this.reasonControl.enable();
        }
      });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.locationMap.setTarget(this.openLayersMapRef.nativeElement);
    }, 0);

    if (!this.readonly) {
      this.locationMap.map.on("click", (event) => {
        const coordinate = proj.transform(
          event.coordinate,
          "EPSG:3857",
          "EPSG:4326",
        );
        this.setLocation(LocationUtil.coordinateToPoint(coordinate), false);
      });
    }

    this.locationMap.map.on("click", () => {
      this.openLayersMapRef.nativeElement.focus();
    });

    this.locationMap.map.on("pointerdrag", () => {
      this.openLayersMapRef.nativeElement.focus();
    });

    if (this.zaak.zaakgeometrie) {
      this.setLocation(this.zaak.zaakgeometrie, false);
    }
  }

  private searchAddresses(query: string): void {
    if (query) {
      this.locationService.addressSuggest(query).subscribe((data) => {
        this.searchResults = data.response.docs;
      });
    }
  }

  displayAddress = (result: SuggestResult): string => {
    return result?.weergavenaam;
  };

  selectAddress($event: MatAutocompleteSelectedEvent): void {
    this.locationService
      .addressLookup($event.option.value.id)
      .subscribe((objectData) => {
        this.nearestAddress = objectData.response.docs[0];
        this.setLocation(
          LocationUtil.wktToPoint(this.nearestAddress.centroide_ll),
          true,
        );
      });
  }

  resetLocation() {
    this.setLocation();
    this.locationMap.resetView();
    this.nearestAddress = {} as AddressResult;
  }

  private disableReasonControl() {
    this.reasonControl.disable();
    this.reasonControl.reset();
  }

  private setLocation(
    geometry?: GeneratedType<"RestGeometry">,
    fromSearch = true,
  ) {
    this.markerLocatie$.next(geometry ?? null);
    this.locationMap.clearMarkers();
    this.searchControl.reset();

    switch (geometry?.type) {
      case GeometryType.POINT: {
        if (!geometry?.point) return;

        const coordinate = LocationUtil.pointToCoordinate(geometry.point);
        this.locationMap.addMarker(coordinate);
        if (fromSearch) {
          this.locationMap.zoomToMarker(coordinate);
        } else {
          if (
            (this.locationMap.currentZoom() ?? 0) <
            OpenLayersLocationMap.MAX_ZOOM
          ) {
            this.locationMap.zoomToMarker(coordinate);
          }
          this.locationService
            .coordinateToAddress(coordinate)
            .subscribe((objectData) => {
              this.nearestAddress = objectData.response.docs[0];
            });
        }
      }
    }
  }

  cancel(): void {
    void this.sideNav.close();
  }

  save(): void {
    this.mutation.mutate({
      reden: this.reasonControl.value,
      geometrie: this.markerLocatie$.getValue(),
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }
}
