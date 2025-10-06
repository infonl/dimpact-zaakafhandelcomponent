import { Component, computed, input } from "@angular/core";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { lastValueFrom } from "rxjs";
import { KlantenService } from "../../klanten/klanten.service";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { buildBedrijfRouteLink } from "../../klanten/klanten-routing.module";
import { GeneratedType } from "src/app/shared/utils/generated-types";

@Component({
  selector: "betrokkene-link",
  templateUrl: "./betrokkene-link.html",
  styleUrls: [],
})
export class BetrokkeneLinkComponent {
  protected readonly betrokkeneQuery = injectQuery(() => ({
    enabled: !!this.betrokkene(),
    queryKey: ["bedrijf", this.betrokkene()?.identificatie],
    queryFn: async () => {
      const betrokkene = this.betrokkene();
      if (!betrokkene) throw new Error("No betrokkene provided");

      if (betrokkene.type === "BSN") {
        const persoon = await lastValueFrom(
          this.klantService.readPersoon(betrokkene.identificatie, {
            context: "BetrokkeneLinkComponent",
            action: "view",
          })
        );
        return new BetrokkeneIdentificatie(persoon);
      }

      console.log(betrokkene)
      const bedrijf = await lastValueFrom(
        this.klantService.readBedrijf(new BetrokkeneIdentificatie(betrokkene))
      );
      return new BetrokkeneIdentificatie(bedrijf);
    },
  }));

  protected readonly betrokkene =
    input.required<GeneratedType<"RestZaakBetrokkene">>();

  protected readonly bedrijfRouteLink = computed(() =>
    buildBedrijfRouteLink(this.betrokkene())
  );

  constructor(private readonly klantService: KlantenService) {}
}
