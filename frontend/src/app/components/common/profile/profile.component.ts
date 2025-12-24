import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';

import {ButtonModule} from 'primeng/button';
import {TagModule} from 'primeng/tag';
import {AvatarModule} from 'primeng/avatar';
import {RatingModule} from 'primeng/rating';
import {FormsModule} from '@angular/forms';
import {NavbarComponent} from '../navbar/navbar.component';
import {FooterComponent} from '../footer/footer.component';
import {User} from '../../../models/user.model';
import {UserService} from '../../../services/user.service';
import {OrderService} from '../../../services/order.service';
import {ReviewService} from '../../../services/review.service';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {OrdersPage} from '../../../models/ordersPage.model';
import {ReviewsPage} from '../../../models/reviewsPage.model';
import {Paginator, PaginatorState} from 'primeng/paginator';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    TagModule,
    AvatarModule,
    RatingModule,
    NavbarComponent,
    FooterComponent,
    LoadingScreenComponent,
    Paginator
  ],
  templateUrl: './profile.component.html',
  styles: []
})
export class ProfileComponent implements OnInit {

  user!: User;

  foundOrders : OrdersPage = {orders: [], totalOrders: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstOrder: number = 0;
  ordersRows: number = 5;

  foundReviews: ReviewsPage = {reviews: [], totalReviews: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstReview: number = 0;
  reviewsRows: number = 5;

  loading: boolean = true;
  error: boolean = false;

  constructor(private userService: UserService,
              private orderService: OrderService,
              private reviewService: ReviewService) {}

  ngOnInit() {
    this.loadUser();
  }

  protected loadUser(){
    this.userService.getLoggedUserInfo().subscribe({
      next: (user) => {
        this.user = user;
        this.loadUserOrders();
        this.loadUserReviews();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  onUserOrdersPageChange(event: PaginatorState) {
    this.firstOrder = event.first ?? 0;
    this.ordersRows = event.rows ?? 10;
    this.loadUserOrders();
  }

  onUserReviewsPageChange(event: PaginatorState) {
    this.firstReview = event.first ?? 0;
    this.reviewsRows = event.rows ?? 10;
    this.loadUserReviews();
  }

  protected loadUserOrders(){
    this.orderService.getLoggedUserOrders(this.firstOrder/this.ordersRows, this.ordersRows).subscribe({
      next: (orders) => {
        this.foundOrders = orders;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected loadUserReviews(){
    this.reviewService.getLoggedUserReviews(this.firstReview/this.reviewsRows, this.reviewsRows).subscribe({
      next: (reviews) => {
        this.foundReviews = reviews;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected getStatusSeverity(status: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined {
    switch (status) {
      case 'delivered': return 'success';
      case 'shipped': return 'info';
      case 'processing': return 'warn';
      case 'cancelled': return 'danger';
      default: return 'secondary';
    }
  }

  protected getRoleLabel(status: string): string {
    const labels: Record<string, string> = {
      USER: 'Usuario registrado',
      MANAGER: 'Gerente',
      DRIVER: 'Repartidor',
      ADMIN: 'Administrador'
    };
    return labels[status] || status;
  }
}
