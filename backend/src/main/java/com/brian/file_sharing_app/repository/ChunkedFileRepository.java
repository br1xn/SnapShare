package com.brian.file_sharing_app.repository;

import com.brian.file_sharing_app.model.ChunkedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChunkedFileRepository extends JpaRepository<ChunkedFile, Long> {
    List<ChunkedFile> findByUploadIdOrderByChunkIndexAsc(String uploadId);
}