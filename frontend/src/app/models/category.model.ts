export interface Category {
  id: string;
  name: string;
  icon: string;
  bannerText: string;
  shortDescription: string;
  longDescription: string;
  imageUrl: string;
  children: Category[];
}
