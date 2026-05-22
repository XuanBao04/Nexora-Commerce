export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  status: 'ACTIVE' | 'INACTIVE';
  imageUrl?: string;
  imagePublicId?: string;
}

export interface ProductResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  status: string;
  imageUrl?: string;
  imagePublicId?: string;
}
