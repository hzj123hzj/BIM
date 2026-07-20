import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * 轻量 .xlsx 读取器（仅用 JDK 自带 API，无需 Apache POI）。
 * .xlsx 本质是 ZIP 包：xl/sharedStrings.xml 存共享字符串，xl/worksheets/sheet1.xml 存单元格。
 * 只读取第一个工作表（sheet1），返回 List<String[]>，每行一个字符串数组。
 */
public class ExcelReader {

    /** 读取 .xlsx 第一个工作表的所有行 */
    public static List<String[]> readFirstSheet(File file) throws Exception {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = zis.read(buf)) > 0) baos.write(buf, 0, n);
                parts.put(e.getName(), baos.toByteArray());
            }
        }
        List<String> shared = parseSharedStrings(parts.get("xl/sharedStrings.xml"));
        return parseSheet(parts.get("xl/worksheets/sheet1.xml"), shared);
    }

    private static List<String> parseSharedStrings(byte[] data) throws Exception {
        List<String> list = new ArrayList<>();
        if (data == null) return list;
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(data));
        NodeList siList = doc.getElementsByTagName("si");
        for (int i = 0; i < siList.getLength(); i++) {
            list.add(collectText(siList.item(i)));
        }
        return list;
    }

    /** 递归收集节点下所有 <t> 文本（兼容富文本 <r><t>...</t></r>） */
    private static String collectText(Node node) {
        StringBuilder sb = new StringBuilder();
        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(node.getNodeValue());
        } else {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                sb.append(collectText(children.item(i)));
            }
        }
        return sb.toString();
    }

    private static List<String[]> parseSheet(byte[] data, List<String> shared) throws Exception {
        List<String[]> rows = new ArrayList<>();
        if (data == null) return rows;
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(data));
        NodeList rowList = doc.getElementsByTagName("row");
        for (int i = 0; i < rowList.getLength(); i++) {
            Element rowEl = (Element) rowList.item(i);
            NodeList cellList = rowEl.getElementsByTagName("c");
            int maxCol = -1;
            String[] raw = new String[0];
            for (int j = 0; j < cellList.getLength(); j++) {
                Element cell = (Element) cellList.item(j);
                int col = columnIndex(cell.getAttribute("r"));
                if (col > maxCol) {
                    String[] bigger = new String[col + 1];
                    System.arraycopy(raw, 0, bigger, 0, raw.length);
                    raw = bigger;
                    maxCol = col;
                }
                raw[col] = cellValue(cell, shared);
            }
            // 去掉末尾的空列
            int last = raw.length - 1;
            while (last >= 0 && (raw[last] == null || raw[last].isEmpty())) last--;
            if (last < 0) continue; // 整行空，跳过
            String[] trimmed = new String[last + 1];
            System.arraycopy(raw, 0, trimmed, 0, last + 1);
            rows.add(trimmed);
        }
        return rows;
    }

    private static String cellValue(Element cell, List<String> shared) {
        String type = cell.getAttribute("t");
        if ("s".equals(type)) {
            NodeList v = cell.getElementsByTagName("v");
            if (v.getLength() > 0) {
                try {
                    int idx = Integer.parseInt(v.item(0).getTextContent().trim());
                    return idx < shared.size() ? shared.get(idx) : "";
                } catch (Exception ex) { return ""; }
            }
            return "";
        }
        if ("inlineStr".equals(type)) {
            NodeList t = cell.getElementsByTagName("t");
            if (t.getLength() > 0) return t.item(0).getTextContent();
            return "";
        }
        // 默认：数字 / 布尔 / 公式结果
        NodeList v = cell.getElementsByTagName("v");
        if (v.getLength() > 0) return v.item(0).getTextContent().trim();
        return "";
    }

    /** 解析单元格引用（如 "AB3"）得到列索引，A->0 */
    private static int columnIndex(String ref) {
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                col = col * 26 + (c - 'A' + 1);
            }
        }
        return col - 1;
    }
}
