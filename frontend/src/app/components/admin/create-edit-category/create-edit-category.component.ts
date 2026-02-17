import { Component, OnInit, signal, ViewChild, inject } from '@angular/core';
import {NgClass, NgIf} from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// RxJS
import { forkJoin, of } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

// PrimeNG
import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';
import { FileUpload } from 'primeng/fileupload';
import {MessageService, PrimeTemplate, TreeNode} from 'primeng/api';
import { OrganizationChart } from 'primeng/organizationchart';
import { Select } from 'primeng/select'; // Para p-select nuevo
import { Editor } from 'primeng/editor';
import { Textarea } from 'primeng/textarea';

// Custom
import { LoadingScreenComponent } from '../../common/loading-screen/loading-screen.component';
import { LocalImage } from '../../../models/localImage.model';
import { CategoryService } from '../../../services/category.service';
import { Category } from '../../../models/category.model';
import {UiService} from '../../../utils/ui.service';

@Component({
  selector: 'app-create-edit-category',
  standalone: true,
  imports: [
    Button, ReactiveFormsModule, InputText, FileUpload, NgIf, FormsModule,
    RouterLink, LoadingScreenComponent, OrganizationChart, Editor, Textarea, Select, PrimeTemplate, NgClass
  ],
  templateUrl: './create-edit-category.component.html',
  styleUrl: './create-edit-category.component.css'
})
export class CreateEditCategoryComponent implements OnInit {

  @ViewChild('fileUploader') fileUploader: FileUpload | undefined;

  // Inyecciones
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private messageService = inject(MessageService);
  private route = inject(ActivatedRoute);
  private categoryService = inject(CategoryService);
  private sanitizer = inject(DomSanitizer);
  private uiService = inject(UiService);

  // Signals
  orgChartNodes = signal<TreeNode[]>([]);
  categoryId = signal<string | null>(null);

  // Lista plana para el dropdown (con indentación visual)
  flatCategoriesList = signal<Category[]>([]);

  // Imágenes
  oldImage = signal<string | null>(null);
  existingImage = signal<string | null>(null);
  newImage = signal<LocalImage | null>(null);

  categoryForm: FormGroup;
  loading: boolean = true;
  error: boolean = false;
  protected readonly MAX_SIZE = 5000000;

  // Iconos estáticos
  availableIconsList= this.uiService.AVAILABLE_ICONS;

