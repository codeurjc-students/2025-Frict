import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient) {}

  private apiUrl = '/api/v1';

  public uploadUserImage(id: string, selectedPhoto: File) {
    const formData = new FormData();
    formData.append('photo', selectedPhoto);
    return this.http.put(this.apiUrl + `/users/${id}/photo`, formData);
  }
}
