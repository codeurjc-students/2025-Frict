import {Component, OnInit} from '@angular/core';
import {Product} from '../../../models/product.model';
import {ProductService} from '../../../services/product.service';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {NgForOf, NgIf} from '@angular/common';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';

@Component({
  selector: 'app-client-home',
  imports: [
    NgForOf,
    NgIf,
    FontAwesomeModule,
    NavbarComponent
  ],
  templateUrl: './client-home.component.html',
  styleUrl: './client-home.component.css'
})
export class ClientHomeComponent implements OnInit {

  products: Product[] = [];
  availableProducts: boolean = false;
  loginInfo: LoginInfo;

  constructor(private productService: ProductService,
              private authService: AuthService) {
    this.loginInfo = this.authService.getDefaultLoginInfo();
  }

  ngOnInit() {
    this.authService.getLoginInfo().subscribe(info => {console.log(info); this.loginInfo = info;});
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        this.products = Array.isArray(data) ? data : data.products;
        this.availableProducts = this.products.length > 0;
      },
      error: (err) => {
        console.error('Error en la llamada:', err);
      }
    });
  }

}
