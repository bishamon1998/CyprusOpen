import { useState } from "react";
import { type Position } from "../geo/coordinates";
import { type MessageKey } from "../locales";

export function useBrowserLocation() {
  const [position, setPosition] = useState<Position | null>(null);
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState<MessageKey | null>(null);

  function locate(onLocated: () => void) {
    if (!navigator.geolocation) {
      setLocationError("locationUnavailable");
      return;
    }
    setLocating(true);
    setLocationError(null);
    navigator.geolocation.getCurrentPosition(
      (result) => {
        setPosition({
          latitude: result.coords.latitude,
          longitude: result.coords.longitude,
        });
        setLocating(false);
        onLocated();
      },
      (error) => {
        setLocating(false);
        setLocationError(
          error.code === 1 ? "locationDeclined" : "locationFailed",
        );
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 },
    );
  }

  return {
    position,
    locating,
    locationError,
    locate,
    clearLocation: () => setPosition(null),
  };
}
