export interface SeriesBundle {
  name: string;
  data: number[];
}

export interface DataSeries {
  labels: string[];
  series: SeriesBundle[];
}
