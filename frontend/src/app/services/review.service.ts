import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ReviewList} from '../models/reviewList.model';
import {Review} from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/reviews';

  public getReviewsByProductId(id: string): Observable<ReviewList> {
    let params = new HttpParams();
    params = params.append('productId', id);
    return this.http.get<ReviewList>(this.apiUrl + `/`, { params });
  }

  public createReview(review: Partial<Review>): Observable<Review> {
    return this.http.post<Review>(this.apiUrl, review);
  }
}
