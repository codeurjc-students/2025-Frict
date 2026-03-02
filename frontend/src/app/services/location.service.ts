import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {Address} from '../models/address.model';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService {

  constructor(private http: HttpClient) {
  }

  // REVERSE GEOCODING
  getAddressFromCoordinates(lat: number, lng: number): Observable<Partial<Address> | null> {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`;

    return this.http.get<any>(url).pipe(
      map(data => {
        if (!data || !data.address) return null;

        const addr = data.address;

        const mappedAddress: Partial<Address> = {
          street: addr.road || addr.pedestrian || addr.street || '',
          number: addr.house_number || '',
          city: addr.city || addr.town || addr.village || '',
          postalCode: addr.postcode || '',
          country: addr.country || ''
        };

        return mappedAddress;
      })
    );
  }

  // DIRECT GEOCODING
  getCoordinatesFromAddress(address: Partial<Address>): Observable<Coordinates | null> {
    const url = 'https://nominatim.openstreetmap.org/search';

    const queryParts = [
      `${address.street || ''} ${address.number || ''}`.trim(),
      address.city,
      address.postalCode,
      address.country
    ].filter(part => !!part);

    const fullQuery = queryParts.join(', ');

    const params = new HttpParams()
      .set('q', fullQuery)
      .set('format', 'json')
      .set('limit', '1');

    return this.http.get<any[]>(url, { params }).pipe(
      map(results => {
        if (results && results.length > 0) {
          const firstResult = results[0];
          return {
            latitude: parseFloat(firstResult.lat),
            longitude: parseFloat(firstResult.lon)
          };
        }
        return null;
      })
    );
  }

}
