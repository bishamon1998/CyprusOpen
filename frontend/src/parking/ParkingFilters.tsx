import { LocateFixed, Search } from "lucide-react";
import { areas } from "../geo/cyprusAreas";
import { useI18n } from "../i18n";
export function ParkingFilters({
  areaId,
  search,
  locating,
  onSearch,
  onArea,
  onNearMe,
}: {
  areaId: string;
  search: string;
  locating: boolean;
  onSearch: (value: string) => void;
  onArea: (value: string) => void;
  onNearMe: () => void;
}) {
  const { t } = useI18n();
  return (
    <div className="parking-filters">
      <label className="parking-search">
        <Search size={17} aria-hidden="true" />
        <input
          type="search"
          placeholder={t("search")}
          aria-label={t("search")}
          value={search}
          onChange={(event) => onSearch(event.target.value)}
        />
      </label>
      <select
        aria-label={t("area")}
        value={areaId}
        onChange={(event) => onArea(event.target.value)}
      >
        {areas.map((item) => (
          <option key={item.id} value={item.id}>
            {t(item.name)}
          </option>
        ))}
      </select>
      <button className="near-me" onClick={onNearMe} disabled={locating}>
        <LocateFixed size={17} aria-hidden="true" />
        {t(locating ? "locating" : "nearMe")}
      </button>
    </div>
  );
}
