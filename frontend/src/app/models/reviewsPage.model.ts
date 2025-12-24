import {Review} from './review.model';

export interface ReviewsPage {
  reviews: Review[];
  totalReviews: number;
  currentPage: number;
  lastPage: number;
  pageSize: number;
}
