# Observability

The Observability architecture of ZAC is based on [OpenTelemetry](https://opentelemetry.io/) and is illustrated in the following diagram:

```mermaid
C4Component
    title ZAC Observability Components diagram

    Person(Ops, "Operations", "An operations employee")

    Enterprise_Boundary(b0, "ZAC context") {
        System_Boundary(Observ, "Observability") {
            System(Grafana, "Grafana")
            System(Tempo, "Tempo")
            System(OTC, "OTEL Collector")
        }
        System_Boundary(ZAC, "ZAC components") {
            System(ZAC, "ZAC")
        }
    }
    UpdateElementStyle(Grafana, $bgColor="green", $borderColor="black")
    UpdateElementStyle(Tempo, $bgColor="green", $borderColor="black")
    UpdateElementStyle(OTC, $bgColor="green", $borderColor="black")

    Rel(ZAC, OTC, "Push", "Telemetry Data")
    Rel(OTC, Tempo, "Store", "Trace Data")
    Rel(Grafana, Tempo, "Query", "Trace Data")
    Rel(Ops, Grafana, "Monitor", "Dashboard")

```

The following components are part of the ZAC Observability architecture:

| Component                                                   | Description                                                     | ZAC usage                                                                                                           |
|-------------------------------------------------------------|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| [Grafana](https://grafana.com/)                             | Grafana monitoring dashboards for visualising monitored data.   | Used to visualise and track performance, usage and working of the ZAC application.  |
| [Tempo](https://grafana.com/docs/tempo/latest/)             | Tempo object database for traces.                               | Used to store the trace data coming from the ZAC application.                                                       |
| [OTEL Collector](https://opentelemetry.io/docs/collector/)  | OTEL Collector receives, processes and exports telemetry data.  | The ZAC application pushes it's telemetry data to the OTEL Collector, that then stores it into the Tempo database. | 


