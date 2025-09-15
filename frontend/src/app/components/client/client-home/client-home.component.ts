import { Component } from '@angular/core';
import { ProductService } from '../../../services/product.service';
import { Router } from '@angular/router';
import { Product } from '../../../models/product.model';
import { NgForOf } from '@angular/common';

@Component({
  selector: 'app-client-home',
  imports: [
    NgForOf
  ],
  templateUrl: './client-home.component.html',
  styleUrls: ['./client-home.component.css']
})
export class ClientHomeComponent {

  products: Product[] = [];

  constructor(private router: Router, private productService: ProductService) { }

  ngOnInit() {
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        console.log('Respuesta del backend:', data);
        this.products = Array.isArray(data) ? data : data.products;
      },
      error: (err) => {
        console.error('Error en la llamada:', err);
        this.router.navigate(['/error']);
      }
    });
  }

}
