
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MetricDTO {
  label: string;
  value: string | number;
}

@Injectable({
  providedIn: 'root'
})
export class MetricService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/metrics';

  getDashboardStats(): Observable<MetricDTO[]> {
    return this.http.get<MetricDTO[]>(`${this.apiUrl}/stats`);
  }
}
