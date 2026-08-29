package com.clothing.ai.common.util;

import com.clothing.ai.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private final AppProperties props;

    public String save(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String ext = guessExtension(file.getOriginalFilename());
        String name = UUID.randomUUID() + ext;
        Path target = Paths.get(props.getUpload().getBaseDir(), folder, name);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return props.getUpload().getPublicUrl() + "/uploads/" + folder + "/" + name;
    }

    private String guessExtension(String filename) {
        if (filename == null) return ".bin";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : ".bin";
    }
}
