package io.github.kubaseai.av.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application")
public class MainConfiguration {
	
	protected final static Logger logger = LoggerFactory.getLogger(MainConfiguration.class);
		
	private byte[] dbEncryptionPasswordForBytes = new byte[0];
	private byte[] dbEncryptionPasswordForStrings = new byte[0];
	private String accessTokens;
	private String workingDir;
	private long maxFileSize = 30*1024*1024;
	private int threadPoolSize = 16;
	private String avScannerCommand;
	
	public String getAccessTokens() {
		return accessTokens;
	}

	public void setAccessTokens(String accessTokens) {
		this.accessTokens = accessTokens;
	}

	public void setDbEncryptionPasswordForBytes(String password) {
		for (int i=0; i < dbEncryptionPasswordForBytes.length; i++) {
			dbEncryptionPasswordForBytes[i] = ' ';
		}
		dbEncryptionPasswordForBytes = password!=null ? password.getBytes() : new byte[0];
	}
	
	public byte[] getDbEncryptionPasswordForBytes() {
		byte[] response = new byte[dbEncryptionPasswordForBytes.length];
		System.arraycopy(dbEncryptionPasswordForBytes, 0, response, 0, dbEncryptionPasswordForBytes.length);
		return response;
	}
	
	public void setDbEncryptionPasswordForStrings(String password) {
		for (int i=0; i < dbEncryptionPasswordForStrings.length; i++) {
			dbEncryptionPasswordForStrings[i] = ' ';
		}
		dbEncryptionPasswordForStrings = password!=null ? password.getBytes() : new byte[0];
	}
	
	public byte[] getDbEncryptionPasswordForStrings() {
		byte[] response = new byte[dbEncryptionPasswordForStrings.length];
		System.arraycopy(dbEncryptionPasswordForBytes, 0, response, 0, dbEncryptionPasswordForBytes.length);
		return response;
	}

	public String getWorkingDir() {
		return workingDir;
	}
	public void setWorkingDir(String dir) {
		this.workingDir = dir;		
	}

	public long getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}
	
	public void setThreadPoolSize(int size) {
		this.threadPoolSize = size;
	}

	public String getAvScannerCommand() {
		return avScannerCommand;
	}

	public void setAvScannerCommand(String avScannerCommand) {
		this.avScannerCommand = avScannerCommand;
	}
}
