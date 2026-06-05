import org.apache.poi.xwpf.usermodel.XWPFDocument;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println(
                XWPFDocument.class.getName()
        );

        ObjectMapper mapper = new ObjectMapper();

        System.out.println(
                mapper.writeValueAsString(
                        Map.of("status", "ok")
                )
        );
    }
}