package io.github.kubaseai.av.service.impl;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.kubaseai.av.model.FileRecord;
import io.github.kubaseai.av.model.FileRecord.FileRecordType;

public class ScanStrategyRealTime extends ScanStrategy {
	
	private final static Logger logger = LoggerFactory.getLogger(ScanStrategyRealTime.class);

	private final static String EICAR_PART_1 = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-";
	private final static String EICAR_PART_2 = "ANTIVIRUS-TEST-FILE!$H+H*";

	private FileRecord writeAntiVirusTestFile(File f) {
		try (RandomAccessFile out = new RandomAccessFile(f, "rw")) {
			FileChannel ch = out.getChannel();
			byte[] buff = (EICAR_PART_1 + EICAR_PART_2).getBytes();
			out.write(buff);
			ch.force(true);
		}
		catch (Exception e) {}
		FileRecord fr = new FileRecord();
		fr.setLocalFile(f);
		return fr;
	}

	@Override
	protected void scan(FileRecord file) {
		File testFile = new File(file.getLocalFile().getParent(), "av-alive-test-"+file.getId()+".com");
		FileRecord fileRecord = writeAntiVirusTestFile(testFile);
		_scan(fileRecord);
		if (FileRecordType.clean.equals(fileRecord.getStatus())) {
			logger.error("AV didn't clean test file");
			file.setStatus(FileRecordType.failure);
			file.markProcessingEnd();
			return;
		}
		_scan(file);
	}

	protected void _scan(FileRecord file) {
		File f = file.getLocalFile();
		boolean readable = super.canReadFile(f);
		readable = super.canReadFile(f);
		if (readable) {
			file.setStatus(FileRecordType.clean);
			f.delete();
		}
		else {
			file.setStatus(FileRecordType.infected);
		}
		file.setAnalyzedAt(new Date());
		logger.info("File analysis finished: "+file);
	}
}
