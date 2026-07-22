/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  ViewChild,
} from "@angular/core";
import * as style from "ol/style.js";
import { OpenLayersLocationMap } from "../../shared/location/open-layers-location-map";
import { LocationUtil } from "../../shared/location/location-util";
import {
  AddressResult,
  LocationService,
} from "../../shared/location/location.service";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { GeometryType } from "../model/geometryType";

@Component({
  selector: "zac-locatie-tonen",
  templateUrl: "./zaak-locatie-tonen.component.html",
  styleUrls: ["./zaak-locatie-tonen.component.less"],
  standalone: true,
  imports: [StaticTextComponent],
})
export class LocatieTonenComponent implements AfterViewInit, OnChanges {
  @Input({ required: true }) currentLocation!: GeneratedType<"RestGeometry">;

  @ViewChild("openLayersMap", { static: true }) openLayersMapRef!: ElementRef;

  protected nearestAddress?: AddressResult;

  private readonly pointStyle = new style.Style({
    text: new style.Text({
      text: "location_on",
      font: '500 32px "Material Symbols Outlined"',
      fill: new style.Fill({
        color: "#ff0000",
      }),
      offsetY: -15,
    }),
  });

  private readonly locationMap = new OpenLayersLocationMap(this.pointStyle);

  constructor(private readonly locationService: LocationService) {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.locationMap.setTarget(this.openLayersMapRef.nativeElement);
    }, 0);

    if (this.currentLocation) {
      this.setLocation(this.currentLocation);
    }
  }

  ngOnChanges(): void {
    if (this.currentLocation) {
      this.setLocation(this.currentLocation);
    }
  }

  private setLocation(geometry?: GeneratedType<"RestGeometry">) {
    this.locationMap.clearMarkers();

    switch (geometry?.type) {
      case GeometryType.POINT: {
        if (!geometry?.point) return;

        const coordinate = LocationUtil.pointToCoordinate(geometry.point);
        this.locationMap.addMarker(coordinate);
        this.locationMap.zoomToMarker(coordinate);
        this.locationService
          .coordinateToAddress(coordinate)
          .subscribe((objectData) => {
            this.nearestAddress = objectData.response.docs[0];
          });
      }
    }
  }
}
