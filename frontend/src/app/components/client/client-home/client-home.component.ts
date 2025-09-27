import {Component, OnInit} from '@angular/core';
import {Product} from '../../../models/product.model';
import {ProductService} from '../../../services/product.service';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-client-home',
  imports: [
    NgForOf,
    NgIf
  ],
  templateUrl: './client-home.component.html',
  styleUrl: './client-home.component.css'
})
export class ClientHomeComponent implements OnInit {
  products: Product[] = [];
  availableProducts: boolean = false;

  constructor(private productService: ProductService) { }

  ngOnInit() {
    this.productService.getAllProducts().subscribe({
      next: (data) => {
        console.log('Respuesta del backend:', data);
        this.products = Array.isArray(data) ? data : data.products;
        this.availableProducts = this.products.length > 0;
      },
      error: (err) => {
        console.error('Error en la llamada:', err);
      }
    });
  }
}
