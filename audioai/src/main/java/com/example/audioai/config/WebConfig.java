package com.example.audioai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.audioai.util.StoragePathResolver;

import java.nio.file.Path;

/**
 * Phuc vu file tinh trong thu muc Data (anh bia, audio...) qua URL {@code /Data/**}.
 * DB luu duong dan dang {@code /Data/<TenTruyen>/<TenTruyen>.png} nen can map
 * {@code /Data/**} -&gt; file:&lt;app.data-dir&gt;/ de trinh duyet hien thi duoc anh.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.data-dir:../../Data}")
    private String dataDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path baseDir = StoragePathResolver.resolve(dataDir);
        String location = baseDir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/Data/**")
                .addResourceLocations(location);
    }
}