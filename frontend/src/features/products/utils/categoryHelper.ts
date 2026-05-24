import { CategoryResponse } from '../types/product';

export interface FlatCategory {
  id: number;
  name: string;
  displayName: string;
  parentId?: number | null;
}

export const flattenCategoryTree = (
  categories: CategoryResponse[],
  depth = 0
): FlatCategory[] => {
  let flatList: FlatCategory[] = [];

  categories.forEach((cat) => {
    // Generate prefix based on depth
    let prefix = '';
    if (depth > 0) {
      prefix = '\u00A0\u00A0'.repeat(depth * 2) + '└── ';
    }

    flatList.push({
      id: cat.id,
      name: cat.name,
      displayName: `${prefix}${cat.name}`,
      parentId: cat.parentId,
    });

    if (cat.children && cat.children.length > 0) {
      flatList = [...flatList, ...flattenCategoryTree(cat.children, depth + 1)];
    }
  });

  return flatList;
};
