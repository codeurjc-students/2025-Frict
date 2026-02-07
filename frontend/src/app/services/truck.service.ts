import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PageResponse} from '../models/pageResponse.model';
import {ShopStock} from '../models/shopStock.model';
import {Truck} from '../models/truck.model';


@Injectable({
  providedIn: 'root'
})
export class TruckService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/trucks';

  public getTrucksPageByShopId(shopId: string, page: number, size: number): Observable<PageResponse<Truck>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Truck>>(this.apiUrl + `/shop/${shopId}`, { params });
  }
}
