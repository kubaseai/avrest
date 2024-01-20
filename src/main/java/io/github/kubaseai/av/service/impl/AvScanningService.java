package io.github.kubaseai.av.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.kubaseai.av.config.MainConfiguration;
import io.github.kubaseai.av.model.FileRecord;
import io.github.kubaseai.av.model.FileRecord.FileRecordType;
import io.github.kubaseai.av.repository.FileRecordRepository;

@Service
public class AvScanningService {

	private final FileRecordRepository fileRepo;
	private final ExecutorService executor;
	private ScanStrategy scanner;
	private Path workingDir;
	
	public AvScanningService(FileRecordRepository fileRepo, MainConfiguration cfg) {
		this.fileRepo = fileRepo;
		String avCmd = cfg.getAvScannerCommand();
		this.scanner = (avCmd!=null && !avCmd.isBlank()) ? new ScanStrategyCmdline(avCmd) : new ScanStrategyRealTime();
		this.executor = Executors.newFixedThreadPool(cfg.getThreadPoolSize());
		String wd = cfg.getWorkingDir();
		if (wd==null || wd.isBlank()) {
			wd = "./";
		}
		workingDir = Paths.get(wd);
		scanner.setDeepScan(cfg.getDeepScan());
	}
	
	private final static Logger logger = LoggerFactory.getLogger(AvScanningService.class);
	
	@Transactional(readOnly = true)
	public FileRecord getFile(String id) {
		return id==null || id.isBlank() ? null : fileRepo.findById(id).orElse(null);
	}

	@Transactional(readOnly = true)
	public Page<FileRecord> listFiles(Pageable pageable) {
		return fileRepo.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public List<FileRecord> getFileByHash(String hash) {
		return hash==null || hash.isBlank() ? null : fileRepo.findBySha512(hash);
	}

	public void queueFileForScanning(FileRecord fileInfo) {
		SecurityContext ctx = SecurityContextHolder.getContext();
		Authentication auth = ctx!=null ? ctx.getAuthentication() : null;
		if (fileInfo.getStatus().compareTo(FileRecordType.accepted) == 0) {
			fileRepo.save(fileInfo);
			executor.submit(newTask(fileInfo));
		}
		logger.info("Scanning is queued for "+fileInfo+" from "+auth);
	}

	private static class FileTask implements Runnable {
		public FileTask(FileRecord file, ScanStrategy s, FileRecordRepository repo) {
			this.file = file;
			this.scanner = s;
			this.repo = repo;
		}
		private FileRecord file;
		private ScanStrategy scanner;
		private FileRecordRepository repo;
		@Transactional
		public void run() {
			try {
				logger.debug("Scanning starting for id="+file.getId());
				scanner.scan(file);
			}
			catch (Exception e) {
				logger.error("Scanning failed for "+file, e);
				file.setStatus(FileRecordType.failure);
				file.markProcessingEnd();
			}
			finally {
				repo.save(file);
				logger.debug("Scanning completed for id="+file.getId());
			}
		}
	}
	
	private FileTask newTask(FileRecord fileInfo) {
		return new FileTask(fileInfo, scanner, fileRepo);
	}
	
	public File prepareFilePath(String id, String name) {
		try {
			if (!Files.exists(workingDir)) {
				Files.createDirectories(workingDir);
			}
		}
		catch (Exception probablyConcurrencyException) {
			//ignore
		}
		String ext = "";
		int extPos = name.lastIndexOf(".");
		if (extPos!=-1) {
		        ext = name.substring(extPos);
		}
		return new File(workingDir.toFile(), id+ext);
	}
}
