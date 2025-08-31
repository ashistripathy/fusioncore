package com.luckytapu.fusioncore.repository;

import com.luckytapu.fusioncore.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {
}
