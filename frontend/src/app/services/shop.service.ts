import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {PageResponse} from '../models/pageResponse.model';
import {Shop} from '../models/shop.model';
import {Product} from '../models/product.model';
import {User} from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class ShopService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/shops';

  public getShopsPage(page: number, size: number): Observable<PageResponse<Shop>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Shop>>(this.apiUrl + `/`, { params });
  }

  public getShopById(id: string): Observable<Shop> {
    return this.http.get<Shop>(this.apiUrl + `/${id}`);
  }

  public createShop(shopData: Shop): Observable<Shop> {
    return this.http.post<Shop>(this.apiUrl, shopData);
  }

  public updateShop(id: string, shopData: Shop): Observable<Shop> {
    return this.http.put<Shop>(this.apiUrl + `/${id}`, shopData);
  }

  public deleteShop(id: string): Observable<Shop> {
    return this.http.delete<Shop>(this.apiUrl + `/${id}`);
  }

  public updateShopImage(shopId: string, selectedImage: File): Observable<Shop> {
    const formData = new FormData();
    formData.append('image', selectedImage);
    return this.http.post<Shop>(this.apiUrl + `/image/${shopId}`, formData);
  }

  public assignManager(id: string, userId: string, state: boolean): Observable<Shop>{
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Shop>(this.apiUrl + `/${id}/assign/${userId}`, null, { params });
  }
}
