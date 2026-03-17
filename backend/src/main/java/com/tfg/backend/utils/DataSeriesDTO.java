package com.tfg.backend.utils;

import java.util.List;

public record DataSeriesDTO(
        List<String> labels,
        List<SeriesBundle> series
) {
    public record SeriesBundle(
            String name,
            List<? extends Number> data
    ) {}
}
