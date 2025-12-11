# Observability

The Observability architecture of ZAC is based on [OpenTelemetry](https://opentelemetry.io/) and is illustrated in the following diagram:

```mermaid
C4Component
    title ZAC Observability Components diagram

    Person(employee, "Employee", "An employee of a municipality")
    Person(operations, "Operations", "An operations employee")

    System(zac, "ZAC")
    
    Enterprise_Boundary(observerability, "Observability services") {         
        System(grafana, "Grafana")
        System(tempo, "Tempo")
        System(prometheus, "Prometheus")
        System(openTelemetry, "Open Telemetry Collector")
    }
    
    Rel(operations, grafana, "Monitor", "Dashboard")
    Rel(employee, zac, "Handles cases")
    Rel(zac, openTelemetry, "Push", "Telemetry data")
    Rel(openTelemetry, tempo, "Store", "Trace data")
    Rel(prometheus, zac, "Pull", "Metrics data")
    Rel(grafana, tempo, "Query", "Trace data")
    Rel(grafana, prometheus, "Query", "Metrics data")

    UpdateElementStyle(grafana, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(tempo, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(openTelemetry, $bgColor="grey", $borderColor="grey")
    UpdateElementStyle(prometheus, $bgColor="grey", $borderColor="grey")
    
    UpdateLayoutConfig($c4ShapeInRow="2")
```

Note that there are several ways to set up an observability architecture. 
The above diagram illustrates just one way of doing this, and is used in our local Docker Compose setup.
For example, instead of having Prometheus scrape metrics directly from the ZAC application, the Open Telemetry Collector could also be used to receive metrics data
and pass it on to Prometheus.

The following components are part of the ZAC Observability architecture:

| Component                                                           | Description                                                                | ZAC usage                                                                                                                               |
|---------------------------------------------------------------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| [Grafana](https://grafana.com/)                                     | Grafana monitoring dashboards for visualising monitored data.              | Used to visualise and track performance, usage and working of the ZAC application.                                                      |
| [Tempo](https://grafana.com/docs/tempo/latest/)                     | Tempo object database for traces.                                          | Used to store the trace data coming from the ZAC application.                                                                           |
| [Prometheus](https://prometheus.io/)                 | Metrics store (and monitoring solution).               | Used to store metrics coming from the ZAC application.                                                                                  |
| [Open Telemetry Collector](https://opentelemetry.io/docs/collector/) | The Open Telemetry (OTEL) Collector receives and processes telemetry data. | The ZAC application pushes its telemetry data (metrics and traces) to the OTEL Collector, that then stores it into the Tempo database. | 


