package com.brian.file_sharing_app.controller;
import com.brian.file_sharing_app.service.FileService;
import com.brian.file_sharing_app.model.FileEntity;
import com.brian.file_sharing_app.model.SharedBundle;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/* API endpoints for creating shared bundles, uploading files and downloading*/
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "https://snapshare-frontend.onrender.com")
public class FileController{

    @Autowired
    private FileService fileService;

    // Create shared bundle and get OTP
    @PostMapping("/create_bundle")
    public ResponseEntity<Map<String, String>> createBundle() {
        try {
            String otp = fileService.createSharedBundle();
            Map<String, String> response = new HashMap<>();
            response.put("otp", otp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Uploading file under chunk threshold,
    @PostMapping("/upload_single_file/{otp}")
    public ResponseEntity<Map<String, String>> uploadSingleFile(@PathVariable String otp, @RequestParam("file") MultipartFile file) {
        try {
            fileService.uploadSingleFile(otp, file);
            return ResponseEntity.ok(Map.of("message", "File uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Uploading file having large chunks; uses chunking algorithm
    @PostMapping("/chunked_upload/{otp}")
    public ResponseEntity<Map<String, String>> uploadChunk(@PathVariable String otp,
                                                           @RequestParam("chunk") MultipartFile chunk,
                                                           @RequestParam("fileName") String fileName,
                                                           @RequestParam("chunkIndex") int chunkIndex,
                                                           @RequestParam("totalChunks") int totalChunks,
                                                           @RequestParam("uploadId") String uploadId) {
        try {
            fileService.uploadChunk(otp, chunk, fileName, chunkIndex, totalChunks, uploadId);
            return ResponseEntity.ok(Map.of("message", "Chunk uploaded successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Finalize chunked upload
    @PostMapping("/finalize_upload/{otp}")
    public ResponseEntity<Map<String, String>> finalizeUpload(@PathVariable String otp, @RequestBody Map<String, String> payload) {
        try {
            String uploadId = payload.get("uploadId");
            fileService.finalizeUpload(otp, uploadId);
            return ResponseEntity.ok(Map.of("message", "Upload finalized successfully"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Downloading shared file or files
    @GetMapping("/download/{otp}")
    public ResponseEntity<StreamingResponseBody> downloadFiles(@PathVariable String otp){
        HttpHeaders headers = new HttpHeaders();
        try{
            StreamingResponseBody responseBody = outputStream -> {
                try {
                    fileService.streamZipArchive(otp, outputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Error streaming zip archive", e);
                }
            };

            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename("files_" + otp + ".zip").build());

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch(Exception e){
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>(outputStream -> outputStream.write(("Error: " + e.getMessage()).getBytes()), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}