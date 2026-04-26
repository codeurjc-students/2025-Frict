import { Connection } from './connection.model';

export interface Review {
  id: string;
  productId: string;
  creatorId: string;
  productName: string;

  // Flattened user fields
  creatorUsername: string; // Needed for mapping
  creatorName: string;
  creatorImage: string;
  creatorConnection: Connection | null;

  text: string;
  rating: number;
  createdAt: string;
  recommended: boolean;
}
