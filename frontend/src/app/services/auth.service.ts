import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {LoginResponse} from '../models/loginResponse.model';


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1';

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.apiUrl + '/auth/login', { email, password });
  }

  saveToken(token: string) {
    localStorage.setItem('token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isUserLogged(): boolean {
    return !!this.getToken();
  }

  logout(): Observable<any> {
    const token = localStorage.getItem('token');
    if (token) {
      return this.http.post<any>(`${this.apiUrl}/auth/logout`, {}, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
    }
    return new Observable();  // Devuelve un observable vacío si no hay token
  }

  register(userData: FormData): Observable<any> {
    return this.http.post<any>(this.apiUrl + '/auth/registration', userData);
  }

  getUserRole(): Observable<string> {
    return this.http.get<string>(this.apiUrl + '/auth/user-role', {
      headers: {
        'Authorization': `Bearer ${this.getToken()}`
      }
    });
  }

}
