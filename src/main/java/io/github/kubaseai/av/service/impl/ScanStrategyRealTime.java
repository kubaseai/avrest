package io.github.kubaseai.av.service.impl;

import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.kubaseai.av.model.FileRecord;
import io.github.kubaseai.av.model.FileRecord.FileRecordType;

public class ScanStrategyRealTime extends ScanStrategy {
	
	private final static Logger logger = LoggerFactory.getLogger(ScanStrategyRealTime.class);

	public final static String EICAR_PART_1 = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-";
	private final static String EICAR_PART_2 = "ANTIVIRUS-TEST-FILE!$H+H*";
	private final static String EICAR_SHA512 = sha512((EICAR_PART_1 + EICAR_PART_2).getBytes());

	private final static int FILE_TYPE_MAIN = 0;
	private final static int FILE_TYPE_INTERNAL = 1;
	private final static int FILE_TYPE_NESTED = 2;
	private final static String[] FILE_TYPE_TO_STR = { "Main", "Internal", "Nested" };

	private FileRecord writeAntiVirusTestFile(File f) {
		FileRecord fr = writeFileContent(f, (EICAR_PART_1 + EICAR_PART_2).getBytes());
		fr.setId("av-test-file");
		fr.setName("eicar.com");
		fr.setSource("avrest-svc");
		fr.setTypeFromContent("ANTIVIRUS-TEST-FILE");
		fr.setSha512(EICAR_SHA512);
		return fr;
	}

	private FileRecord writeFileContent(File f, byte[] content) {
		FileRecord fr = new FileRecord();
		try (RandomAccessFile out = new RandomAccessFile(f, "rw")) {
			FileChannel ch = out.getChannel();
			out.write(content);
			ch.force(true);
		}
		catch (Exception e) {
			// AV hooking flush() may cause this error or there is no free disk space
			try {
				FileStore fs = Files.getFileStore(f.toPath());
				if (fs.isReadOnly() || fs.getUsableSpace() == 0) {
					fr.setStatus(FileRecordType.failure);
				}
			}
			catch (Exception ex) {}
		}
		fr.setSize(content.length);
		fr.setLocalFile(f);
		return fr;
	}

	private static String sha512(byte[] bytes) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			md.update(bytes);
			return Hex.toHexString(md.digest());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void scan(FileRecord file) {
		File testFile = new File(file.getLocalFile().getParent(), "av-alive-test-"+file.getId()+".com");
		FileRecord fileRecord = writeAntiVirusTestFile(testFile);
		_scan(fileRecord, FILE_TYPE_INTERNAL);
		if (!FileRecordType.infected.equals(fileRecord.getStatus())) {
			logger.error("AV didn't clean test file");
			file.setStatus(FileRecordType.failure);
			file.markProcessingEnd();
			return;
		}
		_scan(file, FILE_TYPE_MAIN);
	}

	protected void _scan(FileRecord file, int type) {
		if (FileRecordType.failure.equals(file.getStatus())) {
			return;
		}
		File f = file.getLocalFile();
		boolean readable = super.canReadFile(f);
		readable = super.canReadFile(f);
		if (readable) {
			String cType = file.getTypeFromContent();
			boolean nestedInfection = false;
			if (deepScan > 0 && cType!=null && cType.startsWith(".json")) {
				nestedInfection = isNestedJsonInfected(file);
			}
			file.setStatus(nestedInfection ? FileRecordType.infected : FileRecordType.clean);
			f.delete();
		}
		else {
			file.setStatus(FileRecordType.infected);
		}
		file.setAnalyzedAt(new Date());
		file.markProcessingEnd();
		logger.info(FILE_TYPE_TO_STR[type] + " file analysis finished: "+file);
	}

	private boolean isNestedJsonInfected(FileRecord file) {
		try {
			logger.info("Checking JSON file "+file.getLocalFile().getPath());
			JsonParser p = Json.createParser(new FileReader(file.getLocalFile()));
			int i=0;
			while (p.hasNext()) {
				Event ev = p.next();
				if (ev == Event.VALUE_STRING) {
					String s = p.getString();
					if (checkBlobInfected(file, s, ++i, p.getLocation()+"")) {
						return true;
					}
				}				
			}
		}
		catch (Exception e) {}
		return false;
	}

	private boolean checkBlobInfected(FileRecord file, String s, int seq, String location) {
		try {
			byte[] b = Base64.getDecoder().decode(s);
			FileRecord fInfo = new FileRecord();
			ScanStrategy.guessFileType(b, fInfo);
			String type = fInfo.getTypeFromContent();
			if (type!=null && type.contains("|application")) {
				fInfo.setId(file.getId()+"-extract-"+seq);
				fInfo.setName("'[extract-"+seq+"]:"+location+"'");
				fInfo.setTypeFromContent(type);
				fInfo.setSource("deepscan");
				fInfo.setSize(b.length);
				fInfo.setSha512(sha512(b));
				String ext = "";
				for (String str : type.split("\\.")) {
					if (str.startsWith(".")) {
						ext = str;
						break;
					}
				}
				File f = new File(file.getLocalFile().getParentFile(), fInfo.getId()+ext);
				fInfo.setLocalFile(f);
				writeFileContent(f, b);
				_scan(fInfo, FILE_TYPE_NESTED);
				return FileRecordType.infected.equals(fInfo.getStatus());
			}
		}
		catch (Exception e) {}
		return false;
	}
}
