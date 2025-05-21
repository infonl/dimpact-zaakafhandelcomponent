/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatAutocompleteSelectedEvent } from "@angular/material/autocomplete";
import { MatDrawer } from "@angular/material/sidenav";
import * as control from "ol/control.js";
import { Coordinate } from "ol/coordinate.js";
import * as extent from "ol/extent.js";
import * as geom from "ol/geom.js";
import * as ol from "ol/index.js";
import * as interaction from "ol/interaction.js";
import * as layer from "ol/layer.js";
import BaseLayer from "ol/layer/Base";
import * as proj from "ol/proj.js";
import * as source from "ol/source.js";
import * as style from "ol/style.js";
import WMTSTileGrid from "ol/tilegrid/WMTS.js";
import { BehaviorSubject, Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { environment } from "src/environments/environment";
import { LocationUtil } from "../../shared/location/location-util";
import {
  AddressResult,
  LocationService,
  SuggestResult,
} from "../../shared/location/location.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GeometryGegevens } from "../model/geometry-gegevens";
import { GeometryType } from "../model/geometryType";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-case-location-edit",
  templateUrl: "./zaak-locatie-wijzigen.component.html",
  styleUrls: ["./zaak-locatie-wijzigen.component.less"],
})
export class CaseLocationEditComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() locatie = new EventEmitter<GeometryGegevens | null>();

  @ViewChild("openLayersMap", { static: true }) openLayersMapRef: ElementRef;

  // markerLocatie?: GeneratedType<"RestGeometry">;
  markerLocatie$ = new BehaviorSubject<GeneratedType<"RestGeometry"> | null>(
    null,
  );
  nearestAddress: AddressResult;
  searchControl = new FormControl();
  reasonControl = new FormControl();
  searchResults: SuggestResult[] = [];

  private unsubscribe$: Subject<void> = new Subject<void>();
  protected readonly: boolean = false;

  private map: ol.Map;
  private view: ol.View;
  private locationSource: source.Vector;
  private readonly WGS84: string = "WGS84";
  private readonly EPSG3857: string = "EPSG:3857";
  private readonly EXTENT_MATRIX: number = 20;

  private readonly DEFAULT_ZOOM: number = 8;
  private readonly MAX_ZOOM: number = 14;

  // Default Center, middle of the Netherlands
  private readonly DEFAULT_CENTER: number[] = [631711.827985, 6856275.890632];

  private layers: BaseLayer[] = [];

  private defaultStyle: style.Style = new style.Style({
    fill: new style.Fill({
      color: "rgba(255, 255, 255, 0.5)",
    }),
    stroke: new style.Stroke({
      color: "#ff0000",
      width: 2,
    }),
  });

  private pointStyle: style.Style = new style.Style({
    text: new style.Text({
      text: "location_on",
      font: '640 32px "Material Symbols Outlined"',
      fill: new style.Fill({
        color: "#ff0000",
      }),
      offsetY: -15,
    }),
  });

  constructor(
    private zakenService: ZakenService,
    private locationService: LocationService,
  ) {}

  ngOnInit(): void {
    const projection = proj.get(this.EPSG3857);
    const projectionExtent = projection?.getExtent();
    const size = extent.getWidth(projectionExtent) / 256;
    const resolutions = new Array(this.EXTENT_MATRIX);
    const matrixIds = new Array(this.EXTENT_MATRIX);
    for (let z = 0; z < this.EXTENT_MATRIX; ++z) {
      resolutions[z] = size / Math.pow(2, z);
      matrixIds[z] = ("0" + z).slice(-2);
    }

    const brtsource = new source.WMTS({
      projection: projection,
      layer: "standaard",
      format: "image/png",
      url: environment.BACKGROUND_MAP_API_URL,
      matrixSet: this.EPSG3857,
      style: "",
      tileGrid: new WMTSTileGrid({
        origin: extent.getTopLeft(projectionExtent),
        resolutions: resolutions,
        matrixIds: matrixIds,
      }),
      attributions: ["© OpenLayers en PDOK"],
    });

    const brtLayer = new layer.Tile({
      source: brtsource,
    });

    this.locationSource = new source.Vector();
    const locationLayer = new layer.Vector({
      source: this.locationSource,
      style: this.defaultStyle,
    });

    this.layers = [brtLayer];
    this.layers.push(locationLayer);

    this.view = new ol.View({
      projection: proj.get(this.EPSG3857),
      center: this.DEFAULT_CENTER,
      constrainResolution: true,
      zoom: this.DEFAULT_ZOOM,
    });

    const interactions = interaction.defaults({
      onFocusOnly: true,
    });
    const controls = control.defaults({ zoom: false });

    this.map = new ol.Map({
      interactions: interactions,
      controls: controls,
      view: this.view,
      layers: this.layers,
    });

    const modify = new interaction.Modify({ source: this.locationSource });
    this.map.addInteraction(modify);

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
      this.map.setTarget(this.openLayersMapRef.nativeElement);
    }, 0);

    if (!this.readonly) {
      this.map.on("click", (event) => {
        const coordinate = proj.transform(
          event.coordinate,
          "EPSG:3857",
          "EPSG:4326",
        );
        this.setLocation(LocationUtil.coordinateToPoint(coordinate), false);
      });
    }

    this.map.on("click", () => {
      this.openLayersMapRef.nativeElement.focus();
    });

    this.map.on("pointerdrag", () => {
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

  clearLocation() {
    this.disableReasonControl();
    this.setLocation();
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
    this.clearPreviousMarker();
    this.searchControl.reset();

    switch (geometry?.type) {
      case GeometryType.POINT: {
        if (!geometry?.point) return;

        const coordinate = LocationUtil.pointToCoordinate(geometry.point);
        this.addMarker(coordinate);
        if (fromSearch) {
          this.zoomToMarker(coordinate);
        } else {
          if ((this.map.getView()?.getZoom() ?? 0) < this.MAX_ZOOM) {
            this.zoomToMarker(coordinate);
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

  private addMarker(coordinate: Coordinate) {
    const marker = new ol.Feature({
      geometry: new geom.Point(proj.fromLonLat(coordinate)),
    });
    marker.setStyle(this.pointStyle);
    this.locationSource.addFeature(marker);
  }

  private clearPreviousMarker() {
    const features = this.locationSource.getFeatures();
    features.forEach((feature) => this.locationSource.removeFeature(feature));
    this.locationSource.refresh();
  }

  private zoomToMarker(coordinate: Array<number>): void {
    const mapCenter: Array<number> = proj.transform(
      coordinate,
      "EPSG:4326",
      "EPSG:3857",
    );
    this.map.getView().setCenter(mapCenter);
    const locationExtent = this.locationSource.getExtent();
    this.map.getView().fit(locationExtent, {
      size: this.map.getSize(),
      maxZoom: this.MAX_ZOOM,
    });
  }

  cancel(): void {
    void this.sideNav.close();
  }

  save(): void {
    this.zakenService
      .updateZaakLocatie(
        this.zaak.uuid,
        this.reasonControl.value,
        this.markerLocatie$.getValue(),
      )
      .subscribe({
        next: () => {
          this.sideNav.close();
          this.locatie.emit();
        },
        error: (err) => {
          console.error("Failed to update location:", err);
        },
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }
}
