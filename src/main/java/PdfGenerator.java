import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PdfGenerator {

    private static final float PAGE_MARGIN = 50f;
    private static final float PAGE_TOP = 750f;
    private static final float PAGE_BOTTOM = 50f;

    private static final float CONTENT_WIDTH = 500f;

    private static final float NORMAL_FONT_SIZE = 11f;
    private static final float H1_FONT_SIZE = 18f;
    private static final float H2_FONT_SIZE = 15f;

    public static void generate(
            Map<String, Object> document,
            String outputFile
    ) throws Exception {

        try (PDDocument pdf = new PDDocument()) {

            PageContext ctx = createPage(pdf);

            List<Map<String, Object>> blocks =
                    (List<Map<String, Object>>) document.get("blocks");

            for (Map<String, Object> block : blocks) {

                String type =
                        String.valueOf(block.get("type"));

                switch (type) {

                    case "heading" -> {

                        int level =
                                ((Number) block.getOrDefault("level", 1))
                                        .intValue();

                        PDType1Font font =
                                new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA_BOLD
                                );

                        float fontSize =
                                level == 1
                                        ? H1_FONT_SIZE
                                        : H2_FONT_SIZE;

                        ctx = writeText(
                                pdf,
                                ctx,
                                block.get("text").toString(),
                                font,
                                fontSize
                        );

                        ctx.y -= 8;
                    }

                    case "paragraph" -> {

                        ctx = writeText(
                                pdf,
                                ctx,
                                block.get("text").toString(),
                                new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA
                                ),
                                NORMAL_FONT_SIZE
                        );

                        ctx.y -= 4;
                    }

                    case "list_item" -> {

                        ctx = writeText(
                                pdf,
                                ctx,
                                "• " + block.get("text"),
                                new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA
                                ),
                                NORMAL_FONT_SIZE
                        );
                    }

                    case "table" -> {

                        ctx = writeText(
                                pdf,
                                ctx,
                                "TABLE",
                                new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA_BOLD
                                ),
                                NORMAL_FONT_SIZE
                        );

                        List<String> headers =
                                (List<String>) block.get("headers");

                        ctx = writeText(
                                pdf,
                                ctx,
                                String.join(" | ", headers),
                                new PDType1Font(
                                        Standard14Fonts.FontName.COURIER_BOLD
                                ),
                                NORMAL_FONT_SIZE
                        );

                        List<List<String>> rows =
                                (List<List<String>>) block.get("rows");

                        for (List<String> row : rows) {

                            ctx = writeText(
                                    pdf,
                                    ctx,
                                    String.join(" | ", row),
                                    new PDType1Font(
                                            Standard14Fonts.FontName.COURIER
                                    ),
                                    NORMAL_FONT_SIZE
                            );
                        }

                        ctx.y -= 8;
                    }

                    case "image" -> {

                        String imageText =
                                "[IMAGE] "
                                        + block.get("name");

                        ctx = writeText(
                                pdf,
                                ctx,
                                imageText,
                                new PDType1Font(
                                        Standard14Fonts.FontName.HELVETICA_OBLIQUE
                                ),
                                NORMAL_FONT_SIZE
                        );
                    }
                }
            }

            closePage(ctx);

            pdf.save(outputFile);
        }
    }

    private static PageContext createPage(
            PDDocument pdf
    ) throws IOException {

        PDPage page = new PDPage();

        pdf.addPage(page);

        PDPageContentStream content =
                new PDPageContentStream(pdf, page);

        content.beginText();

        content.newLineAtOffset(
                PAGE_MARGIN,
                PAGE_TOP
        );

        return new PageContext(
                page,
                content,
                PAGE_TOP
        );
    }

    private static void closePage(
            PageContext ctx
    ) throws IOException {

        ctx.content.endText();
        ctx.content.close();
    }

    private static PageContext writeText(
            PDDocument pdf,
            PageContext ctx,
            String text,
            PDType1Font font,
            float fontSize
    ) throws Exception {

        if (text == null || text.isBlank()) {
            return ctx;
        }

        text = text
                .replace("\r", " ")
                .replace("\n", " ");

        List<String> lines =
                wrapText(
                        text,
                        font,
                        fontSize,
                        CONTENT_WIDTH
                );

        for (String line : lines) {

            if (ctx.y <= PAGE_BOTTOM) {

                closePage(ctx);

                ctx = createPage(pdf);
            }

            ctx.content.setFont(
                    font,
                    fontSize
            );

            ctx.content.showText(line);

            float lineHeight =
                    fontSize + 3;

            ctx.content.newLineAtOffset(
                    0,
                    -lineHeight
            );

            ctx.y -= lineHeight;
        }

        return ctx;
    }

    /**
     * Assumptions:
     *
     * 1. No truncation allowed.
     * 2. Preserve all text.
     * 3. Optimize for chunking quality.
     * 4. Use actual rendered width instead
     *    of character count.
     */
    private static List<String> wrapText(
            String text,
            PDType1Font font,
            float fontSize,
            float maxWidth
    ) throws IOException {

        List<String> lines =
                new ArrayList<>();

        String[] words =
                text.split("\\s+");

        StringBuilder currentLine =
                new StringBuilder();

        for (String word : words) {

            String candidate =
                    currentLine.isEmpty()
                            ? word
                            : currentLine + " " + word;

            float width =
                    font.getStringWidth(candidate)
                            / 1000f
                            * fontSize;

            if (width > maxWidth) {

                if (!currentLine.isEmpty()) {

                    lines.add(
                            currentLine.toString()
                    );
                }

                currentLine =
                        new StringBuilder(word);
            }
            else {

                currentLine =
                        new StringBuilder(candidate);
            }
        }

        if (!currentLine.isEmpty()) {

            lines.add(
                    currentLine.toString()
            );
        }

        return lines;
    }

    private static class PageContext {

        PDPage page;

        PDPageContentStream content;

        float y;

        PageContext(
                PDPage page,
                PDPageContentStream content,
                float y
        ) {

            this.page = page;
            this.content = content;
            this.y = y;
        }
    }
}