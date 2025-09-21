import {Component, OnInit} from '@angular/core';
import {Product} from '../../../models/product.model';
import {Router} from '@angular/router';
import {ProductService} from '../../../services/product.service';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-client-home',
  imports: [
    NgForOf
  ],
  templateUrl: './client-home.component.html',
  styleUrl: './client-home.component.css'
})
export class ClientHomeComponent implements OnInit {
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
