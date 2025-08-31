package com.luckytapu.fusioncore.entity;
import com.luckytapu.fusioncore.model.FileStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID fileId;

    private String fileName;
    private String fileType;

    @Column(columnDefinition = "BYTEA")
    private byte[] data;

    @Column(columnDefinition = "BYTEA")
    private byte[] vectorData;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;
}

