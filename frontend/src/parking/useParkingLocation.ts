import { useState } from "react";
import { latLng } from "leaflet";
import { type MessageKey } from "../locales";

export type Position = { latitude: number; longitude: number };

export function distance(from: Position, to: Position) {
  return (
    latLng(from.latitude, from.longitude).distanceTo(
      latLng(to.latitude, to.longitude),
    ) / 1000
  );
}

export function useParkingLocation() {
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
