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

  private apiUrl = '/api/v1';

  public uploadUserImage(selectedImage: File) {
    const formData = new FormData();
    formData.append('image', selectedImage);
    return this.http.post(this.apiUrl + `/users/image`, formData);
  }

  public getLoggedUserInfo(): Observable<User>{
    return this.http.get<User>(this.apiUrl + `/users/me`);
  }

  public submitPaymentCard(card: PaymentCard): Observable<User>{
    if (card.id){
      return this.http.put<User>(this.apiUrl + `/users/cards`, card);
    }
      return this.http.post<User>(this.apiUrl + `/users/cards`, card);
  }

  public submitAddress(address: Address): Observable<User>{
    if (address.id){
      return this.http.put<User>(this.apiUrl + `/users/addresses`, address);
    }
    return this.http.post<User>(this.apiUrl + `/users/addresses`, address);
  }
}
