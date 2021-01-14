package com.CrimsonGlory.dropbox.Database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Optional;

public interface FileRepository extends CrudRepository<FileInfo, MultipartFile> {
    Optional<File> findByAuthText(MultipartFile authtext);
}
