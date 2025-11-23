export interface Review {
  id: string;
  productId: string;
  creatorId: string;
  creatorName: string;
  creatorThumbnailUrl: string;
  text: string;
  rating: number;
  createdAt: string;
  recommended: boolean;
}
