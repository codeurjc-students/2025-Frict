export interface RouteResult {
  durationSeconds: number;
  distanceMeters: number;
  coordinates: [number, number][]; // [lng, lat] pairs from OSRM
}
