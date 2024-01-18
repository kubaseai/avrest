package io.github.kubaseai.av.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import io.github.kubaseai.av.model.FileRecord;

public abstract class ScanStrategy {

	protected abstract void scan(FileRecord file);
	
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
}
