package io.github.kubaseai.av.model;

import java.io.File;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.github.kubaseai.av.utils.StringAttributeEncryptor;

@RestResource(exported = false)
@Entity
public class FileRecord {
	
	public enum FileRecordType { accepted, rejected, clean, infected, failure };
	
	@Id
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String id;
	
	@Convert(converter = StringAttributeEncryptor.class)
	@Column(columnDefinition = "TEXT")
	private String name;
	
	private String sha512;
	private long size;
	private FileRecordType status;
	private Date queuedAt;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Date analyzedAt;
	@JsonIgnore
	private transient File localFile;
	private String source;
	private transient LinkedBlockingQueue<Object> callback = new LinkedBlockingQueue<Object>();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSha512() {
		return sha512;
	}
	public void setSha512(String sha512) {
		this.sha512 = sha512;
	}
	public FileRecordType getStatus() {
		return status;
	}
	public void setStatus(FileRecordType status) {
		this.status = status;
	}
	public Date getQueuedAt() {
		return queuedAt;
	}
	public void setQueuedAt(Date queuedAt) {
		this.queuedAt = queuedAt;
	}
	public Date getAnalyzedAt() {
		return analyzedAt;
	}
	public void setAnalyzedAt(Date analyzedAt) {
		this.analyzedAt = analyzedAt;		
	}
	public File getLocalFile() {
		return localFile;
	}
	public void setLocalFile(File localFile) {
		this.localFile = localFile;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
	        this.size = size;
	}
	public String getSource() {
	        return source;
	}
	public void setSource(String source) {
	        this.source = source;
	}
	public void waitForCallback(long millis) throws InterruptedException {
		this.callback.poll(millis, TimeUnit.MILLISECONDS);
	}
	public String toString() {
	        return "FileInfo { id="+id+", name="+name+", hash="+sha512+", size="+size+", queuedAt="+queuedAt+", analyzedAt="+analyzedAt+", status="+status+", localPath="+localFile.getAbsolutePath()+", source="+source+" }";
	}
	public HttpStatus getHttpStatus() {
		if (FileRecordType.accepted.equals(status)) {
			return HttpStatus.REQUEST_TIMEOUT;
		}
		else if (FileRecordType.clean.equals(status)) {
			return HttpStatus.OK;
		}
		else if (FileRecordType.failure.equals(status)) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		else if (FileRecordType.rejected.equals(status)) {
			return HttpStatus.BAD_REQUEST;
		}
		return HttpStatus.CONFLICT;
	}

    public void markProcessingEnd() {
        this.callback.offer(new Object());
    }
}
