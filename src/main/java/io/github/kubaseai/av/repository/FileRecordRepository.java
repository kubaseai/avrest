package io.github.kubaseai.av.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import io.github.kubaseai.av.model.FileRecord;

@RepositoryRestResource(exported = false)
public interface FileRecordRepository extends PagingAndSortingRepository<FileRecord, String> {
	List<FileRecord> findBySha512(String hash);
}

