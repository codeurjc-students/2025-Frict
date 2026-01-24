import {ImageInfo} from './imageInfo.model';

export interface Category {
  id: string;
  name: string;
  icon: string;
  bannerText: string;
  shortDescription: string;
  longDescription: string;
  imageInfo: ImageInfo;
  parentId: string;
  children: Category[];
}
