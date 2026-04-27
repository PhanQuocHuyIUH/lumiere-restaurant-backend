package iuh.fit.se.shared.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredImage uploadMenuItemImage(Long menuItemId, MultipartFile file);

    StoredImage uploadTableQrImage(String tableCode, byte[] imageBytes);

    void deleteImage(String publicId);
}
