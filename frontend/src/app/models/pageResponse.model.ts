
export interface PageResponse<T> {
  items: T[];
  totalItems: number;
  currentPage: number;
  lastPage: number;
  pageSize: number;
}
