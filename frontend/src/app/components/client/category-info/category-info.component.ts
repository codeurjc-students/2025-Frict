import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, RouterModule} from '@angular/router'; // Importar Router
import {ButtonModule} from 'primeng/button';
import {TagModule} from 'primeng/tag';
import {RatingModule} from 'primeng/rating';
import {FormsModule} from '@angular/forms';
import {ProductCardComponent} from '../product-card/product-card.component';
import {Product} from '../../../models/product.model';
import {CategoryService} from '../../../services/category.service';
import {ProductService} from '../../../services/product.service';
import {Category} from '../../../models/category.model';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';

@Component({
  selector: 'app-category-info',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    TagModule,
    RatingModule,
    FormsModule,
    ProductCardComponent,
    BreadcrumbReloadComponent,
    LoadingScreenComponent
  ],
  templateUrl: './category-info.component.html',
  styleUrl: './category-info.component.css'
})
export class CategoryInfoComponent implements OnInit {

  private categoryService = inject(CategoryService);
  private productService = inject(ProductService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private breadcrumbService = inject(BreadcrumbService);

  topSalesProducts: Product[] = []; //Top sales products of this main category
  mainCategory!: Category;
  similarCategories: Category[] = []; //Main category siblings

  loading: boolean = true;
  error: boolean = false;

  useCases = [
    { title: 'Seguridad para tu Negocio', icon: 'pi pi-briefcase' },
    { title: 'Vigila a tu Mascota', icon: 'pi pi-heart' },
    { title: 'Monitor de Bebés', icon: 'pi pi-users' },
    { title: 'Segunda Residencia', icon: 'pi pi-home' }
  ];

  ngOnInit() {
    this.route.params.subscribe(() => { //If a related product is clicked when visualizing a product, the page should refresh the information
      this.loadMainCategory();
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  loadMainCategory() {
    this.loading = true;
    this.error = false;

    const id = this.route.snapshot.paramMap.get('id');
    // Capturamos el state de navegación al inicio
    const navState = history.state;

    if (id) {
      this.categoryService.getCategoryById(id).subscribe({
        next: (category) => {
          this.mainCategory = category;

          const currentUrl = this.router.url;

          this.breadcrumbService.setNodesForUrl(currentUrl, [{ label: category.name }]);

          if (navState.from === 'categories-management') {
            // From Categories Manager: Inicio > Categories Manager > Category Name
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, [
              { label: 'Gestor de Categorías', routerLink: '/admin/categories' }
            ]);
          } else {
            // From Home or direct access: Home > Category Name
            this.breadcrumbService.insertPenultimateNodesForUrl(currentUrl, []);
          }

          this.loadSimilarCategories();
          this.loadTopSalesProducts();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      })
    }
  }

  loadSimilarCategories(){
    if(this.mainCategory.parentId){
      this.categoryService.getCategoryById(this.mainCategory.parentId).subscribe({
        next: (category) => {
          this.similarCategories = category.children.filter(c => c.id !== this.mainCategory.id);
          this.loading = false;
        }
      })
    }
    else{
      this.categoryService.getAllCategories().subscribe({
        next: (c) => {
          this.similarCategories = c.filter(c => c.id !== this.mainCategory.id);
          this.loading = false;
        }
      })
    }
  }

  loadTopSalesProducts(){
    this.productService.getProductsByCategoryName(this.mainCategory.name).subscribe({
      next: (page) => {
        this.topSalesProducts = page.items;
        this.loading = false;
      }
    })
  }
}
