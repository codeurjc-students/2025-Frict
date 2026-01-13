import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {User} from '../models/user.model';
import {PaymentCard} from '../models/paymentCard.model';
import {Address} from '../models/address.model';
import {PageResponse} from '../models/pageResponse.model';
import {StatData} from '../utils/statData.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/users';

  public uploadUserImage(userId: string, selectedImage: File): Observable<User> {
    const formData = new FormData();
    formData.append('image', selectedImage);
    return this.http.post<User>(this.apiUrl + `/image/${userId}`, formData);
  }

  public getLoggedUserInfo(): Observable<User>{
    return this.http.get<User>(this.apiUrl + `/me`);
  }

  public getAllUsers(page: number, size: number): Observable<PageResponse<User>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<User>>(this.apiUrl + `/`, { params });
  }

  public anonLoggedUser(): Observable<User> {
    return this.http.delete<User>(this.apiUrl);
  }

  //POST method could be used by administrators to create new user profiles
  public submitUserData(user: User): Observable<User>{
    if (user.id){
      return this.http.put<User>(this.apiUrl + `/data`, user);
    }
    return this.http.post<User>(this.apiUrl + `/data`, user);
  }

  public submitPaymentCard(card: PaymentCard): Observable<User>{
    if (card.id){
      return this.http.put<User>(this.apiUrl + `/cards`, card);
    }
      return this.http.post<User>(this.apiUrl + `/cards`, card);
  }

  public submitAddress(address: Address): Observable<User>{
    if (address.id){
      return this.http.put<User>(this.apiUrl + `/addresses`, address);
    }
    return this.http.post<User>(this.apiUrl + `/addresses`, address);
  }

  public deleteAddress(id: string): Observable<User> {
    return this.http.delete<User>(this.apiUrl + `/addresses/${id}`);
  }

  public deletePaymentCard(id: string): Observable<User> {
    return this.http.delete<User>(this.apiUrl + `/cards/${id}`);
  }

  //User Management component endpoints
  public toggleAllBans(banState: boolean): Observable<boolean> {
    return this.http.put<boolean>(this.apiUrl + `/ban/`, banState);
  }

  public toggleUserBan(id: string, banState: boolean): Observable<User> {
    return this.http.put<User>(this.apiUrl + `/ban/${id}`, banState);
  }

  public anonAll(): Observable<boolean> {
    return this.http.put<boolean>(this.apiUrl + `/anon/`, null);
  }

  public anonUser(id: string): Observable<User> {
    return this.http.put<User>(this.apiUrl + `/anon/${id}`, null);
  }

  public deleteAll(): Observable<boolean> {
    return this.http.delete<boolean>(this.apiUrl + `/`);
  }

  public deleteUser(id: string): Observable<boolean> {
    return this.http.delete<boolean>(this.apiUrl + `/${id}`);
  }

  public getUsersStats(): Observable<StatData[]> {
    return this.http.get<StatData[]>(this.apiUrl + `/stats`)
  }

}
