package iuh.fit.se.shared.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import iuh.fit.se.shared.exception.DomainException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String menuFolder;
    private final String tableQrFolder;
    private final long maxFileSizeBytes;

    public CloudinaryImageStorageService(
            Cloudinary cloudinary,
            @Value("${app.storage.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.storage.cloudinary.api-key:}") String apiKey,
            @Value("${app.storage.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.storage.cloudinary.menu-folder:lumiere/menu-items}") String menuFolder,
            @Value("${app.storage.cloudinary.table-qr-folder:lumiere/table-qr}") String tableQrFolder,
            @Value("${app.storage.cloudinary.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.menuFolder = normalizeFolder(menuFolder);
        this.tableQrFolder = normalizeFolder(tableQrFolder);
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public StoredImage uploadMenuItemImage(Long menuItemId, MultipartFile file) {
        if (menuItemId == null || menuItemId <= 0) {
            throw new DomainException("Menu item id is invalid");
        }
        if (file == null || file.isEmpty()) {
            throw new DomainException("Image file is required");
        }
        validateAllowedImageExtension(file);

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.toLowerCase(Locale.ROOT).startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            throw new DomainException("Only image files are supported");
        }

        ensureConfigured();
        ensureWithinSize(file.getSize());

        String fileName = "menu-item-" + menuItemId;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", menuFolder,  // Truyền folder riêng
                            "public_id", fileName, // Truyền tên file riêng
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true
                    )
            );
            return toStoredImage(result);
        } catch (IOException ex) {
            throw new DomainException("Failed to upload menu image", ex);
        }
    }

    @Override
    public StoredImage uploadTableQrImage(String tableCode, byte[] imageBytes) {
        if (tableCode == null || tableCode.isBlank()) {
            throw new DomainException("Table code is required to upload QR image");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new DomainException("QR image bytes are empty");
        }

        ensureConfigured();
        ensureWithinSize(imageBytes.length);

        String safeTableCode = tableCode.trim().replaceAll("[^a-zA-Z0-9-]", "-");
        String fileName = "table-" + safeTableCode; // Chỉ để tên file, không kèm path
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap(
                            "folder", tableQrFolder, // Cloudinary sẽ tạo folder này nếu chưa có
                            "public_id", fileName,   // Tên định danh trong folder đó
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true,
                            "format", "png"
                    )
            );
            return toStoredImage(result);
        } catch (IOException ex) {
            throw new DomainException("Failed to upload table QR image", ex);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        ensureConfigured();
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "invalidate", true
                    )
            );
        } catch (IOException ex) {
            throw new DomainException("Failed to delete image from storage", ex);
        }
    }

    private StoredImage toStoredImage(Map<?, ?> result) {
        Object urlObj = result.get("secure_url");
        if (urlObj == null) {
            urlObj = result.get("url");
        }
        Object publicIdObj = result.get("public_id");

        String url = urlObj == null ? null : urlObj.toString();
        String publicId = publicIdObj == null ? null : publicIdObj.toString();

        if (url == null || url.isBlank() || publicId == null || publicId.isBlank()) {
            throw new DomainException("Image upload completed but storage metadata is missing");
        }

        return new StoredImage(url, publicId);
    }

    private void ensureConfigured() {
        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new DomainException("Cloudinary credentials are not configured");
        }
    }

    private void ensureWithinSize(long fileSizeBytes) {
        if (fileSizeBytes <= 0) {
            throw new DomainException("Image payload is empty");
        }
        if (fileSizeBytes > maxFileSizeBytes) {
            throw new DomainException("Image file exceeds max allowed size");
        }
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new DomainException("Cloudinary folder configuration is required");
        }
        return folder.trim().replaceAll("/+$", "");
    }

    private void validateAllowedImageExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new DomainException("Image file name is required");
        }

        String normalizedFileName = originalFilename.trim().replace('\\', '/');
        int slashIndex = normalizedFileName.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalizedFileName = normalizedFileName.substring(slashIndex + 1);
        }

        int dotIndex = normalizedFileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == normalizedFileName.length() - 1) {
            throw new DomainException("Only image files with extensions .png, .jpg, .jpeg are supported");
        }

        String extension = normalizedFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new DomainException("Only image files with extensions .png, .jpg, .jpeg are supported");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
