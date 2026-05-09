import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';


export interface RegistryFilterParams {
  startDate: string;
  endDate: string;
  viewType: 'GRAPH' | 'TABLE';
  interval?: 'day' | 'week' | 'month' | 'year' | string;
  metricMode?: 'VALUE' | 'TOTAL';
  entityType?: string;
  dataType?: string;
  shopIds?: string[];
  userIds?: string[];
  productIds?: string[];
  orderIds?: string[];
}

//Cross reference
export interface CrossReferencesMap {
  shopId: string[];
  productId: string[];
  userId: string[];
  orderId: string[];
}

@Injectable({
  providedIn: 'root'
})
export class RegistryService {

  private http = inject(HttpClient);
  private apiUrl = '/api/v1/registry';

  loadPublicRegistry(params: any): Observable<any[]> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && !(Array.isArray(value) && value.length === 0)) {
        if (Array.isArray(value)) {
          value.forEach(v => {
            httpParams = httpParams.append(key, v as string);
          });
        } else {
          httpParams = httpParams.set(key, value as string | number | boolean);
        }
      }
    });

    return this.http.get<any[]>(`${this.apiUrl}/public/views`, { params: httpParams });
  }

  //Aggregated data for the chart, or detailed registries for the table
  loadInternalRegistry(params: RegistryFilterParams): Observable<any[]> {
    let httpParams = new HttpParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && !(Array.isArray(value) && value.length === 0)) {
        if (Array.isArray(value)) {
          value.forEach(v => {
            httpParams = httpParams.append(key, v);
          });
        } else {
          httpParams = httpParams.set(key, value);
        }
      }
    });

    return this.http.get<any[]>(`${this.apiUrl}/private/stats`, { params: httpParams });
  }


  //Get the main entities list that contains data
  getEntityTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/private/entities`);
  }


  //Get the available metrics for an specific entity
  getMetrics(entityType: string): Observable<string[]> {
    const params = new HttpParams().set('entityType', entityType);
    return this.http.get<string[]>(`${this.apiUrl}/private/metrics`, { params });
  }

  //Get all IDs from associated entities given an entity and a metric
  getCrossReferences(entityType: string, dataType: string): Observable<CrossReferencesMap> {
    const params = new HttpParams()
      .set('entityType', entityType)
      .set('dataType', dataType);

    return this.http.get<CrossReferencesMap>(`${this.apiUrl}/private/references`, { params });
  }

  exportCustomPdf(payload: any) {
    return this.http.post(`${this.apiUrl}/private/export/pdf`, payload, {
      responseType: 'blob'
    });
  }
}