  constructor() {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      icon: ['pi pi-folder', []],
      parentId: [null, []], // null para raíz
      bannerText: ['', []],
      shortDescription: ['', []],
      longDescription: ['', []]
    });

    // --- ESCUCHA AUTOMÁTICA DEL CAMBIO DE PADRE ---
    // Se dispara al seleccionar en el HTML o al hacer patchValue en el TS
    this.categoryForm.get('parentId')?.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((selectedParentId) => {
        this.updateOrgChart(selectedParentId);
      });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    const parentIdParam = this.route.snapshot.paramMap.get('parentId');
    this.categoryId.set(id);
    this.loadData(id, parentIdParam);
  }

  loadData(currentId: string | null, urlParentId: string | null) {
    const allCategoriesRequest = this.categoryService.getAllCategories().pipe(
      map((response: any) => Array.isArray(response) ? response : (response.items || []))
    );

    const requests = {
      allCategoriesTree: allCategoriesRequest,
      currentCategory: currentId ? this.categoryService.getCategoryById(currentId) : of(null)
    };

    forkJoin(requests)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (response) => {
          const { allCategoriesTree, currentCategory } = response;
          // 1. Aplanamos la lista completa para el dropdown
          let flattened = this.flattenCategories(allCategoriesTree);

          // 2. FILTRADO INTELIGENTE (Excluir rama completa)
          if (currentId && currentCategory) {
            // Obtenemos el ID de la categoría actual y todos sus hijos/nietos
            const forbiddenIds = this.getForbiddenIds(currentCategory);

            // Filtramos la lista plana: eliminamos cualquier categoría que esté en la "lista negra"
            flattened = flattened.filter(c => !forbiddenIds.includes(String(c.id)));
          }
          this.flatCategoriesList.set(flattened);

          // 3. MODO EDICIÓN
          if (currentCategory) {
            this.oldImage.set(currentCategory.imageInfo?.imageUrl || null);
            this.existingImage.set(currentCategory.imageInfo?.imageUrl || null);
            this.categoryForm.patchValue({
              name: currentCategory.name,
              icon: currentCategory.icon,
              parentId: currentCategory.parentId,
              bannerText: currentCategory.bannerText,
              shortDescription: currentCategory.shortDescription,
              longDescription: currentCategory.longDescription
            });
          }
          // 4. MODO CREACIÓN (con parámetro en URL)
          else if (urlParentId) {
            const parentExists = this.flatCategoriesList().some(c => c.id == urlParentId);
            if (parentExists) {
              this.categoryForm.patchValue({ parentId: urlParentId });
            }
          }
        },
        error: (err) => {
          console.error(err);
          this.error = true;
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los datos.' });
        }
      });
  }

  private getForbiddenIds(cat: Category): string[] {
    let ids: string[] = [String(cat.id)];

    if (cat.children && cat.children.length > 0) {
      cat.children.forEach(child => {
        ids = [...ids, ...this.getForbiddenIds(child)];
      });
    }
    return ids;
  }

  flattenCategories(categories: Category[], level: number = 0): Category[] {
    let result: Category[] = [];

    for (const cat of categories) {
      const flatCat: Category = { ...cat, name: cat.name };
      result.push(flatCat);

      if (cat.children && cat.children.length > 0) {
        result = result.concat(this.flattenCategories(cat.children, level + 1));
      }
    }
    return result;
  }

  updateOrgChart(parentId: number | string | null) {
    // Si no hay lista o no hay ID, limpiar gráfico
    if (!parentId || this.flatCategoriesList().length === 0) {
      this.orgChartNodes.set([]);
      return;
    }

    // Usamos '==' para comparar loose (string "1" vs number 1)
    const parentCategory = this.flatCategoriesList().find(c => c.id == parentId);

    if (parentCategory) {
      // Limpiamos el nombre de los guiones para que se vea bonito en el Gráfico
      const cleanCategory = {
        ...parentCategory,
        name: parentCategory.name
      };

      this.orgChartNodes.set([this.mapToOrgChart(cleanCategory)]);
    } else {
      this.orgChartNodes.set([]);
    }
  }

  mapToOrgChart(cat: Category): TreeNode {
    return {
      expanded: true,
      type: 'category', // Coincide con el HTML pTemplate="category"
      styleClass: 'bg-transparent',
      data: {
        id: cat.id,
        name: cat.name,
        icon: cat.icon && cat.icon.trim() !== '' ? cat.icon : 'pi pi-folder',
        timesUsed: cat.timesUsed || 0
      },
      children: cat.children ? cat.children.map(c => this.mapToOrgChart(c)) : []
    };
  }

  // --- GETTERS UI ---
  protected availableIcons() { return this.availableIconsList; }

  // --- LÓGICA DE IMAGEN Y SUBMIT (Sin cambios funcionales mayores) ---
  onFileSelect(event: any) {
    const file = event.files[0];
    if (file && file.size <= this.MAX_SIZE) {
      const preview = this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(file));
      this.newImage.set({ file: file, previewUrl: preview });
      this.existingImage.set(null);
    }
  }

  onFileRemove(event: any) { this.newImage.set(null); }

  removeImage() {
    this.newImage.set(null);
    this.existingImage.set(null);
    if (this.fileUploader) this.fileUploader.clear();
  }

  restoreImage() {
    this.existingImage.set(this.oldImage());
  }

  onSubmit() {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Atención', detail: 'Revisa los campos.' });
      return;
    }

    const formValue = this.categoryForm.getRawValue();
    const categoryData: Category = {
      ...formValue,
      id: this.categoryId() ? this.categoryId() : undefined
    };

    const imageFile = this.newImage()?.file;
    const request$ = this.categoryId()
      ? this.categoryService.updateCategory(this.categoryId()!, categoryData)
      : this.categoryService.createCategory(categoryData);

    request$.subscribe({
      next: (res) => {
        if (imageFile) {
          this.updateCategoryImage(res.id.toString(), imageFile);
        } else {
          this.finishSubmit();
        }
      },
      error: () => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Fallo al guardar.' });
      }
    });
  }

  protected updateCategoryImage(id: string, image: File) {
    this.categoryService.updateCategoryImage(id, image).subscribe({
      next: () => this.finishSubmit(),
      error: () => this.finishSubmit('warn')
    });
  }

  private finishSubmit(severity: string = 'success') {
    this.loading = false;
    const detail = severity === 'success' ? 'Categoría guardada.' : 'Categoría guardada (error imagen).';
    this.messageService.add({ severity: severity, summary: 'Info', detail: detail });
    this.router.navigate(['/admin/categories']);
  }
}
