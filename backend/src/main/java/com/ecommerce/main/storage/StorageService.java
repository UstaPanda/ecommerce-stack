package com.ecommerce.main.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage soyutlama katmanı.
 * Cloud'a geçince sadece bu interface'in yeni bir implementasyonunu yazman yeterli.
 * Controller, Service, Entity'lere dokunman gerekmez.
 */
public interface StorageService {

    /**
     * Dosyayı yükler ve erişilebilir URL döner.
     *
     * @param file   yüklenecek dosya
     * @param folder klasör adı (örn. "products", "stores")
     * @return public URL (örn. "http://localhost:8080/uploads/products/abc.jpg")
     */
    String upload(MultipartFile file, String folder);

    /**
     * URL'ye karşılık gelen dosyayı siler.
     *
     * @param url daha önce upload() ile dönen URL
     */
    void delete(String url);
}
