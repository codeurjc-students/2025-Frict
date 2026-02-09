import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {PageResponse} from '../models/pageResponse.model';
import {Shop} from '../models/shop.model';
import {Product} from '../models/product.model';
import {User} from '../models/user.model';
import {ShopStock} from '../models/shopStock.model';
import {ListResponse} from '../models/listResponse.model';

@Injectable({
  providedIn: 'root'
})
export class ShopService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/shops';


  public getShopById(id: string): Observable<Shop> {
    return this.http.get<Shop>(this.apiUrl + `/${id}`);
  }

  //Managers: retrieve only shops that have the logged user assigned
  public getAssignedShopsPage(page: number, size: number): Observable<PageResponse<Shop>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Shop>>(this.apiUrl, { params });
  }

  //Admin: Retrieve all organization shops
  public getAllShopsPage(page: number, size: number): Observable<PageResponse<Shop>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Shop>>(this.apiUrl + `/`, { params });
  }

  public getStocksPageByShopId(id: string, page: number, size: number): Observable<PageResponse<ShopStock>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<ShopStock>>(this.apiUrl + `/stock/${id}`, { params });
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

  public toggleLocalActivation(id: string, state: boolean): Observable<Product> {
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Product>(this.apiUrl + `/active/${id}`, null, { params });
  }

  public toggleAllLocalActivations(shopId: string, state: boolean): Observable<Boolean> {
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Boolean>(this.apiUrl + `/${shopId}/active/`, null, { params });
  }

  public restockProduct(stockId: string, units: number): Observable<ShopStock>{
    let params = new HttpParams();
    params = params.append('units', units);
    return this.http.post<ShopStock>(this.apiUrl + `/restock/${stockId}`, null, { params });
  }

  //stockId will act as an identifier for a product and for a stock (when assigning it will be a product id, whereas when unassigning it will be a stock id)
  public assignStock(shopId: string, stockId: string, state: boolean): Observable<Shop>{
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Shop>(this.apiUrl + `/${shopId}/assign/stock/${stockId}`, null, { params });
  }

  public assignTruck(shopId: string, truckId: string, state: boolean): Observable<Shop>{
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Shop>(this.apiUrl + `/${shopId}/assign/truck/${truckId}`, null, { params });
  }

  public assignManager(shopId: string, userId: string, state: boolean): Observable<Shop>{
    let params = new HttpParams();
    params = params.append('state', state);
    return this.http.post<Shop>(this.apiUrl + `/${shopId}/assign/manager/${userId}`, null, { params });
  }
}
