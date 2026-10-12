package com.example.recruitment.resume.util;

import com.example.recruitment.common.exception.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 简历文件文本提取工具：PDF / Word(docx) / 纯文本
 */
public final class DocumentTextExtractor {

    private DocumentTextExtractor() {
    }

    public static String extract(Path path, String ext) {
        try {
            switch (ext.toLowerCase()) {
                case "pdf" -> {
                    try (PDDocument document = PDDocument.load(path.toFile())) {
                        return new PDFTextStripper().getText(document);
                    }
                }
                case "docx" -> {
                    try (InputStream in = Files.newInputStream(path);
                         XWPFDocument doc = new XWPFDocument(in)) {
                        StringBuilder sb = new StringBuilder();
                        for (XWPFParagraph p : doc.getParagraphs()) {
                            appendLine(sb, p.getText());
                        }
                        for (XWPFTable table : doc.getTables()) {
                            for (XWPFTableRow row : table.getRows()) {
                                for (XWPFTableCell cell : row.getTableCells()) {
                                    appendLine(sb, cell.getText());
                                }
                            }
                        }
                        return sb.toString();
                    }
                }
                case "doc" -> throw BusinessException.badRequest(
                        "暂不支持旧版 .doc 文件，请另存为 .docx 后再上传");
                case "txt" -> {
                    try {
                        return Files.readString(path, StandardCharsets.UTF_8);
                    } catch (CharacterCodingException e) {
                        byte[] bytes = Files.readAllBytes(path);
                        return new String(bytes, StandardCharsets.ISO_8859_1);
                    }
                }
                default -> throw BusinessException.badRequest("不支持的文件类型: " + ext);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.badRequest("简历文件解析失败，请确认文件未损坏");
        }
    }

    private static void appendLine(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            sb.append(text.trim()).append('\n');
        }
    }
}
