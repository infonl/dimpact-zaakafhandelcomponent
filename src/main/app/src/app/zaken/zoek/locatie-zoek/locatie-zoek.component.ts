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
import * as proj from "ol/proj.js";
import * as source from "ol/source.js";
import * as style from "ol/style.js";
import WMTSTileGrid from "ol/tilegrid/WMTS.js";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { environment } from "src/environments/environment";
import { LocationUtil } from "../../../shared/location/location-util";
import {
  AddressResult,
  LocationService,
  SuggestResult,
} from "../../../shared/location/location.service";
import { Geometry } from "../../model/geometry";
import { GeometryGegevens } from "../../model/geometry-gegevens";
import { GeometryType } from "../../model/geometryType";

@Component({
  selector: "zac-locatie-zoek",
  templateUrl: "./locatie-zoek.component.html",
  styleUrls: ["./locatie-zoek.component.less"],
})
export class LocatieZoekComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input({ required: true }) currentLocation!: Geometry;
  @Input() readonly = false;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Input({ required: true }) reasonControl!: FormControl<string>;
  @Output() locatie = new EventEmitter<GeometryGegevens>();
  @Output() locationChanged = new EventEmitter<Geometry | undefined>();
  @ViewChild("openLayersMap", { static: true }) openLayersMapRef: ElementRef;
  markerLocatie: Geometry;
  nearestAddress: AddressResult;
  searchControl = new FormControl();
  searchResults: SuggestResult[] = [];
  initialLocation: Geometry;

  private unsubscribe$: Subject<void> = new Subject<void>();

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

  private layers: any[];

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

  constructor(private locationService: LocationService) {}

  ngOnInit(): void {
    this.initialLocation = this.currentLocation;
    const projection = proj.get(this.EPSG3857);
    const projectionExtent = projection.getExtent();
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

    this.searchControl.valueChanges
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.searchAddresses(value);
      });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.map.setTarget(this.openLayersMapRef.nativeElement);
    }, 0);

    if (!this.readonly) {
      this.map.on("click", (event) => {
        const coordinate: Array<number> = proj.transform(
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

    if (this.currentLocation) {
      this.setLocation(this.currentLocation, false);
    }
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  private searchAddresses(query): void {
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
    this.setLocation();
  }

  private setLocation(geometry?: Geometry, fromSearch = true) {
    this.markerLocatie = geometry;
    this.clearPreviousMarker();
    this.searchControl.reset();

    switch (geometry?.type) {
      case GeometryType.POINT: {
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

    if (JSON.stringify(geometry) === JSON.stringify(this.initialLocation)) {
      return;
    }
    this.locationChanged.emit(geometry);
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
    this.currentLocation = this.initialLocation;
    this.locationChanged.emit(this.initialLocation);
    void this.sideNav.close();
  }

  save(): void {
    this.initialLocation = this.currentLocation;
    this.locatie.next(
      new GeometryGegevens(this.markerLocatie, this.reasonControl.value),
    );
  }
}
