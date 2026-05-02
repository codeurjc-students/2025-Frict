package com.tfg.backend.registry;

import com.tfg.backend.dto.PdfExportRequest;
import com.tfg.backend.notification.EntityType;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistryService {

    private final RegistryRepository repository;

    public Page<org.bson.Document> getRegistryStats(
            Date startDate, Date endDate, String viewType, String interval,
            EntityType entityType, RegistryType dataType,
            String metricMode,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds,
            int page, int size) {

        return repository.getRegistryData(startDate, endDate, viewType, interval,
                entityType, dataType, metricMode, storeIds, userIds, productIds, orderIds, page, size);
    }

    public List<String> getActiveEntityTypes() {
        return repository.getActiveEntityTypes();
    }

    public List<String> getActiveDataTypes(EntityType entityType) {
        return repository.getActiveDataTypes(entityType);
    }

    public Map<String, List<String>> getCrossReferences(EntityType entityType, RegistryType dataType) {
        return repository.getCrossReferences(entityType, dataType);
    }


    static class HeaderFooterEvent extends PdfPageEventHelper {
        private final String periodText;
        private final Font watermarkFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(148, 163, 184));
        private Image logoImage;

        public HeaderFooterEvent(Date startDate, Date endDate) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            this.periodText = "Período: " + sdf.format(startDate) + " - " + sdf.format(endDate);

            try {
                logoImage = Image.getInstance(Objects.requireNonNull(getClass().getResource("/static/img/frictLogo.png")));
                logoImage.scaleToFit(20, 20);
            } catch (Exception e) {
                logoImage = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            float startX = document.left();
            if (logoImage != null) {
                logoImage.setAbsolutePosition(startX, document.top() + 11);
                try { cb.addImage(logoImage); } catch (Exception ignored) {}
                startX += 28;
            }

            Phrase headerLeft = new Phrase("Frict", new Font(Font.HELVETICA, 13, Font.BOLD, new Color(100, 116, 139)));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, headerLeft, startX, document.top() + 15, 0);

            Phrase headerCenter = new Phrase("Informe de resultados", watermarkFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, headerCenter, (document.right() - document.left()) / 2 + document.leftMargin(), document.top() + 15, 0);

            Phrase headerRight = new Phrase(periodText, watermarkFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, headerRight, document.right(), document.top() + 15, 0);
            
            Phrase footer = new Phrase("Página " + writer.getPageNumber(), watermarkFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, footer, document.right(), document.bottom() - 15, 0);
        }
    }

    public byte[] generateCustomPdf(List<org.bson.Document> records, PdfExportRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 40, 40, 70, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterEvent(request.getStartDate(), request.getEndDate()));
            document.open();

            Color textDark = new Color(30, 41, 59);
            Color textMuted = new Color(100, 116, 139);
            Color primaryBlue = new Color(37, 99, 235);
            Color borderLight = new Color(241, 245, 249);
            Color bgLight = new Color(248, 250, 252);

            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, primaryBlue);
            Font subtitleFont = new Font(Font.HELVETICA, 9, Font.BOLD, textMuted);
            Font thFont = new Font(Font.HELVETICA, 8, Font.BOLD, textMuted);
            Font tdFont = new Font(Font.HELVETICA, 9, Font.NORMAL, textDark);
            Font tdBlueFont = new Font(Font.HELVETICA, 9, Font.BOLD, primaryBlue);

            document.add(new Paragraph("Informe de resultados", titleFont));

            SimpleDateFormat sdfDateOnly = new SimpleDateFormat("dd/MM/yyyy");
            String entityName = request.getEntityType().getTranslation();
            String metricName = request.getDataType() != null ? String.valueOf(request.getDataType()) : "Todas";
            boolean isTotalMode = "TOTAL".equalsIgnoreCase(request.getMetricMode());
            String modeName = isTotalMode ? "Acumulado" : "Variación";
            String periodText = sdfDateOnly.format(request.getStartDate()) + " - " + sdfDateOnly.format(request.getEndDate());

            String intervalName = request.getInterval() != null ? request.getInterval().toUpperCase() : "GENERAL";

            String subtitleText = String.format("%s  •  %s  •  %s  •  MODO %s  •  %s",
                    entityName.toUpperCase(), metricName.toUpperCase(), modeName.toUpperCase(), intervalName, periodText);

            Paragraph subtitle = new Paragraph(subtitleText, subtitleFont);
            subtitle.setSpacingAfter(25f);
            document.add(subtitle);

            if (request.getChartImage() != null && request.getChartImage().contains(",")) {
                String base64Data = request.getChartImage().split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                Image chartImage = Image.getInstance(imageBytes);

                chartImage.scaleToFit(500, 240);
                chartImage.setAlignment(Element.ALIGN_CENTER);
                document.add(chartImage);
            }

            double kpiValue = 0;

            if (records != null && !records.isEmpty()) {
                if (isTotalMode) {
                    org.bson.Document latestRecord = records.getFirst();
                    org.bson.Document metrics = latestRecord.get("metrics", org.bson.Document.class);
                    if (metrics != null && metrics.get("total") != null) {
                        kpiValue = ((Number) metrics.get("total")).doubleValue();
                    }
                } else {
                    for (org.bson.Document doc : records) {
                        org.bson.Document metrics = doc.get("metrics", org.bson.Document.class);
                        if (metrics != null && metrics.get("value") != null) {
                            kpiValue += ((Number) metrics.get("value")).doubleValue();
                        }
                    }
                }
            }

            PdfPTable kpiTable = new PdfPTable(2);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingBefore(20f);
            kpiTable.setSpacingAfter(20f);

            PdfPCell cLLabel = new PdfPCell(new Phrase("TOTAL REGISTROS", thFont));
            cLLabel.setBorder(Rectangle.NO_BORDER);
            cLLabel.setBackgroundColor(bgLight);
            cLLabel.setPaddingTop(12f);
            cLLabel.setPaddingLeft(15f);

            String kpiLabelText = isTotalMode ? "VALOR ACUMULADO (MÁS RECIENTE)" : "VOLUMEN TOTAL (VARIACIÓN)";
            PdfPCell cRLabel = new PdfPCell(new Phrase(kpiLabelText, thFont));
            cRLabel.setBorder(Rectangle.NO_BORDER);
            cRLabel.setBackgroundColor(bgLight);
            cRLabel.setPaddingTop(12f);
            cRLabel.setPaddingRight(15f);
            cRLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);

            assert records != null;
            PdfPCell cLVal = new PdfPCell(new Phrase(String.valueOf(records.size()), new Font(Font.HELVETICA, 18, Font.BOLD, primaryBlue)));
            cLVal.setBorder(Rectangle.NO_BORDER);
            cLVal.setBackgroundColor(bgLight);
            cLVal.setPaddingBottom(12f);
            cLVal.setPaddingLeft(15f);

            PdfPCell cRVal = new PdfPCell(new Phrase(String.format(java.util.Locale.US, "%.1f", kpiValue).replace(".0", ""), new Font(Font.HELVETICA, 18, Font.BOLD, primaryBlue)));
            cRVal.setBorder(Rectangle.NO_BORDER);
            cRVal.setBackgroundColor(bgLight);
            cRVal.setPaddingBottom(12f);
            cRVal.setPaddingRight(15f);
            cRVal.setHorizontalAlignment(Element.ALIGN_RIGHT);

            kpiTable.addCell(cLLabel);
            kpiTable.addCell(cRLabel);
            kpiTable.addCell(cLVal);
            kpiTable.addCell(cRVal);

            document.add(kpiTable);

            PdfPTable dataTable = new PdfPTable(5);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{1.5f, 1.5f, 3f, 1.5f, 1.5f});

            SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd/MM/yy HH:mm");
            String[] headers = {"FECHA", "ID " + entityName.toUpperCase(), entityName.toUpperCase(), "VARIACIÓN", "ACUMULADO"};

            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, thFont));
                headerCell.setBorder(Rectangle.BOTTOM);
                headerCell.setBorderColorBottom(borderLight);
                headerCell.setBorderWidthBottom(1.5f);
                headerCell.setPaddingBottom(8f);
                headerCell.setPaddingTop(8f);
                dataTable.addCell(headerCell);
            }

            for (org.bson.Document row : records) {
                PdfPCell c1 = new PdfPCell(new Phrase(sdfDateTime.format(row.getDate("timestamp")), tdFont));

                org.bson.Document metadata = row.get("metadata", org.bson.Document.class);
                String entId = "-";
                String entName = "-";
                if (metadata != null) {
                    entId = metadata.entrySet().stream()
                            .filter(e -> e.getKey().endsWith("Id") && e.getValue() != null)
                            .map(e -> e.getValue().toString()).findFirst().orElse("-");
                    entName = metadata.entrySet().stream()
                            .filter(e -> e.getKey().endsWith("Name") && e.getValue() != null)
                            .map(e -> e.getValue().toString()).findFirst().orElse("-");
                }
                PdfPCell c2 = new PdfPCell(new Phrase(entId, tdFont));
                PdfPCell c3 = new PdfPCell(new Phrase(entName, tdFont));

                org.bson.Document metrics = row.get("metrics", org.bson.Document.class);
                String val = (metrics != null && metrics.get("value") != null) ? metrics.get("value").toString() : "-";
                String tot = (metrics != null && metrics.get("total") != null) ? metrics.get("total").toString() : "-";

                PdfPCell c4 = new PdfPCell(new Phrase(val, tdBlueFont));
                PdfPCell c5 = new PdfPCell(new Phrase(tot, tdBlueFont));

                PdfPCell[] rowCells = {c1, c2, c3, c4, c5};
                for (PdfPCell cell : rowCells) {
                    cell.setBorder(Rectangle.BOTTOM);
                    cell.setBorderColorBottom(borderLight);
                    cell.setPaddingTop(10f);
                    cell.setPaddingBottom(10f);
                    dataTable.addCell(cell);
                }
            }

            document.add(dataTable);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF estético", e);
        }
    }
}