import {Component, computed, OnInit} from '@angular/core';
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
import {MessageService} from 'primeng/api';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/textFormat.util';
import {CategoryService} from '../../../services/category.service';
import {Category} from '../../../models/category.model';
import {Dialog} from 'primeng/dialog';
import {Avatar} from 'primeng/avatar';
import {Rating} from 'primeng/rating';
import {MeterGroupModule} from 'primeng/metergroup';
import {ReviewService} from '../../../services/review.service';
import {Review} from '../../../models/review.model';import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {Textarea} from 'primeng/textarea';
import {TableModule} from 'primeng/table';
import {ShopStock} from '../../../models/shopStock.model';
import {OrderService} from '../../../services/order.service';
import {HttpErrorResponse} from '@angular/common/http';
import {Image} from 'primeng/image';
import {BreadcrumbComponent} from '../../common/breadcrumb/breadcrumb.component';
import {Tag} from 'primeng/tag';
import {getStockTagInfo} from '../../../utils/tagManager.util';


@Component({
  selector: 'app-product-info',
  standalone: true,
  imports: [
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
    Dialog,
    Avatar,
    Rating,
    MeterGroupModule,
    LoadingScreenComponent,
    Textarea,
    TableModule,
    Image,
    BreadcrumbComponent,
    Tag
  ],
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

  protected product!: Product;
  protected inFavourites: boolean = false;
  protected productCategory!: Category;
  stockStatus = computed(() => getStockTagInfo(this.product.totalUnits));

  protected visibleShippingDialog: boolean = false;
  protected visibleAvailabilityDialog: boolean = false;

  protected stars: any[] = [];
  protected recommendedCount: number = 0;
  protected recommendationPercentage: number = 0;
  protected productReviews: Review[] = [];

  protected userReviewed: boolean = false;
  protected newReview: Partial<Review> = { rating: 5, recommended: true };

  protected stocks: ShopStock[] = []

  protected loggedUserInfo!: LoginInfo;

  constructor(private productService: ProductService,
              private categoryService: CategoryService,
              private reviewService: ReviewService,
              private orderService: OrderService,
              protected authService: AuthService,
              private route: ActivatedRoute,
              private router: Router,
              private messageService: MessageService) {}

  ngOnInit() {
    this.route.params.subscribe(() => { //If a related product is clicked when visualizing a product, the page should refresh the information
      this.loadProduct();
    });
  }

  //If the Partial<Review> object does not include a value in the field id, then it is a new review
  protected submitReview(){
    this.newReview.productId = this.product.id;
    this.newReview.creatorId = this.loggedUserInfo.id;
    this.reviewService.submitReview(this.newReview).subscribe({
      next: () => {
        this.userReviewed = true;
        this.loadReviews();
      }
    })
  }

  protected editReview(id: string) {
    const publishedReview = this.productReviews.find(r => r.id === id);
    if(publishedReview){
      this.newReview = { ...publishedReview }; //Deep copy, in order to be able to delete the old one from reviews array
      this.productReviews = this.productReviews.filter(r => r.id !== id);
      this.userReviewed = false;
    }
  }

  protected deleteReview(id: string) {
    this.reviewService.deleteReviewById(id).subscribe({
      next: () => {
        this.newReview = {};
        this.userReviewed = false;
        this.loadReviews();
      }
    })
  }

  protected setRecommendation(b: boolean) {
    this.newReview.recommended = b;
  }

  protected resetError() {
    this.loading = true;
    this.error = false;
    this.loadProduct();
  }

  protected loadProduct() {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.productService.getProductById(id).subscribe({
        next: (product) => {
          this.product = product;

          if (product.imagesInfo && Array.isArray(product.imagesInfo)) {
            this.images = product.imagesInfo.map((imgInfo) => ({
              itemImageSrc: imgInfo.imageUrl
            }));
          } else {
            this.images = [];
          }

          // Light scroll to the top of the page on page change
          if (typeof window !== 'undefined') {
            window.scrollTo({ top: 0, behavior: 'instant' });
          }

          this.loadProductCategory();
          this.checkInFavourites();
          this.loadShopStocks();
          this.loadReviews();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    }
  }

  protected loadProductCategory() { //Only the first category is taken into account in related products
    this.categoryService.getCategoryById(this.product.categories[0].id).subscribe({
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

  protected loadRelatedProducts() {
    const currentProductId = this.route.snapshot.paramMap.get('id');
    this.productService.getProductsByCategoryName(this.productCategory.name).subscribe({
      next: (products) => {
        this.relatedProducts = products.items.filter(p => p.id.toString() !== currentProductId?.toString());
        this.relatedLoading = false;
      },
      error: () => {
        this.relatedLoading = false;
        this.relatedError = true;
      }
    })
  }

  protected showShippingDialog() {
    this.visibleShippingDialog = true;
  }

  protected showAvailabilityDialog() {
    this.visibleAvailabilityDialog = true;
  }


  protected loadReviews() {
    this.reviewService.getReviewsByProductId(this.product.id).subscribe({
      next: (reviews) => {
        this.productReviews = reviews;
        const stars = [
          { value: 5, count: 0 },
          { value: 4, count: 0 },
          { value: 3, count: 0 },
          { value: 2, count: 0 },
          { value: 1, count: 0 },
        ];

        this.product.totalReviews = this.productReviews.length;
        this.product.averageRating = this.productReviews.length
          ? this.productReviews.reduce((acc, r) => acc + r.rating, 0) / this.productReviews.length
          : 0;
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

  protected loadShopStocks() {
    this.productService.getStockByProductId(this.product.id).subscribe({
      next: (s) => {
        this.stocks = s;
      }
    })
  }

  protected addItemToCart() {
    if(!this.authService.isLogged()){
      this.router.navigate(['/login']);
    }

    if(this.quantity == 0){
      this.messageService.add({ severity: 'info', summary: 'Cantidad no válida', detail: 'Introduce una cantidad válida' });
    }
    else{
      this.orderService.addItemToCart(this.product.id, this.quantity).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto añadido correctamente al carrito' });
          this.product.totalUnits -= this.quantity;
          this.orderService.incrementItemsCount(this.quantity);
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 405){
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No hay suficiente stock disponible' });
          }
          else{
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error añadiendo el producto al carrito' });
          }
        }
      })
    }
  }

  protected checkInFavourites() {
    this.productService.checkInFavourites(this.product.id).subscribe({
      next: () => {
        this.inFavourites = true;
      },
      error: () => { //The only error that could be caught is 400 (bad request), as other errors will have stopped this method from running
        this.inFavourites = false;
      }
    })
  }

  protected toggleInFavourites() {
    if(!this.authService.isLogged()){
      this.router.navigate(['/login']);
    }

    if(!this.inFavourites){
      this.productService.addProductToFavourites(this.product.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'info', summary: 'Añadido', detail: 'Producto añadido a tus favoritos' });
          this.inFavourites = true;
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error añadiendo el producto a tus favoritos' });
        }
      })
    }
    else{
      this.productService.deleteProductFromFavourites(this.product.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'info', summary: 'Añadido', detail: 'Producto eliminado de tus favoritos' });
          this.inFavourites = false;
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error eliminando el producto de tus favoritos' });
        }
      })
    }

  }
}
