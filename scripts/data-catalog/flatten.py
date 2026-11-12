"""Flattens the data.gov.cy DCAT-AP 2.0 catalog export into one CSV row per dataset.

Usage: python3 -I scripts/data-catalog/flatten.py <all_datasets.json> <datasets.csv>
The export is https://data.gov.cy/sites/default/files/serialised_datasets/dcat_ap_2_0_json/all_datasets.json
(https://www.data.gov.cy/data.json redirects there). Prints summary counts after writing the CSV.
"""
import collections
import csv
import json
import re
import sys

src, out = sys.argv[1], sys.argv[2]
catalog = json.load(open(src, encoding="utf-8"))


def as_list(value):
    if value is None:
        return []
    return value if isinstance(value, list) else [value]


def text(value):
    if isinstance(value, dict):
        return value.get("#") or value.get("rdf:value") or value.get("@rdf:resource") or ""
    return value or ""


def resource(value):
    return value.get("@rdf:resource", "") if isinstance(value, dict) else value or ""


def key(url):
    """Resource links appear both as www.data.gov.cy/index.php/... and data.gov.cy/...; compare them by path."""
    return re.sub(r"^https?://(www\.)?data\.gov\.cy/(index\.php/)?", "", url or "")


distributions = {}
for item in as_list(catalog.get("dcat:Distribution")):
    entry = {
        "format": ((item.get("dct:format") or {}).get("dct:MediaTypeOrExtent") or {}).get("rdf:value", "").lower(),
        "download": resource(item.get("dcat:downloadURL")) or resource(item.get("dcat:accessURL")),
    }
    distributions[key(resource(item.get("dcat:accessURL")))] = entry
    page = ((item.get("foaf:page") or {}).get("foaf:Document") or {}).get("rdf:value")
    if page:
        distributions.setdefault(key(page), entry)

rows = []
for item in as_list(catalog["dcat:Catalog"]["dcat:dataset"]):
    dataset = item["dcat:Dataset"]
    publishers = [p.get("foaf:Agent", {}).get("foaf:name", "").strip() for p in as_list(dataset.get("dct:publisher"))]
    themes = []
    for theme in as_list(dataset.get("dcat:theme")):
        concept = theme.get("skos:Concept", {}) if isinstance(theme, dict) else {}
        themes.append(str(concept.get("skos:prefLabel") or concept.get("rdf:value") or "").rsplit("/", 1)[-1])
    frequency = (dataset.get("dct:accrualPeriodicity") or {}).get("dct:Frequency") or {}
    frequency = frequency.get("rdf:value") or frequency.get("skos:prefLabel") or ""
    files = [distributions.get(key(resource(d)), {}) for d in as_list(dataset.get("dcat:Distribution"))]
    rows.append({
        "id": dataset.get("dct:identifier"),
        "url": dataset.get("@rdf:about"),
        "title": dataset.get("dct:title"),
        "description": (dataset.get("dct:description") or "")[:300].replace("\n", " "),
        "publisher": "; ".join(publishers),
        "theme": "; ".join(themes),
        "frequency": str(frequency).rsplit("/", 1)[-1],
        "modified": text(dataset.get("dct:modified"))[:10],
        "issued": text(dataset.get("dct:issued"))[:10],
        "formats": ",".join(sorted({f.get("format") or "?" for f in files})),
        "downloads": " ".join(f.get("download", "") for f in files if f.get("download")),
        "keywords": "; ".join(k if isinstance(k, str) else text(k) for k in as_list(dataset.get("dcat:keyword"))),
    })

with open(out, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)

count = collections.Counter
print("datasets", len(rows))
for title, counter in (
    ("publishers", count(r["publisher"] for r in rows)),
    ("themes", count(t for r in rows for t in r["theme"].split("; "))),
    ("frequency", count(r["frequency"] for r in rows)),
    ("formats", count(f for r in rows for f in r["formats"].split(","))),
):
    print("\n" + title)
    for name, n in counter.most_common(15):
        print(f"{n:5} {name}")
