package com.tfg.backend.utils;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.tfg.backend.dto.PdfReportDTO;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PdfService {

    // --- MANEJADOR DE CABECERA Y PIE DE PÁGINA (iText 8) ---
    private static class HeaderFooterEventHandler implements IEventHandler {
        private final String rightSideText;
        private Image logoImage;

        public HeaderFooterEventHandler(String rightSideText) {
            this.rightSideText = rightSideText;
            try {
                byte[] logoBytes = Objects.requireNonNull(getClass().getResourceAsStream("/static/img/frictLogo.png")).readAllBytes();
                logoImage = new Image(ImageDataFactory.create(logoBytes)).scaleToFit(16, 16);
            } catch (Exception e) {
                logoImage = null;
            }
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();

            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
            Canvas canvas = new Canvas(pdfCanvas, pageSize);

            try {
                PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                PdfFont fontNorm = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                Color textMuted = new DeviceRgb(148, 163, 184);

                float startX = 50;
                float textYPosition = pageSize.getTop() - 40;

                if (logoImage != null) {
                    logoImage.setFixedPosition(startX, textYPosition - 2);
                    canvas.add(logoImage);
                    startX += 20;
                }

                canvas.showTextAligned(new Paragraph("Frict").setFont(fontBold).setFontSize(15).setFontColor(textMuted),
                        startX, textYPosition, TextAlignment.LEFT);

                canvas.showTextAligned(new Paragraph(rightSideText).setFont(fontNorm).setFontSize(8).setFontColor(textMuted),
                        pageSize.getRight() - 50, textYPosition, TextAlignment.RIGHT);

                canvas.showTextAligned(new Paragraph("Página " + pdfDoc.getPageNumber(page)).setFont(fontNorm).setFontSize(8).setFontColor(textMuted),
                        pageSize.getRight() - 50, pageSize.getBottom() + 30, TextAlignment.RIGHT);

            } catch (Exception ignored) {}
            canvas.close();
        }
    }

    /**
     * Helper para descargar imágenes saltándose los bloqueos 403 Forbidden de servidores externos
     * simulando ser un navegador Chrome estándar y forzando formatos compatibles con iText.
     */
    private Image safeLoadImage(String imageUrl, float width, float height) {
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
            if (imageUrl.startsWith("//")) imageUrl = "https:" + imageUrl; // Arreglar URLs sin protocolo

            java.net.URL url = URI.create(imageUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Disfrazar la petición de Java como si fuera un navegador web real
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "image/jpeg, image/png, image/gif");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Manejar redirecciones automáticamente
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = connection.getHeaderField("Location");
                return safeLoadImage(newUrl, width, height);
            }

            try (InputStream in = connection.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return new Image(ImageDataFactory.create(bytes)).scaleToFit(width, height);
            }
        } catch (Exception e) {
            System.err.println("No se pudo pintar la imagen (" + imageUrl + ") en el PDF: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // 1. GENERADOR DE INFORMES DE MÉTRICAS (iText 8)
    // =========================================================================
    public byte[] generateCustomPdfReport(List<org.bson.Document> records, PdfReportDTO request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String periodText = "Periodo: " + sdf.format(request.getStartDate()) + " - " + sdf.format(request.getEndDate());
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new HeaderFooterEventHandler(periodText));

            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(80, 50, 80, 50);

            Color textDark = new DeviceRgb(30, 41, 59);
            Color textMuted = new DeviceRgb(100, 116, 139);
            Color primaryBlue = new DeviceRgb(37, 99, 235);
            Color borderLight = new DeviceRgb(241, 245, 249);
            Color bgLight = new DeviceRgb(248, 250, 252);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNorm = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            document.add(new Paragraph("Informe de resultados").setFont(fontBold).setFontSize(22).setFontColor(primaryBlue));

            String entityName = request.getEntityType().getTranslation();
            String metricName = request.getDataType() != null ? String.valueOf(request.getDataType()) : "Todas";
            boolean isTotalMode = "TOTAL".equalsIgnoreCase(request.getMetricMode());
            String modeName = isTotalMode ? "Acumulado" : "Variación";

            String subtitleText = String.format("%s  •  %s  •  MODO %s  •  %s",
                    entityName.toUpperCase(), metricName.toUpperCase(), modeName.toUpperCase(), periodText);

            document.add(new Paragraph(subtitleText).setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(25));

            if (request.getChartImage() != null && request.getChartImage().contains(",")) {
                String base64Data = request.getChartImage().split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                Image chartImage = new Image(ImageDataFactory.create(imageBytes)).scaleToFit(500, 240).setHorizontalAlignment(HorizontalAlignment.CENTER);
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

            Table kpiTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            kpiTable.setMarginTop(20).setMarginBottom(20);

            String kpiLabelText = isTotalMode ? "VALOR ACUMULADO (MÁS RECIENTE)" : "VOLUMEN TOTAL (VARIACIÓN)";

            Cell cLLabel = new Cell().add(new Paragraph("TOTAL REGISTROS").setFont(fontBold).setFontSize(8).setFontColor(textMuted)).setBorder(Border.NO_BORDER).setBackgroundColor(bgLight).setPaddingTop(12).setPaddingLeft(15);
            Cell cRLabel = new Cell().add(new Paragraph(kpiLabelText).setFont(fontBold).setFontSize(8).setFontColor(textMuted)).setBorder(Border.NO_BORDER).setBackgroundColor(bgLight).setPaddingTop(12).setPaddingRight(15).setTextAlignment(TextAlignment.RIGHT);
            Cell cLVal = null;
            if (records != null) {
                cLVal = new Cell().add(new Paragraph(String.valueOf(records.size())).setFont(fontBold).setFontSize(18).setFontColor(primaryBlue)).setBorder(Border.NO_BORDER).setBackgroundColor(bgLight).setPaddingBottom(12).setPaddingLeft(15);
            }
            Cell cRVal = new Cell().add(new Paragraph(String.format(java.util.Locale.US, "%.1f", kpiValue).replace(".0", "")).setFont(fontBold).setFontSize(18).setFontColor(primaryBlue)).setBorder(Border.NO_BORDER).setBackgroundColor(bgLight).setPaddingBottom(12).setPaddingRight(15).setTextAlignment(TextAlignment.RIGHT);

            kpiTable.addCell(cLLabel).addCell(cRLabel).addCell(cLVal).addCell(cRVal);
            document.add(kpiTable);

            Table dataTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1.5f, 3f, 1.5f, 1.5f})).useAllAvailableWidth();
            SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd/MM/yy HH:mm");
            String[] headers = {"FECHA", "REFERENCIA", "NOMBRE", "VARIACIÓN", "ACUMULADO"};

            for (String header : headers) {
                dataTable.addHeaderCell(new Cell().add(new Paragraph(header).setFont(fontBold).setFontSize(8).setFontColor(textMuted))
                        .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(borderLight, 1.5f)).setPaddingBottom(8));
            }

            for (org.bson.Document row : records) {
                org.bson.Document metadata = row.get("metadata", org.bson.Document.class);
                String entId = "-", entName = "-";

                if (metadata != null && request.getEntityType() != null) {
                    String baseKey = request.getEntityType().name().toLowerCase();
                    if (metadata.get(baseKey + "Id") != null) entId = metadata.get(baseKey + "Id").toString();
                    if (metadata.get(baseKey + "Name") != null) entName = metadata.get(baseKey + "Name").toString();
                }

                org.bson.Document metrics = row.get("metrics", org.bson.Document.class);
                String val = (metrics != null && metrics.get("value") != null) ? metrics.get("value").toString() : "-";
                String tot = (metrics != null && metrics.get("total") != null) ? metrics.get("total").toString() : "-";

                String[] rowData = {sdfDateTime.format(row.getDate("timestamp")), entId, entName, val, tot};
                for (int i = 0; i < rowData.length; i++) {
                    Color cellColor = (i >= 3) ? primaryBlue : textDark;
                    PdfFont cellFont = (i >= 3) ? fontBold : fontNorm;
                    dataTable.addCell(new Cell().add(new Paragraph(rowData[i]).setFont(cellFont).setFontSize(9).setFontColor(cellColor))
                            .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(borderLight, 1f)).setPaddingTop(10).setPaddingBottom(10));
                }
            }
            document.add(dataTable);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF estético", e);
        }
    }

    // =========================================================================
    // 2. GENERADOR DE FACTURAS DE PEDIDO (Unificado con el componente Angular)
    // =========================================================================
    public byte[] generateOrderInvoicePdf(Order order, String qrToken) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new HeaderFooterEventHandler("Ref: " + order.getReferenceCode()));

            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(80, 50, 80, 50);

            Color primaryBlue = new DeviceRgb(37, 99, 235);
            Color textDark = new DeviceRgb(30, 41, 59);
            Color textMuted = new DeviceRgb(100, 116, 139);
            Color borderLight = new DeviceRgb(226, 232, 240);
            Color bgLight = new DeviceRgb(248, 250, 252);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNorm = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            // Determinar estado actual y fecha de actualización
            String statusName = "Desconocido";
            String statusDate = "";
            if (order.getHistory() != null && !order.getHistory().isEmpty()) {
                var lastLog = order.getHistory().getLast();
                statusName = lastLog.getStatus().getDescription();
                if (lastLog.getUpdates() != null && !lastLog.getUpdates().isEmpty()) {
                    statusDate = lastLog.getUpdates().getLast().getDate().format(formatter);
                }
            }

            // --- 1. CABECERA TIPO INFORME (Resumen y QR) ---
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{3f, 1f})).useAllAvailableWidth();

            Cell infoCell = new Cell().setBorder(Border.NO_BORDER);
            infoCell.add(new Paragraph("Resumen de Pedido").setFont(fontBold).setFontSize(22).setFontColor(primaryBlue));

            String subtitleText = String.format("REFERENCIA: %s  •  ESTADO ACTUAL: %s", order.getReferenceCode(), statusName);
            infoCell.add(new Paragraph(subtitleText).setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(3));
            if (!statusDate.isEmpty()) {
                infoCell.add(new Paragraph("(Última actualización: " + statusDate + ")").setFont(fontNorm).setFontSize(8).setFontColor(textMuted).setMarginBottom(10));
            } else {
                infoCell.add(new Paragraph(" ").setFontSize(8).setMarginBottom(10));
            }

            headerTable.addCell(infoCell);

            Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            if (qrToken != null && !qrToken.isEmpty()) {
                BarcodeQRCode qrCode = new BarcodeQRCode(qrToken);
                PdfFormXObject qrXObject = qrCode.createFormXObject(ColorConstants.BLACK, pdf);
                Image qrImage = new Image(qrXObject).setWidth(80).setHeight(80).setHorizontalAlignment(HorizontalAlignment.RIGHT);
                qrCell.add(qrImage);
            }
            headerTable.addCell(qrCell);
            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // --- 2. GRID PRINCIPAL (Dos Columnas: Datos y Resumen Económico) ---
            // Un 55% para la izquierda (texto) y 45% para la derecha (números)
            Table gridTable = new Table(UnitValue.createPercentArray(new float[]{1.2f, 1f})).useAllAvailableWidth().setMarginBottom(20);

            // --- COLUMNA 1: Izquierda (Dirección, Fecha y Pago) ---
            Cell colLeft = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(20);

            // Dirección de envío
            colLeft.add(new Paragraph("DIRECCIÓN DE ENVÍO").setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(5));
            String sendingAddr = order.getFullSendingAddress() != null ? order.getFullSendingAddress() : "Dirección no disponible";
            colLeft.add(new Paragraph(sendingAddr).setFont(fontNorm).setFontSize(10).setFontColor(textDark).setMarginBottom(15));

            // Fecha Confirmación
            colLeft.add(new Paragraph("FECHA DE CONFIRMACIÓN").setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(5));
            colLeft.add(new Paragraph(order.getCreatedAt().format(formatter)).setFont(fontNorm).setFontSize(10).setFontColor(textDark).setMarginBottom(15));

            // Pago Realizado
            colLeft.add(new Paragraph("PAGO REALIZADO").setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(5));
            colLeft.add(new Paragraph("Pagado correctamente").setFont(fontBold).setFontSize(10).setFontColor(new DeviceRgb(34, 197, 94))); // Verde
            String cardEnding = order.getCardNumberEnding() != null ? order.getCardNumberEnding() : "XXXX";
            colLeft.add(new Paragraph("Tarjeta terminada en ···· " + cardEnding).setFont(fontNorm).setFontSize(9).setFontColor(textMuted));

            gridTable.addCell(colLeft);

            // --- COLUMNA 2: Derecha (Resumen Económico) ---
            Cell colRight = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(bgLight).setPadding(15);
            colRight.add(new Paragraph("RESUMEN").setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(10));

            Table summaryInner = new Table(UnitValue.createPercentArray(new float[]{3f, 1.5f})).useAllAvailableWidth();
            // Subtotal
            summaryInner.addCell(new Cell().add(new Paragraph(String.format("Subtotal (%d artículos):", order.getTotalItems())).setFont(fontNorm).setFontSize(9).setFontColor(textMuted)).setBorder(Border.NO_BORDER).setPaddingBottom(5));
            summaryInner.addCell(new Cell().add(new Paragraph(String.format(java.util.Locale.US, "%.2f €", order.getSubtotalCost())).setFont(fontBold).setFontSize(10).setFontColor(textDark)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPaddingBottom(5));
            // Descuentos
            summaryInner.addCell(new Cell().add(new Paragraph("Descuentos aplicados:").setFont(fontNorm).setFontSize(9).setFontColor(textMuted)).setBorder(Border.NO_BORDER).setPaddingBottom(5));
            summaryInner.addCell(new Cell().add(new Paragraph(String.format(java.util.Locale.US, "-%.2f €", order.getTotalDiscount())).setFont(fontBold).setFontSize(10).setFontColor(textDark)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPaddingBottom(5));
            // Envío
            summaryInner.addCell(new Cell().add(new Paragraph("Coste de envío:").setFont(fontNorm).setFontSize(9).setFontColor(textMuted)).setBorder(Border.NO_BORDER).setPaddingBottom(5));
            summaryInner.addCell(new Cell().add(new Paragraph(String.format(java.util.Locale.US, "%.2f €", order.getShippingCost())).setFont(fontBold).setFontSize(10).setFontColor(textDark)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPaddingBottom(5));

            colRight.add(summaryInner);
            colRight.add(new Paragraph("").setMarginTop(5).setMarginBottom(10).setBorderBottom(new SolidBorder(borderLight, 1f))); // Separador

            Table totalInner = new Table(UnitValue.createPercentArray(new float[]{1f, 1f})).useAllAvailableWidth();
            totalInner.addCell(new Cell().add(new Paragraph("Total").setFont(fontBold).setFontSize(12).setFontColor(textDark)).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.BOTTOM));
            Double totalValue = order.getTotalCost() + order.getShippingCost();
            totalInner.addCell(new Cell().add(new Paragraph(String.format(java.util.Locale.US, "%.2f €", totalValue)).setFont(fontBold).setFontSize(16).setFontColor(primaryBlue)).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            colRight.add(totalInner);

            gridTable.addCell(colRight);
            document.add(gridTable);

            // --- 3. SECCIÓN DE TIENDA (Con foto e información del Manager) ---
            document.add(new Paragraph("ATENDIDO POR").setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(5));
            Table shopInnerTable = new Table(UnitValue.createPercentArray(new float[]{1f, 7f})).useAllAvailableWidth().setMarginBottom(20);

            Cell shopImgCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(15).setVerticalAlignment(VerticalAlignment.MIDDLE);
            Cell shopTextCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setVerticalAlignment(VerticalAlignment.MIDDLE);

            if (order.getAssignedShop() != null) {
                Image shopImg = null;
                if (order.getAssignedShop().getImage() != null && order.getAssignedShop().getImage().getImageUrl() != null) {
                    shopImg = safeLoadImage(order.getAssignedShop().getImage().getImageUrl(), 50, 50);
                }
                if (shopImg != null) {
                    shopImgCell.add(shopImg);
                } else {
                    // Placeholder vacío si no hay imagen
                    shopImgCell.add(new Paragraph(" ").setHeight(50).setWidth(50));
                }

                shopTextCell.add(new Paragraph(order.getAssignedShop().getName()).setFont(fontBold).setFontSize(11).setFontColor(textDark));

                if(order.getAssignedShop().getAddress() != null) {
                    shopTextCell.add(new Paragraph(order.getAssignedShop().getAddress().getStreet() + ", " + order.getAssignedShop().getAddress().getCity()).setFont(fontNorm).setFontSize(9).setFontColor(textMuted));
                }

                // Información de contacto del Manager
                if (order.getAssignedShop().getAssignedManager() != null) {
                    var manager = order.getAssignedShop().getAssignedManager();
                    String contactName = "Manager: " + manager.getName();
                    String contactEmail = "Correo: " + manager.getEmail();
                    shopTextCell.add(new Paragraph(contactName).setFont(fontNorm).setFontSize(9).setFontColor(textMuted));
                    shopTextCell.add(new Paragraph(contactEmail).setFont(fontNorm).setFontSize(9).setFontColor(textMuted));
                }
            } else {
                shopTextCell.add(new Paragraph("Tienda Central Frict").setFont(fontBold).setFontSize(11).setFontColor(textDark));
            }
            shopInnerTable.addCell(shopImgCell).addCell(shopTextCell);
            document.add(shopInnerTable);

            // --- 4. LISTA DE PRODUCTOS ---
            document.add(new Paragraph("CONTENIDO DEL PEDIDO").setFont(fontBold).setFontSize(9).setFontColor(textMuted).setMarginBottom(10));

            Table listTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 5f, 1.5f, 2f})).useAllAvailableWidth();

            for (OrderItem item : order.getItems()) {
                // 1. FOTO (Carga segura)
                Cell imgCell = new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(borderLight, 1f)).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE);
                Image pImg = safeLoadImage(item.getProductImageUrl(), 40, 40);
                if (pImg != null) {
                    pImg.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    imgCell.add(pImg);
                }
                listTable.addCell(imgCell);

                // 2. DETALLES (Nombre + Precio/ud debajo)
                Cell detailsContentCell = new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(borderLight, 1f)).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE);
                detailsContentCell.add(new Paragraph(item.getProductName()).setFont(fontBold).setFontSize(10).setFontColor(textDark));
                detailsContentCell.add(new Paragraph(String.format(java.util.Locale.US, "%.2f € / ud.", item.getProductPrice())).setFont(fontNorm).setFontSize(9).setFontColor(textMuted));
                listTable.addCell(detailsContentCell);

                // 3. UNIDADES
                Cell qtyListCell = new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(borderLight, 1f)).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.CENTER);
                qtyListCell.add(new Paragraph(item.getQuantity() + " uds.").setFont(fontBold).setFontSize(10).setFontColor(textDark));
                listTable.addCell(qtyListCell);

                // 4. TOTAL (Destacado en Azul)
                Cell totalListCell = new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(borderLight, 1f)).setPadding(10).setVerticalAlignment(VerticalAlignment.MIDDLE).setTextAlignment(TextAlignment.RIGHT);
                totalListCell.add(new Paragraph(String.format(java.util.Locale.US, "%.2f €", item.getQuantity() * item.getProductPrice())).setFont(fontBold).setFontSize(11).setFontColor(primaryBlue));
                listTable.addCell(totalListCell);
            }
            document.add(listTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando la factura PDF con iText", e);
        }
    }
}