package com.brian.file_sharing_app.service;

import com.brian.file_sharing_app.model.ChunkedFile;
import com.brian.file_sharing_app.model.FileEntity;
import com.brian.file_sharing_app.model.SharedBundle;
import com.brian.file_sharing_app.repository.ChunkedFileRepository;
import com.brian.file_sharing_app.repository.FileRepository;
import com.brian.file_sharing_app.repository.SharedBundleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

@Service
public class FileService {

    // File paths
    @Value("${UPLOAD_DIR}")
    private String uploadDir;
    @Value("${CHUNK_DIR}")
    private String chunkDir;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SharedBundleRepository sharedBundleRepository;

    @Autowired
    private ChunkedFileRepository chunkedFileRepository;

    // Creates the initial shared bundle and returns the OTP
    @Transactional
    public String createSharedBundle() {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        SharedBundle sharedBundle = new SharedBundle();
        sharedBundle.setOtp(otp);
        sharedBundle.setExpiryTime(expiryTime);
        sharedBundleRepository.save(sharedBundle);
        return otp;
    }

    // For small files (under 100MB)
    @Transactional
    public void uploadSingleFile(String otp, MultipartFile file) throws IOException {
        SharedBundle sharedBundle = sharedBundleRepository.findByOtp(otp).orElse(null);
        if (sharedBundle == null) {
            throw new IOException("Invalid OTP: Shared bundle not found.");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(uniqueFileName);
        file.transferTo(filePath.toFile());

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(originalFileName);
        fileEntity.setFilePath(filePath.toString());
        fileEntity.setSharedBundle(sharedBundle);
        fileRepository.save(fileEntity);
    }

    // For Large Files (>100MB)
    public void uploadChunk(String otp, MultipartFile chunk, String fileName, int chunkIndex, int totalChunks, String uploadId) throws IOException {
        SharedBundle sharedBundle = sharedBundleRepository.findByOtp(otp).orElse(null);
        if (sharedBundle == null) {
            throw new IOException("Invalid OTP: Shared bundle not found.");
        }

        Path chunkPath = Paths.get(chunkDir, uploadId);
        if (!Files.exists(chunkPath)) {
            Files.createDirectories(chunkPath);
        }

        Path fileChunkPath = chunkPath.resolve(chunkIndex + "_" + fileName);
        chunk.transferTo(fileChunkPath.toFile());

        ChunkedFile chunkedFile = new ChunkedFile();
        chunkedFile.setUploadId(uploadId);
        chunkedFile.setChunkIndex(chunkIndex);
        chunkedFile.setTotalChunks(totalChunks);
        chunkedFile.setFileName(fileName);
        chunkedFile.setFilePath(fileChunkPath.toString());
        chunkedFileRepository.save(chunkedFile);
    }

    @Transactional
    public void finalizeUpload(String otp, String uploadId) throws IOException {
        SharedBundle sharedBundle = sharedBundleRepository.findByOtp(otp).orElse(null);
        if (sharedBundle == null) {
            throw new IOException("Invalid OTP: Shared bundle not found.");
        }

        List<ChunkedFile> chunks = chunkedFileRepository.findByUploadIdOrderByChunkIndexAsc(uploadId);
        if (chunks.isEmpty() || chunks.size() != chunks.get(0).getTotalChunks()) {
            throw new IOException("Missing file chunks.");
        }

        Path finalFilePath = Paths.get(uploadDir, UUID.randomUUID().toString() + "_" + chunks.get(0).getFileName());
        try (var finalFileStream = Files.newOutputStream(finalFilePath)) {
            for (ChunkedFile chunk : chunks) {
                Path chunkPath = Paths.get(chunk.getFilePath());
                Files.copy(chunkPath, finalFileStream);
                Files.delete(chunkPath);
            }
        }

        Files.deleteIfExists(Paths.get(chunkDir, uploadId));

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(chunks.get(0).getFileName());
        fileEntity.setFilePath(finalFilePath.toString());
        fileEntity.setSharedBundle(sharedBundle);
        fileRepository.save(fileEntity);

        chunkedFileRepository.deleteAll(chunks);
    }

    // Creating the zip file
    @Transactional
    public void streamZipArchive(String otp, OutputStream outputStream) throws IOException {
        SharedBundle sharedBundle = sharedBundleRepository.findByOtp(otp).filter(bundle -> bundle.getExpiryTime().isAfter(LocalDateTime.now()))
                .orElse(null);

        if (sharedBundle == null) {
            throw new IOException("Invalid or expired OTP");
        }

        List<FileEntity> files = fileRepository.findBySharedBundle(sharedBundle);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (FileEntity fileEntity : files) {
                Path filePath = Paths.get(fileEntity.getFilePath());
                if (Files.exists(filePath)) {
                    ZipEntry entry = new ZipEntry(fileEntity.getFileName());
                    zos.putNextEntry(entry);
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            }
        }

        for (FileEntity fileEntity : files) {
            Files.deleteIfExists(Paths.get(fileEntity.getFilePath()));
            fileRepository.delete(fileEntity);
        }

        sharedBundleRepository.delete(sharedBundle);
    }
}
