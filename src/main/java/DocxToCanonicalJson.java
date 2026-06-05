import org.apache.poi.xwpf.usermodel.*;
import tools.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocxToCanonicalJson {

    public static void main(String[] args) throws Exception {

        String docxPath = "/Users/ayushbansal/dev/2026/java/document-converter-poc/docs/sample.docx";

        Map<String, Object> document = extract(docxPath);

        ObjectMapper mapper = new ObjectMapper();

        String json = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(document);

        System.out.println(json);
        PdfGenerator.generate(document, "output_enhanced.pdf");

        System.out.println("PDF Generated");
    }

    public static Map<String, Object> extract(String docxPath) throws Exception {

        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", "DOCX");
        metadata.put("fileName", Path.of(docxPath).getFileName().toString());

        List<Map<String, Object>> blocks = new ArrayList<>();

        try (
                FileInputStream fis = new FileInputStream(docxPath);
                XWPFDocument document = new XWPFDocument(fis)
        ) {

            for (IBodyElement bodyElement : document.getBodyElements()) {

                if (bodyElement instanceof XWPFParagraph paragraph) {

                    processParagraph(paragraph, blocks);
                }

                if (bodyElement instanceof XWPFTable table) {

                    processTable(table, blocks);
                }
            }

            processImages(document, blocks);
        }

        result.put("metadata", metadata);
        result.put("blocks", blocks);

        return result;
    }

    private static void processParagraph(
            XWPFParagraph paragraph,
            List<Map<String, Object>> blocks
    ) {

        String text = paragraph.getText();

        if (text == null || text.isBlank()) {
            return;
        }

        String style = paragraph.getStyle();

        if (style != null && style.matches("Heading\\d+")) {

            int level = Integer.parseInt(
                    style.replace("Heading", "")
            );

            Map<String, Object> heading = new LinkedHashMap<>();
            heading.put("type", "heading");
            heading.put("level", level);
            heading.put("text", text);

            blocks.add(heading);

        } else {

            Map<String, Object> paragraphBlock = new LinkedHashMap<>();
            paragraphBlock.put("type", "paragraph");
            paragraphBlock.put("text", text);

            blocks.add(paragraphBlock);
        }
    }

    private static void processTable(
            XWPFTable table,
            List<Map<String, Object>> blocks
    ) {

        List<List<String>> rows = new ArrayList<>();

        for (XWPFTableRow row : table.getRows()) {

            List<String> cells = new ArrayList<>();

            for (XWPFTableCell cell : row.getTableCells()) {

                cells.add(
                        cell.getText()
                                .replace("\n", " ")
                                .trim()
                );
            }

            rows.add(cells);
        }

        if (rows.isEmpty()) {
            return;
        }

        Map<String, Object> tableBlock = new LinkedHashMap<>();
        tableBlock.put("type", "table");

        if (rows.size() >= 1) {
            tableBlock.put("headers", rows.get(0));
        }

        if (rows.size() > 1) {
            tableBlock.put(
                    "rows",
                    rows.subList(1, rows.size())
            );
        } else {
            tableBlock.put("rows", List.of());
        }

        blocks.add(tableBlock);
    }

    private static void processImages(
            XWPFDocument document,
            List<Map<String, Object>> blocks
    ) {

        int imageCounter = 1;

        for (XWPFPictureData picture : document.getAllPictures()) {

            Map<String, Object> imageBlock = new LinkedHashMap<>();

            imageBlock.put("type", "image");
            imageBlock.put("name", "image_" + imageCounter++);
            imageBlock.put("pictureType", picture.getPictureType());
            imageBlock.put("sizeBytes", picture.getData().length);

            blocks.add(imageBlock);
        }
    }
}