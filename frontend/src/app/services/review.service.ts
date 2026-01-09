import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {Review} from '../models/review.model';
import {PageResponse} from '../models/pageResponse.model';
import {ListResponse} from '../models/listResponse.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/reviews';

  public getLoggedUserReviews(page: number, size: number): Observable<PageResponse<Review>>{
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Review>>(this.apiUrl, { params });
  }

  public getReviewsByProductId(id: string): Observable<Review[]> {
    let params = new HttpParams();
    params = params.append('productId', id);
    return this.http.get<ListResponse<Review>>(this.apiUrl + `/`, { params }).pipe(map(response => response.items));
  }

  //Creates and edits reviews
  public submitReview(review: Partial<Review>): Observable<Review> {
    if (review.id){
      return this.http.put<Review>(this.apiUrl, review); //Edit a review
    }
    return this.http.post<Review>(this.apiUrl, review); //Create a review
  }

  public deleteReviewById(id: string): Observable<Review> {
    return this.http.delete<Review>(this.apiUrl + `/${id}`);
  }
}
