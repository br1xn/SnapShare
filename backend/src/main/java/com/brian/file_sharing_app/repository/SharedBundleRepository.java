package com.brian.file_sharing_app.repository;

import com.brian.file_sharing_app.model.SharedBundle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedBundleRepository extends JpaRepository<SharedBundle, Long> {
    Optional<SharedBundle> findByOtp(String otp);
}