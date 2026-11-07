# data.gov.cy catalog snapshot

Snapshot of the national open data portal catalog, taken on **6 November 2026**, so dataset research does not have to re-crawl the portal. The files here are a research aid, not a production data source: services keep fetching their own sources.

| File | Contents |
| --- | --- |
| `all_datasets.json.gz` | Full DCAT-AP 2.0 export (1957 datasets, 4056 distributions), gzip of `all_datasets.json` |
| `datasets.csv` | One row per dataset: id, page, Greek title and description, publisher, theme, declared frequency, catalog dates, formats, download links, keywords |

Refresh both with one request plus the flattening script:

```sh
curl -sSL -o all_datasets.json https://www.data.gov.cy/data.json
python3 -I scripts/data-catalog/flatten.py all_datasets.json docs/data-catalog/datasets.csv
gzip -9 -c all_datasets.json > docs/data-catalog/all_datasets.json.gz
```

`https://www.data.gov.cy/data.json` redirects to `https://data.gov.cy/sites/default/files/serialised_datasets/dcat_ap_2_0_json/all_datasets.json`. The portal is Drupal/DKAN; the CKAN (`/api/3/action/...`) and DKAN metastore APIs return 404. A single dataset's DCAT JSON is `/el/dataset/{node id}/dcat-ap-2.0/json` (used by pharmacies-service); a dataset page `/el/dataset/i/{uuid}` lists its file links under `/sites/default/files/`.

## What the catalog holds

- 1006 of 1957 datasets are CYSTAT tables (publisher `Στατιστική Υπηρεσία`); their data is served by the CYSTAT PxWeb API that statistics-service already uses.
- The other 951 come from about 40 departments. Largest: Land Registry and Surveys 157 (mostly map layers), Environment 76, Nicosia Municipality 47, Interior 46, Police 43, Labour Inspection 37, Forests 35.
- **Catalog dates are not data freshness.** Hundreds of datasets carry `modified` 2024-01-16 and their files carry `Last-Modified` 23 Oct 2023 from the portal migration. Check the newest file and its last row before relying on a dataset.

## Freshness of checked non-CYSTAT datasets

Checked on 6 November 2026 by the newest file on the dataset page (`Last-Modified`) and its last data row.

| Dataset (id prefix) | Publisher | Newest data | Notes |
| --- | --- | --- | --- |
| Duty pharmacies (`f2d5af02`) | Pharmaceutical Services | roster to 30/04/2027 | used by pharmacies-service |
| Property sales/transfers by district, monthly (`60b3041d`) | Land Registry | 2026 file, 06 Oct 2026 | also sales to foreigners (`a3705724`), mortgages (`26c81c93`), contracts of sale (`efd87688`); quarterly updates |
| Forest fires list (`eac58c16`) | Forests | last fire 05/12/2025 | date, coordinates, area (ha), cause |
| Hourly air pollutants, history (`0f659439`) | Labour Inspection | to 31/12/2025 | yearly CSV per year |
| Daily max/min temperature and rain (`03b62ff1`) | Meteorology | to 31/12/2025 | |
| Bathing water sampling points (`7ef40ef0`) | Environment | 2025 file, 25 Nov 2025 | EU Bathing Water Directive |
| Private pharmacies list (`65e90161`) | Pharmaceutical Services | 2024–2025 list | |
| Green Dot recycling points (`b9e4d56b`) | Green Dot | June 2024 | |
| Paphos airport daily traffic (`75679133`) | Civil Aviation | 2023 | |
| Monthly airport arrivals (`2deb4436`) | Finance | April 2024 | |
| EV charging stations (`00f53316`) | EAC | Oct 2023 | EAC stations only |
| Road accidents per year (`51488756`) | Police | 2022 | |
| Monthly dam inflows (`ab3f5b76`), dam storage (`7c696d72`) | Water Development | 2022–23 / Aug 2022 | |
| Daily rainfall final (`9413b53f`) | Meteorology | 2018 | |
| Unemployment benefit claimants (`8bf55212`, `c13c8471`) | Social Insurance | 2018 / 2017 | |
| Larnaca airport daily traffic (`5d2050fa`) | Civil Aviation | 2017 | |
| Road traffic census (`777b0aa0`, `1c4f2e06`) | Public Works | 2015 | |

## Live endpoints listed in the catalog (not yet verified)

These hosts were blocked by the research session's network, so availability and format are unconfirmed.

| Data | Endpoint |
| --- | --- |
| Retail fuel prices per station | `https://eforms.eservices.cyprus.gov.cy/MCIT/MCIT/PetroleumPrices` |
| Air quality, current | `https://www.airquality.dli.mlsi.gov.cy/all_stations_data_PM` |
| Weather stations, current | `https://dom.org.cy/AWS/OpenData/CyDoM.xml` |
| Point forecasts (Cyprus Institute) | `http://weather.cyi.ac.cy/data/txt/DSM_{ATHALASSA,KARVOUNAS,LARNAKA,LEMESOS,PAPHOS}.txt` |
| Recent earthquakes | `http://www.gsd-seismology.org.cy/events/indirect.kml` |
| Buses (Motion Bus Card) | GTFS `https://www.motionbuscard.org.cy/opendata`, SIRI `http://213.207.141.26:8013/SiriWS.asmx` |

## Existing services (checked with a web search, mostly English results)

| Topic | Existing | Assessment |
| --- | --- | --- |
| Fuel prices by station | CY Fuel, Fuel In Cyprus, fuelCY, Fuel Finder CY, Source CY | covered |
| Air quality | official airquality.dli.mlsi.gov.cy | covered |
| Earthquakes | EMSC (uses GSD data), EarthquakeToday | covered |
| Medicine prices | Ministry of Health pricing platform | covered |
| Weather, buses | Department of Meteorology, Motion app, Google Maps (GTFS) | covered |
| Forest fire history map | studies, Statista, one-off news maps | no interactive map found |
| Property market by district | DLS monthly tables, monthly news articles | no dashboard found |
| Beaches: water quality and Blue Flag together | separate official Blue Flag map and EU bathing water data | no combined map found |

Source CY also covers water levels, transport and EV chargers. Greek-language apps were not searched separately; repeat the search in Greek before starting one of the gaps.

## CYSTAT tables not yet charted

House price index (quarterly, 2025=100); motor vehicle registrations (monthly, by type); building permits (monthly, by district); births and divorces; tourist spending per capita and per day; GDP; retail trade turnover index.
