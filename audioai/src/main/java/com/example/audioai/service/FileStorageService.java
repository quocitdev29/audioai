package com.example.audioai.service;

import com.example.audioai.exception.ApiException;
import com.example.audioai.util.StoragePathResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Luu file media (anh bia truyen) vao thu muc Data/&lt;TenTruyen&gt;/&lt;TenTruyen&gt;.&lt;ext&gt;
 * va tra ve duong dan web dang {@code /Data/&lt;TenTruyen&gt;/&lt;TenTruyen&gt;.&lt;ext&gt;}
 * de luu vao DB (Story.coverUrl).
 * <p>
 * Tren Windows, file dich co the dang bi process khac giu (Windows Defender,
 * trinh duyet/vite dang phuc vu anh cu...) nen khong ghi de duoc. Khi do se luu
 * bang ten duy nhat (them timestamp) va tra ve duong dan do - dam bao luon luu
 * duoc anh va hien thi duoc, khong bao loi "being used by another process".
 */
@Service
@Slf4j
public class FileStorageService {

    /** Thu muc goc chua du lieu truyen (cau hinh trong application.properties). */
    @Value("${app.data-dir:../audio-story-frontend/public/Data}")
    private String dataDir;

    /**
     * Luu anh bia cua truyen.
     *
     * @param file  file anh upload
     * @param title tieu de truyen (dung de dat ten thu muc va ten file)
     * @return duong dan web tuong doi, vi du {@code /Data/LoiNguyenHuyetNgai/LoiNguyenHuyetNgai.png}
     */
    public String saveStoryCover(MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh bìa");
        }

