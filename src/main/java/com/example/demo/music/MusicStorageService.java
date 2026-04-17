package com.example.demo.music;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MusicStorageService {

    private final Path baseDir;

    public MusicStorageService(@Value("${zentrix.music.storage-dir:src/main/resources/static/uploads/music}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Empty file");
        }

        Files.createDirectories(baseDir);

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "track" : file.getOriginalFilename());
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot).toLowerCase();
        }

        String safeName = UUID.randomUUID() + ext;
        Path target = baseDir.resolve(safeName).normalize();
        file.transferTo(target.toFile());

        return new StoredFile(target.toString(), file.getContentType() == null ? "application/octet-stream" : file.getContentType(), file.getSize());
    }

    public record StoredFile(String absolutePath, String contentType, long sizeBytes) {}
}

