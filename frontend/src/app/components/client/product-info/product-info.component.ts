import {Component, OnInit} from '@angular/core';
import {FooterComponent} from '../../common/footer/footer.component';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {GalleriaModule} from 'primeng/galleria';
import {carouselResponsiveOptions, galleryResponsiveOptions} from '../../../app.config';
import {Product} from '../../../models/product.model';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {FormsModule} from '@angular/forms';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Carousel} from 'primeng/carousel';
import {LoadingSectionComponent} from '../../common/loading-section/loading-section.component';
import {ProductCardComponent} from '../product-card/product-card.component';
import {ActivatedRoute, Router} from '@angular/router';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem, MessageService} from 'primeng/api';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/numberFormat.util';
import {CategoryService} from '../../../services/category.service';
import {Category} from '../../../models/category.model';
import {Dialog} from 'primeng/dialog';
import {Panel} from 'primeng/panel';
import {Avatar} from 'primeng/avatar';
import {Rating} from 'primeng/rating';
import {MeterGroupModule} from 'primeng/metergroup';
import {ReviewService} from '../../../services/review.service';
import {Review} from '../../../models/review.model';
import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';
import {Toast} from 'primeng/toast';
import {StyleClass} from 'primeng/styleclass';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';


@Component({
  selector: 'app-product-info',
  standalone: true,
  imports: [
    FooterComponent,
    NavbarComponent,
    GalleriaModule,
    Tabs,
    TabList,
    TabPanels,
    Tab,
    TabPanel,
    FormsModule,
    InputNumber,
    Button,
    Carousel,
    LoadingSectionComponent,
    ProductCardComponent,
    Breadcrumb,
    Dialog,
    Panel,
    Avatar,
    Rating,
    MeterGroupModule,
    Toast,
    StyleClass,
    LoadingScreenComponent
  ],
  providers: [MessageService],
  templateUrl: './product-info.component.html'
})
export class ProductInfoComponent implements OnInit {

  protected readonly galleryResponsiveOptions = galleryResponsiveOptions;
  protected readonly carouselResponsiveOptions = carouselResponsiveOptions;
  protected readonly formatPrice = formatPrice;
  protected readonly Math = Math;

  protected images: any[] = [];

  protected quantity: number = 1;

  protected loading: boolean = true;
  protected error: boolean = false;

  protected relatedLoading: boolean = true;
  protected relatedError: boolean = false;
  protected relatedProducts: Product[] = []; //Products related to products first category

  protected previousPages: any[] = [{ icon: 'pi pi-home', route: '/installation' }, { label: 'Buscar' }, { label: 'Ordenadores', route: '/inputtext' }];
  protected homePage: MenuItem | undefined;

  protected product!: Product;
  protected productCategory!: Category;

  protected visibleShippingDialog: boolean = false;

  protected stars: any[] = [];
  protected recommendedCount: number = 0;
  protected recommendationPercentage: number = 0;
  protected productReviews: Review[] = [];

  protected userReviewed: boolean = false;
  protected loggedUserInfo!: LoginInfo;

  constructor(private productService: ProductService,
              private categoryService: CategoryService,
              private reviewService: ReviewService,
              protected authService: AuthService,
              private route: ActivatedRoute,
              private router: Router,
              private messageService: MessageService) {}

  ngOnInit() {
    this.route.params.subscribe(() => { //If a related product is clicked when visualizing a product, the page should refresh the information
      this.loadProduct();
    });
  }

  protected resetError() {
    this.loading = true;
    this.error = false;
    this.loadProduct();
  }

  loadProduct(){
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (product) => {
          this.product = product;
          this.images.push({
            itemImageSrc: product.imageUrl,
            thumbnailImageSrc: product.thumbnailUrl
          });
          this.loadProductCategory();
          this.loadReviews();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    }
  }

  loadProductCategory() { //Only the first category is taken into account in related products
    this.categoryService.getCategoryById(this.product.categoriesId[0]).subscribe({
      next: (category) => {
        this.productCategory = category;
        this.loadRelatedProducts();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  loadRelatedProducts() {
    const currentProductId = this.route.snapshot.paramMap.get('id');
    this.productService.getProductsByCategoryName(this.productCategory.name).subscribe({
      next: (products) => {
        this.relatedProducts = products.products.filter(p => p.id.toString() !== currentProductId?.toString());
        this.relatedLoading = false;
      },
      error: () => {
        this.relatedLoading = false;
        this.relatedError = true;
      }
    })
  }

  showShippingDialog() {
    this.visibleShippingDialog = true;
  }


  loadReviews() {
    this.reviewService.getReviewsByProductId(this.product.id).subscribe({
      next: (reviews) => {
        this.productReviews = reviews.reviews;
        const stars = [
          { value: 5, count: 0 },
          { value: 4, count: 0 },
          { value: 3, count: 0 },
          { value: 2, count: 0 },
          { value: 1, count: 0 },
        ];

        this.productReviews.forEach((review: any) => {
          const star = stars.find(s => s.value === review.rating);
          if (star) {
            star.count += 1;
          }
        });

        this.stars = stars;

        if(this.productReviews.length > 0){
          this.recommendedCount = this.productReviews.filter(r => r.recommended).length;
          this.recommendationPercentage = (this.recommendedCount / this.productReviews.length) * 100;
        }

        this.authService.getLoginInfo().subscribe({
          next: (loginInfo) => {
            this.userReviewed = this.productReviews.some(r => r.creatorId === loginInfo.id)
            this.loggedUserInfo = loginInfo;
            this.loading = false;
          }
        })
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  addToCart() {
    if(!this.authService.isLogged()){
      this.router.navigate(['/login']);
    }
    this.productService.addProductToCart(this.product.id, this.quantity).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto añadido correctamente al carrito' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error añadiendo el producto al carrito' });
      }
    })
  }

  addToFavourites() {
    if(!this.authService.isLogged()){
      this.router.navigate(['/login']);
    }
    this.productService.addProductToFavourites(this.product.id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'info', summary: 'Añadido', detail: 'Producto añadido a tus favoritos' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error añadiendo el producto a tus favoritos' });
      }
    })
  }
}
