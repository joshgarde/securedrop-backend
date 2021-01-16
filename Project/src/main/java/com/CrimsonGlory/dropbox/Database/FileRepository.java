package com.CrimsonGlory.dropbox.Database;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface FileRepository extends CrudRepository<FileInfo, String> {
    Optional<FileInfo> findById(String id);
}