        String slug = toSlug(title);
        if (slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Tiêu đề truyện không hợp lệ để đặt tên ảnh. Vui lòng nhập tiêu đề trước khi tải ảnh");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không đọc được file ảnh: " + e.getMessage());
        }

        String ext = resolveExtension(file.getOriginalFilename(), "png");
        Path baseDir = StoragePathResolver.resolve(dataDir);
        Path storyDir = baseDir.resolve(slug);

        try {
            Files.createDirectories(storyDir);
        } catch (IOException e) {
            log.error("Không tạo được thư mục {}: {}", storyDir, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tạo được thư mục lưu ảnh: " + e.getMessage());
        }

        // 1) Ưu tiên tên chuẩn: <slug>.<ext>  ->  /Data/<slug>/<slug>.<ext>
        String standardFile = slug + "." + ext;
        if (writeWithRetry(storyDir, standardFile, data, 6)) {
            log.info("Đã lưu ảnh bìa: {}", storyDir.resolve(standardFile));
            return "/Data/" + slug + "/" + standardFile;
        }

        // 2) File đích đang bị process khác giữ (sharing violation) trên Windows.
        //    Dùng tên duy nhất (chưa tồn tại -> không thể bị khóa) để đảm bảo lưu được.
        String uniqueFile = slug + "_" + System.currentTimeMillis() + "." + ext;
        if (writeWithRetry(storyDir, uniqueFile, data, 3)) {
            log.warn("File '{}' đang bị khóa, đã lưu ảnh với tên duy nhất: {}", standardFile, uniqueFile);
            return "/Data/" + slug + "/" + uniqueFile;
        }

        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể lưu ảnh bìa, vui lòng thử lại");
    }

    /**
     * Luu file audio cua chuong vao {@code Data/<TenTruyen>/<ten-file-goc>.<ext>}
     * va tra ve duong dan web {@code /Data/<TenTruyen>/<ten-file>.<ext>} de luu vao
     * {@code Chapter.audioUrl}.
     * <p>
     * Ten file giu theo ten goc (chi bo duong dan va ky tu khong hop le), neu file
     * dich dang bi process khac giu (Windows) thi tu dong them timestamp.
     *
     * @param file       file audio upload
     * @param storyTitle tieu de truyen (dung de dat ten thu muc)
     * @return duong dan web tuong doi, vi du {@code /Data/LoiNguyenHuyetNgai/Chuong 1.wav}
     */
    public String saveStoryAudio(MultipartFile file, String storyTitle) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn file audio");
        }

        String slug = toSlug(storyTitle);
        if (slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Truyện không hợp lệ để đặt tên file audio");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không đọc được file audio: " + e.getMessage());
        }

        String ext = resolveExtension(file.getOriginalFilename(), "mp3");
        String base = sanitizeFileName(file.getOriginalFilename());
        if (base.isBlank()) {
            base = slug;
        }

        Path storyDir = StoragePathResolver.resolve(dataDir).resolve(slug);
        try {
            Files.createDirectories(storyDir);
        } catch (IOException e) {
            log.error("Không tạo được thư mục {}: {}", storyDir, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tạo được thư mục lưu audio: " + e.getMessage());
        }

        // 1) Ưu tiên tên file gốc: /Data/<slug>/<base>.<ext>
        String fileName = base + "." + ext;
        if (writeWithRetry(storyDir, fileName, data, 6)) {
            log.info("Đã lưu audio chương: {}", storyDir.resolve(fileName));
            return "/Data/" + slug + "/" + fileName;
        }

        // 2) File đích đang bị process khác giữ -> dùng tên duy nhất
        String uniqueFile = base + "_" + System.currentTimeMillis() + "." + ext;
        if (writeWithRetry(storyDir, uniqueFile, data, 3)) {
            log.warn("File '{}' đang bị khóa, đã lưu audio với tên duy nhất: {}", fileName, uniqueFile);
            return "/Data/" + slug + "/" + uniqueFile;
        }

        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể lưu file audio, vui lòng thử lại");
    }

    /**
     * Ghi file an toàn với vài lần thử lại: ưu tiên ghi ra file tạm rồi thay thế,
     * nếu bị chặn thì ghi trực tiếp (truncate). Trả về {@code true} nếu thành công.
     */
    private boolean writeWithRetry(Path dir, String fileName, byte[] data, int attempts) {
        Path target = dir.resolve(fileName);
        for (int i = 1; i <= attempts; i++) {
            // Cách 1: ghi ra file tạm rồi move thay thế (an toàn, không ghi dở dang)
            Path tmp = dir.resolve(fileName + ".tmp");
            try {
                Files.write(tmp, data);
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException e) {
                deleteQuietly(tmp);
                log.warn("Ghi '{}' (temp->move) thất bại lần {}/{}: {}", fileName, i, attempts, e.getMessage());
            }

            // Cách 2: ghi trực tiếp (truncate) - đôi khi mở để ghi được dù move bị chặn
            try {
                Files.write(target, data,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
                return true;
            } catch (IOException e) {
                log.warn("Ghi trực tiếp '{}' thất bại lần {}/{}: {}", fileName, i, attempts, e.getMessage());
            }

            sleepQuietly(200);
        }
        return false;
    }

    private void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // bo qua - file tam se duoc don dep sau
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Chuyen tieu de tieng Viet thanh ten an toan cho thu muc/file.
     * Vi du: "Lỗi Nguyện Huyết Ngãi" -&gt; "LoiNguyenHuyetNgai".
     */
    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        StringBuilder sb = new StringBuilder();
        for (char c : normalized.toCharArray()) {
            if (c == 'đ' || c == 'Đ') {
                sb.append('d');
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
            // bo qua khoang trang va ky tu dac biet
        }
        return sb.toString();
    }

    /** Lay phan mo rong cua file goc; neu khong hop le thi dung {@code defaultExt}. */
    private String resolveExtension(String originalName, String defaultExt) {
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                String ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("[a-z0-9]{1,5}")) {
                    return ext;
                }
            }
        }
        return defaultExt;
    }

    /**
     * Lam sach ten file goc: bo duong dan, bo phan mo rong va thay cac ky tu
     * khong hop le tren Windows bang '_'. Giu lai khoang trang va chu tieng Viet.
     * Vi du: "C:\\audio\\Chuong 1.wav" -&gt; "Chuong 1".
     */
    private String sanitizeFileName(String originalName) {
        if (originalName == null) {
            return "";
        }
        String name = originalName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
    }
}