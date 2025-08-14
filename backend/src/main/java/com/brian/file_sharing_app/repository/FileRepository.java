package com.brian.file_sharing_app.repository;

import com.brian.file_sharing_app.model.FileEntity;
import com.brian.file_sharing_app.model.SharedBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findBySharedBundle(SharedBundle sharedBundle);
}