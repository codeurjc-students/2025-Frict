import { Component, OnInit, signal } from '@angular/core';
import {NgIf} from '@angular/common';
import {FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule} from '@angular/forms';
import {Router, ActivatedRoute} from '@angular/router';

// PrimeNG Imports
import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {Button} from 'primeng/button';
import {Editor} from 'primeng/editor'
import {FileUpload} from 'primeng/fileupload';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {MessageService, TreeNode} from 'primeng/api';
import {ProductService} from '../../../services/product.service';
import {CategoryService} from '../../../services/category.service';
import {fixKeys, mapToTreeNodes} from '../../../utils/nodeMapper.util';
import {TreeSelect} from 'primeng/treeselect';

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
    FormsModule
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
              private categoryService: CategoryService) {

    this.productForm = this.fb.nonNullable.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      currentPrice: [0, [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.required]],
      selectedCategories: ['', [Validators.required]],
      active: [true],
      referenceCode: [{ value: '', disabled: true }]
    });
  }


  isEditMode = signal<boolean>(false);
  existingImages = signal<string[]>([]);

  productForm: FormGroup;
  categories: TreeNode[] = [];
  selectedCategories: TreeNode[] = []


  ngOnInit() {
    const productId = this.route.snapshot.paramMap.get('id');
    if (productId) {
      this.isEditMode.set(true);
      this.loadProductData(productId);
    }
    this.loadCategories();
  }

  loadCategories(){
    this.categoryService.getAllCategories().subscribe({
      next: (list) => {
        const rawCategories = list || [];
        this.categories = mapToTreeNodes(rawCategories);
        this.assignSelectedCategories();
        //this.disableParents(this.categories);
      }
    })
  }

  assignSelectedCategories(){
    if (this.categories.length > 0 && this.selectedCategories.length > 0){
      this.productForm.patchValue({ selectedCategories: fixKeys(this.selectedCategories, this.categories) });
    }
  }

  loadProductData(id: string) {
    this.productService.getProductById(id).subscribe({
      next: (product) => {
        this.selectedCategories = mapToTreeNodes(product.categories);
        this.productForm.patchValue(product);
        this.existingImages.set(product.imageUrls);
      },
      error: (err) => {
        console.error('Error al cargar el producto:', err);
      }
    });
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

  onSelect(event: any) {
    console.log(this.productForm.get('selectedCategories')?.value);
  }

  onUpload(event: any) {
    // Aquí capturas los archivos NUEVOS subidos por el componente FileUpload
    // En un escenario real, PrimeNG puede subirlos automáticamente o puedes usar (onSelect) para guardarlos en un array y enviarlos al final.
    this.messageService.add({ severity: 'info', summary: 'Imagen cargada', detail: 'Archivo listo para enviar' });
  }

  onSubmit() {
    if (this.productForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Por favor completa los campos requeridos.' });
      return;
    }

    const formValue = this.productForm.getRawValue();
    console.log('Datos a enviar al Backend:', formValue);
    console.log('Descripción (HTML):', formValue.description);

    // Lógica de guardado...
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto guardado correctamente' });

    setTimeout(() => {
      this.goBack();
    }, 1000);
  }

  goBack() {
    this.router.navigate(['/admin/products']); // Ajusta la ruta según tu routing
  }

  removeExistingImage(index: number) {
    // Lógica para marcar imagen para borrar en backend
    this.existingImages.update(imgs => imgs.filter((_, i) => i !== index));
  }
}
