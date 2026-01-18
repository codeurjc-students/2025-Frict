import {TreeNode} from 'primeng/api';

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
