package com.example.audioai.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Xac dinh thu muc du lieu (Data) linh hoat theo cau hinh va vi tri chay thuc te.
 * Tranh loi khi working directory khac nhau giua cac cach chay (mvnw, IntelliJ...).
 */
public final class StoragePathResolver {

    private StoragePathResolver() {
    }

    /**
     * @param configuredDir gia tri cau hinh {@code app.data-dir} (tuyet doi hoac tuong doi)
     * @return duong dan tuyet doi toi thu muc Data
     */
    public static Path resolve(String configuredDir) {
        Path configured = Paths.get(configuredDir);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path cwd = Paths.get("").toAbsolutePath();

        // 1) Thu resolve truc tiep tu working directory
        Path direct = cwd.resolve(configured).normalize();
        if (Files.isDirectory(direct)) {
            return direct;
        }

        // 2) Do nguoc len cac cap cha: thu ca duong dan tuong doi lan ten thu muc
        //    (vd: "Data") de khong phu thuoc working directory (mvnw vs IntelliJ...)
        Path leaf = configured.getFileName();
        Path cur = cwd;
        for (int i = 0; i < 6 && cur != null; i++) {
            Path candidate = cur.resolve(configured).normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            if (leaf != null) {
                Path leafCandidate = cur.resolve(leaf).normalize();
                if (Files.isDirectory(leafCandidate)) {
                    return leafCandidate;
                }
            }
            cur = cur.getParent();
        }

        return direct;
    }
}