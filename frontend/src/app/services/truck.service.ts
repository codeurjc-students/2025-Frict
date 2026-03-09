import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PageResponse} from '../models/pageResponse.model';
import {ShopStock} from '../models/shopStock.model';
import {Truck} from '../models/truck.model';
import {Product} from '../models/product.model';
import {Shop} from '../models/shop.model';


@Injectable({
  providedIn: 'root'
})
export class TruckService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/trucks';

  public getAllTrucksPage(page: number, size: number): Observable<PageResponse<Truck>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Truck>>(this.apiUrl + `/`, { params });
  }

  public getUnassignedTrucks(): Observable<Truck[]> {
    return this.http.get<Truck[]>(this.apiUrl + `/available/`);
  }

  public getTruckById(id: string): Observable<Truck> {
    return this.http.get<Truck>(this.apiUrl + `/${id}`);
  }

  public getAllShopTrucks(shopId: string): Observable<Truck[]> {
    return this.http.get<Truck[]>(this.apiUrl + `/shop/${shopId}/list`);
  }

  public getTrucksPageByShopId(shopId: string, page: number, size: number): Observable<PageResponse<Truck>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Truck>>(this.apiUrl + `/shop/${shopId}`, { params });
  }

  public createTruck(truckData: Truck): Observable<Truck> {
    return this.http.post<Truck>(this.apiUrl, truckData);
  }

  public updateTruck(id: string, truckData: Truck): Observable<Truck> {
    return this.http.put<Truck>(this.apiUrl + `/${id}`, truckData);
  }

  public deleteTruck(id: string): Observable<Truck> {
    return this.http.delete<Truck>(this.apiUrl + `/${id}`);
  }

  public assignDriver(driverId: string, truckId: string, state: boolean): Observable<Truck>{
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Truck>(this.apiUrl + `/${truckId}/assign/driver/${driverId}`, null, { params });
  }
}
