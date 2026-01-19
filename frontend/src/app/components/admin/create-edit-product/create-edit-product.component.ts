import { Component, OnInit, signal } from '@angular/core';
import {NgIf} from '@angular/common';
import {FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule} from '@angular/forms';
import {Router, ActivatedRoute, RouterLink} from '@angular/router';

import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Editor} from 'primeng/editor'
import {FileUpload} from 'primeng/fileupload';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {MessageService, TreeNode} from 'primeng/api';
import {ProductService} from '../../../services/product.service';
import {CategoryService} from '../../../services/category.service';
import {fixKeys, mapToCategories, mapToTreeNodes} from '../../../utils/nodeMapper.util';
import {TreeSelect} from 'primeng/treeselect';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {formatPrice} from '../../../utils/numberFormat.util';
import {Product} from '../../../models/product.model';
import {ImageInfo} from '../../../models/imageInfo.model';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {forkJoin} from 'rxjs';

interface LocalImage {
  file: File;
  previewUrl: SafeUrl;
}

@Component({
  selector: 'app-create-edit-product',
  standalone: true,
  imports: [
    Button,
    ReactiveFormsModule,
    InputText,
    InputNumber,
    Editor,
    ToggleSwitch,
    FileUpload,
    NgIf,
    TreeSelect,
    FormsModule,
    RouterLink,
    LoadingScreenComponent
  ],
  templateUrl: './create-edit-product.component.html',
  styleUrl: 'create-edit-product.component.css'
})
export class CreateEditProductComponent implements OnInit {

  constructor(private fb: FormBuilder,
              private router: Router,
              private messageService: MessageService,
              private route: ActivatedRoute,
              private productService: ProductService,
              private categoryService: CategoryService,
              private sanitizer: DomSanitizer) {

    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      currentPrice: [0, [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.required]],
      selectedCategories: ['', [Validators.required]],
      active: [true],
      referenceCode: [{ value: '', disabled: true }]
    });
  }

  productId = signal<string | null>(null);
  product = signal<Product | null>(null);
  existingImages = signal<ImageInfo[]>([]);
  newImages = signal<LocalImage[]>([]);

  protected readonly MAX_SIZE = 5000000;
  productForm: FormGroup;
  categories: TreeNode[] = [];
  selectedCategories: TreeNode[] = []

  loading: boolean = true;
  error: boolean = false;

  ngOnInit() {
    this.productId.set(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  loadData(){
    const productId = this.productId();
    if (productId){
      forkJoin({
        list: this.categoryService.getAllCategories(),
        product: this.productService.getProductById(productId)
      }).subscribe({
        next: ({ list, product }) => {
          const rawCategories = list || [];
          this.categories = mapToTreeNodes(rawCategories);
          this.selectedCategories = mapToTreeNodes(product.categories);
          this.product.set(product);
          this.productForm.patchValue(product);
          this.existingImages.set(product.imagesInfo);

          if (this.categories.length > 0 && this.selectedCategories.length > 0){
            this.productForm.patchValue({ selectedCategories: fixKeys(this.selectedCategories, this.categories) });
          }
          //this.disableParents(this.categories);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      })
    }
    else {
      this.categoryService.getAllCategories().subscribe({
        next: (list) => {
          const rawCategories = list || [];
          this.categories = mapToTreeNodes(rawCategories);
          if (this.categories.length > 0 && this.selectedCategories.length > 0){
            this.productForm.patchValue({ selectedCategories: fixKeys(this.selectedCategories, this.categories) });
          }
          //this.disableParents(this.categories);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      })
    }
  }




  private disableParents(nodes: TreeNode[]) {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        node.selectable = false;
        this.disableParents(node.children);
      } else {
        node.selectable = true;
      }
    }
  }

  onFileSelect(event: any) {
    const incomingFiles: File[] = Array.from(event.files || []);

    const validFiles: File[] = [];

    incomingFiles.forEach(file => {
      if (file.size <= this.MAX_SIZE) {
        validFiles.push(file);
      }
    });

    // Process valid files only
    if (validFiles.length > 0) {
      const currentNewImages = this.newImages();
      const processedFiles: LocalImage[] = validFiles.map(file => ({
        file: file,
        previewUrl: this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(file))
      }));

      this.newImages.set([...currentNewImages, ...processedFiles]);
      console.log("Imágenes válidas añadidas:", validFiles.length);
    }
  }

  removeNewImage(index: number) {
    this.newImages.update(imgs => imgs.filter((_, i) => i !== index));
  }

  removeExistingImage(index: number) {
    this.existingImages.update(imgs => imgs.filter((_, i) => i !== index));
  }

  onSubmit() {
    if (this.productForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos requeridos.' });
      return;
    }

    const formValue = this.productForm.getRawValue();
    console.log('Datos a enviar al Backend:', formValue);
    console.log('Descripción (HTML):', formValue.description);

    if(this.productId()){
      const editingProduct = this.product();
      if(editingProduct){
        const formValue = this.productForm.getRawValue();
        const productData: Product = {
          ...formValue,
          categories: mapToCategories(formValue.selectedCategories),
          images: this.existingImages() // Añadimos las imágenes existentes que no se borraron
        };
        this.productService.updateProduct(editingProduct.id, productData).subscribe({
          next: (product) => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Producto ${editingProduct.name} actualizado correctamente.` });
            this.updateProductImages(product.id);
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: `No se ha podido actualizar el producto ${editingProduct.name}.` });

          }
        })
      }
    }
    else{
      const formValue = this.productForm.getRawValue();
      const productData: Product = {
        ...formValue,
        categories: mapToCategories(formValue.selectedCategories),
        images: this.existingImages() // Añadimos las imágenes existentes que no se borraron
      };
      this.productService.createProduct(productData).subscribe({
        next: (product) => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto creado correctamente.' });
          this.updateProductImages(product.id);
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: `No se ha podido crear el nuevo producto.` });

        }
      })
    }
  }

  updateProductImages(id: string){
    this.productService.updateProductImages(id, this.existingImages(), this.newImages().map(img => img.file)).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Imágenes del producto ${this.product()?.name} actualizadas correctamente.` });
        this.router.navigate(['/admin/products']);
      }
    })
  }

  printSelectedCategories(){
    console.log(this.productForm.value);
  }

  protected readonly formatPrice = formatPrice;
}
