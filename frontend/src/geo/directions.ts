export function googleDirections(destination: string) {
  return "https://www.google.com/maps/dir/?" +
    new URLSearchParams({ api: "1", destination, travelmode: "driving" });
}
