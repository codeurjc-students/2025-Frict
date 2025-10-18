import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {LoginInfo} from '../models/loginInfo.model';
import {catchError, map, Observable, of} from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1';

  public login(user: string, pass: string) {
    return this.http.post(
      this.apiUrl + "/auth/login",
      { username: user, password: pass },
      { withCredentials: true }
    )}

  public getLoginInfo(): Observable<LoginInfo> {
    return this.http.get<LoginInfo>(this.apiUrl + "/users/me", { withCredentials: true }).pipe(
      map(info => ({...info, isLogged: true})),
      catchError(() => {
        return of({isLogged: false, id: 0, name: '', username: '', admin: false} as LoginInfo);
      })
    );
  }

  public register(userData: FormData) {
    return this.http.post<any>(this.apiUrl + '/auth/registration', userData);
  }

  public logout() {
    this.http.post(this.apiUrl + "/logout", { withCredentials: true });
  }

  getDefaultLoginInfo() {
    return {isLogged: false, id: 0, name: '', username: '', admin: false} as LoginInfo;
  }
}
