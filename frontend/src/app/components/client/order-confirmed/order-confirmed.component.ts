import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {FooterComponent} from '../../common/footer/footer.component';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {UserService} from '../../../services/user.service';
import {User} from '../../../models/user.model';

@Component({
  selector: 'app-order-confirmed',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    NavbarComponent,
    FooterComponent,
    LoadingScreenComponent
  ],
  templateUrl: './order-confirmed.component.html',
  styleUrl: './order-confirmed.component.css'
})
export class OrderConfirmedComponent implements OnInit {

  orderId: string = '';
  orderRefCode: string = '';

  user!: User;

  loading: boolean = true;
  error: boolean = false;

  constructor(private route: ActivatedRoute,
              private userService: UserService) {}

  ngOnInit(): void {
    // Intentamos obtener la referencia de la URL (ej: /order-confirmed/ORD-123)
    // O desde los queryParams
    this.route.queryParams.subscribe(params => {
      if(params['ref']) this.orderRefCode = params['ref'];
      if(params['id'])  this.orderId = params['id'];
    });
    this.loadLoggedUserInfo();
  }

  loadLoggedUserInfo() {
    this.userService.getLoggedUserInfo().subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = true;
      }
    })
  }
}
