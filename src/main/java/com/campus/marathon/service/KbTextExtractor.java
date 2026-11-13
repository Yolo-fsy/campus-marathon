package com.campus.marathon.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 文本提取：txt / md 直接按 UTF-8 读取，pdf 用 PDFBox 解析，统一做空白归一化。
 * 扫描版 PDF（纯图片）无法提取文本，由调用方提示需要 OCR。
 */
public class KbTextExtractor {

    public String extract(String fileName, InputStream input) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            throw new IOException("缺少文件名");
        }
        if (input == null) {
            throw new IOException("文件内容为空");
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        String raw;
        if (lower.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(input)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                raw = stripper.getText(document);
            }
        } else if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            raw = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } else {
            throw new IOException("仅支持 .txt / .md / .pdf 文件");
        }
        return Chunker.normalize(raw);
    }
}
