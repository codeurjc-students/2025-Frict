import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {User} from '../models/user.model';
import {PaymentCard} from '../models/paymentCard.model';
import {Address} from '../models/address.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1/users';

  public uploadUserImage(userId: string, selectedImage: File) {
    const formData = new FormData();
    formData.append('image', selectedImage);
    return this.http.post(this.apiUrl + `/image/${userId}`, formData);
  }

  public getLoggedUserInfo(): Observable<User>{
    return this.http.get<User>(this.apiUrl + `/me`);
  }

  public deleteLoggedUser(): Observable<User> {
    return this.http.delete<User>(this.apiUrl);
  }

  //POST method could be used by administrators to create new user profiles
  public submitUserData(user: User): Observable<User>{
    if (user.id){
      return this.http.put<User>(this.apiUrl + `/data`, user);
    }
    return this.http.post<User>(this.apiUrl + `/data`, user); //Unused
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
}
