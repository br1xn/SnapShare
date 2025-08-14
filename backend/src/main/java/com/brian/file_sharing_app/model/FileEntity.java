package com.brian.file_sharing_app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_bundle_id", nullable = false)
    private SharedBundle sharedBundle;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    // Constructors, Getters and Setters
    public FileEntity(){}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public SharedBundle getSharedBundle() { return sharedBundle; }
    public void setSharedBundle(SharedBundle sharedBundle) { this.sharedBundle = sharedBundle; }

}