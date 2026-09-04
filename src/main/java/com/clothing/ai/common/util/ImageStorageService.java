package com.clothing.ai.common.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.clothing.ai.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

/**
 * Unified image storage service.
 *
 * <p>Strategy selection (automatic, no code change needed):
 * <ol>
 *   <li><b>Cloudinary</b> — when {@code CLOUDINARY_CLOUD_NAME}, {@code CLOUDINARY_API_KEY}, and
 *       {@code CLOUDINARY_API_SECRET} are all set. Images survive Render restarts and are served
 *       from Cloudinary's CDN.
 *   <li><b>Local disk</b> — fallback when any Cloudinary env var is missing. Files are written to
 *       {@code app.upload.base-dir}. These are ephemeral on Render's free tier.
 * </ol>
 *
 * <p>The {@code folder} parameter becomes the Cloudinary folder or the local sub-directory,
 * e.g. {@code "banners"} or {@code "products"}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageStorageService {

    private final AppProperties props;

    @Value("${CLOUDINARY_CLOUD_NAME:}")
    private String cloudName;

    @Value("${CLOUDINARY_API_KEY:}")
    private String apiKey;

    @Value("${CLOUDINARY_API_SECRET:}")
    private String apiSecret;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Save an image and return its publicly accessible URL.
     *
     * @param file   the uploaded file (must not be empty)
     * @param folder logical folder / sub-directory (e.g. "banners", "products")
     * @return a public URL (Cloudinary CDN or local server URL)
     */
    public String save(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (isCloudinaryConfigured()) {
            return uploadToCloudinary(file, folder);
        }
        return saveLocally(file, folder);
    }

    /** Returns true when all three Cloudinary env vars are present. */
    public boolean isCloudinaryConfigured() {
        return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
    }

    // ── Cloudinary ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String uploadToCloudinary(MultipartFile file, String folder) throws IOException {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));

        Map<String, Object> params = ObjectUtils.asMap(
                "folder",         "astrimi/" + folder,
                "use_filename",   true,
                "unique_filename", true,
                "overwrite",      false,
                "resource_type",  "image"
        );

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
        String url = (String) result.get("secure_url");
        log.info("Cloudinary upload OK folder={} url={}", folder, url);
        return url;
    }

    // ── Local disk ────────────────────────────────────────────────────────────

    private String saveLocally(MultipartFile file, String folder) throws IOException {
        String ext  = guessExtension(file.getOriginalFilename());
        String name = UUID.randomUUID() + ext;
        Path   target = Paths.get(props.getUpload().getBaseDir(), folder, name);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        String url = props.getUpload().getPublicUrl() + "/uploads/" + folder + "/" + name;
        log.info("Local upload OK folder={} path={}", folder, target);
        return url;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String guessExtension(String filename) {
        if (filename == null) return ".bin";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot).toLowerCase() : ".bin";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
