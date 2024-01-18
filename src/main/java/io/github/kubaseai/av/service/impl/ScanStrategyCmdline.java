package io.github.kubaseai.av.service.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.kubaseai.av.model.FileRecord;
import io.github.kubaseai.av.model.FileRecord.FileRecordType;

public class ScanStrategyCmdline extends ScanStrategy {
	
	private String command;
	private final static Logger logger = LoggerFactory.getLogger(ScanStrategyCmdline.class);

	public ScanStrategyCmdline(String avCmd) {
		this.command = avCmd;
	}

	@Override
	protected void scan(FileRecord file) {
		StringBuilder out = new StringBuilder();
		File fPath = file.getLocalFile().toPath().normalize().toFile();
		String cmd = command.replace("$1", fPath.getAbsolutePath());
		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			InputStream stdout = proc.getInputStream();
			InputStream stderr = proc.getErrorStream();
			if (!proc.waitFor(60, TimeUnit.SECONDS)) {
			        proc.destroyForcibly();
			}
			try {
				out.append(new String(stdout.readAllBytes()));
			}
			catch (Exception e) {}
			try {
				out.append(new String(stderr.readAllBytes()));
			}
			catch (Exception e) {}
			logger.debug("Scan command '"+cmd+"' output for "+file+": "+out);
		}
		catch (Exception e) {
			logger.error("Exception while scanning "+file, e);
		}
		boolean readable = super.canReadFile(file.getLocalFile());
		if (readable) {
			file.setStatus(FileRecordType.clean);
			file.getLocalFile().delete();
		}
		else {
			file.setStatus(FileRecordType.infected);
		}
		file.setAnalyzedAt(new Date());
		file.markProcessingEnd();
		logger.info("File analysis finished: "+file);
	}
}
