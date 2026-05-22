import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Review, ReviewStats} from '../models/review.model';
import {PageResponse} from '../models/pageResponse.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {

  constructor(private http: HttpClient) {
  }

  private apiUrl = '/api/v1/reviews';

  public getUserReviewsByUserId (id: string, page: number, size: number): Observable<PageResponse<Review>>{
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Review>>(this.apiUrl + `/user/${id}`, { params });
  }

  public getLoggedUserReviews(page: number, size: number): Observable<PageResponse<Review>>{
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    return this.http.get<PageResponse<Review>>(this.apiUrl, { params });
  }

  public getReviewsByProductId(id: string): Observable<Review[]> {
    let params = new HttpParams();
    params = params.append('productId', id);
    return this.http.get<Review[]>(this.apiUrl + `/`, { params });
  }

  public getProductReviews(productId: string, page: number, size: number, sort: string): Observable<PageResponse<Review>> {
    const params = new HttpParams()
      .set('productId', productId)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<PageResponse<Review>>(this.apiUrl + `/product`, { params });
  }

  public getProductReviewStats(productId: string): Observable<ReviewStats> {
    const params = new HttpParams().set('productId', productId);
    return this.http.get<ReviewStats>(this.apiUrl + `/product/stats`, { params });
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
