import {TreeNode} from 'primeng/api';
import {Category} from '../models/category.model';

//Create an unselected category tree
export function mapToTreeNodes(categories: any[], parentKey: string | null = null): TreeNode[] {
  return categories.map((cat, index) => {
    const currentKey = parentKey !== null ? `${parentKey}-${index}` : `${index}`;
    return {
      key: currentKey,
      label: cat.name,
      data: cat.id,
      children: cat.children ? mapToTreeNodes(cat.children, currentKey) : [],
    };
  });
}

//Create a category object list from tree nodes (valuable fields: id and name only)
export function mapToCategories(nodes: TreeNode[], parentId: string = ''): Category[] {
  if (!nodes) return [];

  const result = nodes.map(node => ({
    id: String(node.data),
    name: node.label || '',
    parentId: parentId,
    children: mapToCategories(node.children || [], String(node.data)),

    // Boilerplate in order to fit to interface
    icon: '',
    bannerText: '',
    shortDescription: '',
    longDescription: '',
    imageInfo: { id: 0, imageUrl: '', s3Key: '', fileName: '' } as any,
    timesUsed: 0
  }));
  return result;
}

//Mark selected categories in category tree from backend data
export function fixKeys(selectedList: TreeNode[], masterTree: TreeNode[]) {
  const findCorrectKey = (nodes: any[], idTarget: any): string | undefined => {
    for (const node of nodes) {
      if (node.data === idTarget) return node.key;

      if (node.children && node.children.length > 0) {
        const found = findCorrectKey(node.children, idTarget);
        if (found) return found;
      }
    }
    return undefined;
  };

  selectedList.forEach(item => {
    const realKey = findCorrectKey(masterTree, item.data);
    if (realKey) {
      item.key = realKey;
    }
  });

  return selectedList;
}
