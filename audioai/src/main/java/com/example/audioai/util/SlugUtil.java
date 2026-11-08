package com.example.audioai.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Tiện ích tạo slug (đường dẫn thân thiện SEO) từ tiêu đề tiếng Việt.
 * Dùng cho URL dạng {@code /story/dai-quan-gia-la-ma-hoang}.
 */
public final class SlugUtil {

    /** Khối dấu tổ hợp sinh ra khi tách chữ tiếng Việt bằng NFD. */
    private static final String COMBINING_MARKS = "\\p{InCombiningDiacriticalMarks}+";

    private SlugUtil() {
    }

    /**
     * Chuyển chuỗi về dạng slug: bỏ dấu tiếng Việt, viết thường, thay mọi ký tự
     * không phải chữ/số bằng dấu gạch ngang và cắt gạch ngang ở hai đầu.
     *
     * @param input chuỗi nguồn (thường là tiêu đề truyện), có thể {@code null}
     * @return slug (ví dụ "Đại Quân Giả Là Ma Hoàng" -&gt; "dai-quan-gia-la-ma-hoang"),
     *         hoặc chuỗi rỗng nếu input rỗng
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // NFD tách chữ + dấu thành 2 ký tự -> xoá dấu; "đ" không bị tách nên xử lý riêng
        String withoutMarks = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll(COMBINING_MARKS, "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT);

        return withoutMarks
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
    }

    /**
     * Chuẩn hoá chuỗi để tìm kiếm không phân biệt dấu và hoa/thường:
     * bỏ dấu tiếng Việt, {@code đ -> d}, viết thường nhưng GIỮ khoảng trắng
     * (khác {@link #toSlug} vì toSlug thay khoảng trắng bằng gạch ngang).
     *
     * @param input chuỗi nguồn, có thể {@code null}
     * @return chuỗi đã bỏ dấu + viết thường, hoặc chuỗi rỗng nếu input rỗng
     */
    public static String normalizeForSearch(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll(COMBINING_MARKS, "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}