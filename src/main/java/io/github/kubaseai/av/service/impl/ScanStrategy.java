package io.github.kubaseai.av.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import io.github.kubaseai.av.model.FileRecord;

public abstract class ScanStrategy {

	protected abstract void scan(FileRecord file);
	protected int deepScan = 1;
	
	public boolean canReadFile(File f) {
		boolean readable = false;
		if (f.exists()) {
			try (FileInputStream fis = new FileInputStream(f)) {
				try {
					fis.read(new byte[4096*8]);
					readable = true;
				} catch (IOException e) {
					readable = false;
				}
			}
			catch (IOException ioe) {
				readable = false;
			}
		}
		else {
			readable = false;
		}
		return readable;
	}

	public static void guessFileType(byte[] arr, FileRecord fileInfo) {
		String s = new String(arr, 0, arr.length);
		if (s.startsWith("MZ") && s.contains("DOS mode")) {
			fileInfo.setTypeFromContent(".exe|.dll|application");
		}
		else if (s.indexOf("ELF") == 1) {
			fileInfo.setTypeFromContent("elf|application");
		}
		else if (s.startsWith(ScanStrategyRealTime.EICAR_PART_1)) {
			fileInfo.setTypeFromContent(".com|eicar|application");
		}
		else if ((s.startsWith("{") || s.startsWith("[")) && s.contains("\"") && s.contains(":")) {
			fileInfo.setTypeFromContent(".json|json");
		}
		else if (s.startsWith("PK") && s.contains("docProps/core.xml")) {
			fileInfo.setTypeFromContent(".docx|document");
		}
		else if (s.startsWith("PK") && s.contains("[Content_Types].xml")) {
			fileInfo.setTypeFromContent(".xlsx|document");
		}
		else if (s.startsWith("%PDF-")) {
			fileInfo.setTypeFromContent(".pdf|document");
		}
	}

	public void setDeepScan(int deepScan) {
		this.deepScan = deepScan;
	}
}
