# Architecture

Only local PostgreSQL provisioning is currently prepared. Services below describe the intended boundaries; none is running yet.

```mermaid
flowchart LR
    Browser --> Frontend[Frontend / reverse proxy]
    Frontend --> Statistics[statistics-service]
    Frontend --> Parking[parking-service]
    Statistics --> StatisticsDB[(statistics database)]
    Parking -. optional static metadata .-> ParkingDB[(parking database)]
    Parking --> Cache[Caffeine: latest availability]
```

Backends remain independent Spring Boot applications. A small common JAR will contain only genuinely shared code. Official data fetching will be disabled by default locally and in tests. No source integrations or schedules have been enabled.
