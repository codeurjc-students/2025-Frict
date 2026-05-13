import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable, of} from 'rxjs';
import {Address} from '../models/address.model';

export interface Coordinates {
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root'
})
export class LocationService {

  private apiUrl = '/api/v1/locations';

  constructor(private http: HttpClient) {
  }

  // REVERSE GEOCODING
  getAddressFromCoordinates(lat: number, lng: number): Observable<Partial<Address> | null> {
    return this.http
      .get<Partial<Address>>(`${this.apiUrl}/reverse`, {params: {lat, lng}})
      .pipe(
        map(addr => addr ?? null),
        catchError(() => of(null))
      );
  }

  // DIRECT GEOCODING
  getCoordinatesFromAddress(address: Partial<Address>): Observable<Coordinates | null> {
    const body = {
      street: address.street ?? '',
      number: address.number ?? '',
      postalCode: address.postalCode ?? '',
      city: address.city ?? '',
      country: address.country ?? ''
    };
    return this.http
      .post<Coordinates>(`${this.apiUrl}/direct`, body)
      .pipe(
        map(coords => coords ?? null),
        catchError(() => of(null))
      );
  }

}
