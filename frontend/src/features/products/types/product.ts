export interface ProductVariantAttribute {
  name: string;
  value: string;
}

export interface ProductVariant {
  sku: string;
  price: number;
  quantity: number;
  reservedQuantity?: number;
  soldQuantity?: number;
  attributes?: ProductVariantAttribute[];
}

export interface ProductImage {
  id: number;
  sku?: string | null;
  imageUrl: string;
  isPrimary: boolean;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  status: 'ACTIVE' | 'INACTIVE';
  variants: ProductVariant[];
  images: ProductImage[];
  categoryId?: number;
  brandId?: number;
  imageUrl?: string;
  price?: number;
  quantity?: number;
  categoryName?: string;
  brandName?: string;
}

export type ProductResponse = Product;

export interface ProductFormRequest {
  id?: string;
  name: string;
  description: string;
  status: 'ACTIVE' | 'INACTIVE';
  categoryId?: number;
  brandId?: number;
  variants: ProductVariant[];
}

export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  parentId?: number | null;
  children?: CategoryResponse[];
}

export interface CategoryRequest {
  name: string;
  slug?: string;
  parentId?: number | null;
}

export interface BrandResponse {
  id: number;
  name: string;
  slug: string;
}

export interface BrandRequest {
  name: string;
  slug?: string;
}
