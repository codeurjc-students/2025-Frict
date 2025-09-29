import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {LoginResponse} from '../models/loginResponse.model';


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) { }

  private apiUrl = '/api';

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.apiUrl + '/auth/login', { email, password });
  }

  saveToken(token: string) {
    localStorage.setItem('token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
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

  register(name: string, email: string, password: string, comunidadAutonoma: string, hospitalRef: string, consentFirm: boolean): Observable<any> {
    const body = { name, email, password, comunidadAutonoma, hospitalRef, consentFirm };
    return this.http.post<any>(this.apiUrl + '/auth/register', body);
  }

  getUserRole(): Observable<string> {
    return this.http.get<string>(this.apiUrl + '/auth/user-role', {
      headers: {
        'Authorization': `Bearer ${this.getToken()}`
      }
    });
  }

}
