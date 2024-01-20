package io.github.kubaseai.av.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.http.HttpHeaders;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletRequest;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import io.github.kubaseai.av.config.MainConfiguration;
import io.github.kubaseai.av.model.FileRecord;
import io.github.kubaseai.av.model.FileRecord.FileRecordType;
import io.github.kubaseai.av.service.impl.AvScanningService;
import io.github.kubaseai.av.service.impl.ScanStrategy;
import io.github.kubaseai.av.service.impl.ScanStrategyRealTime;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@OpenAPIDefinition(
	info = @Info(
		title = "Anti-Virus Service",
		version = "0.9"
	)
)
public class AvScanningRestService {
	
	private final AvScanningService svc;
	private final MainConfiguration cfg;
	private final static Logger logger = LoggerFactory.getLogger(AvScanningRestService.class);
	
	public AvScanningRestService(AvScanningService svc, MainConfiguration cfg) {
		this.cfg = cfg;
		this.svc = svc;
	}
		
	@Secured("ROLE_USER")
	@Operation(summary = "This operation lists all uploaded files.")
	@RequestMapping(value = "/rest/api/1.0/av-scan/files", 
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PagedModel<EntityModel<FileRecord>>> listPackages(Pageable pageable,
		PagedResourcesAssembler<FileRecord> assembler) {
		PagedModel<EntityModel<FileRecord>> payload = assembler.toModel(svc.listFiles(pageable));
			return new ResponseEntity<>(payload, HttpStatus.OK);
	}
	
	@Secured("ROLE_USER")
	@Operation(summary = "This operation returns a single file info.")
	@RequestMapping(value = "/rest/api/1.0/av-scan/files/{id}", 
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<FileRecord> getFile(@PathVariable("id") String id) {
		FileRecord payload = svc.getFile(id);
		return new ResponseEntity<>(payload, payload!=null ? HttpStatus.OK : HttpStatus.NOT_FOUND);
	}
	
	
	@Secured("ROLE_USER")
	@Operation(summary = "This operation checks if for a given hash the file exists.")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "Files matching provided hash", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = FileRecord.class))})})
	@RequestMapping(value = "/rest/api/1.0/av-scan/files/by-hash/{hash}", 
		method = RequestMethod.GET,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<FileRecord>> getFileByHash(@PathVariable("hash") String hash) throws Exception {
		List<FileRecord> list = svc.getFileByHash(hash);
		return new ResponseEntity<>(list, HttpStatus.OK);
	}
	
	@Secured("ROLE_USER")
	@Operation(summary = "This operation queues a file scan")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "201", description = "File upload accepted", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = String.class)) })})
	@RequestMapping(value = "/rest/api/1.0/av-scan/files/{name}", 
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<FileRecord> scan(ServletRequest req, InputStream file, @PathVariable("name") String name, 
	        @RequestParam(value="waitMillis", required=false) Long waitMillis) throws Exception {
		if (req instanceof MultipartHttpServletRequest) {
			MultipartHttpServletRequest multi = (MultipartHttpServletRequest)req;
			MultipartFile multipartFile = multi.getFile("file");
			file = multipartFile.getInputStream();
			name = new File(multipartFile.getOriginalFilename()).getName();
		}
		String id = UUID.randomUUID().toString();
		FileRecord fileInfo = new FileRecord();
		fileInfo.setId(id);
		fileInfo.setName(name);
		try {
			return processFileStream(fileInfo, file, req, waitMillis);
        }
		catch (Exception e) {
			logger.error("Error in file processing", e);
			fileInfo.setStatus(FileRecordType.failure);
			return new ResponseEntity<>(fileInfo, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private String getSource(ServletRequest req) {
	        SecurityContext ctx = SecurityContextHolder.getContext();
		Authentication auth = ctx!=null ? ctx.getAuthentication() : null;
		String user = (auth!=null) ? auth.getPrincipal()+"": "anonymous";
		String host = req.getRemoteAddr();
		if (!host.equals(req.getRemoteHost())) {
		        host = req.getRemoteHost() + "/" + host;
		}
		return user+"@"+host;
	}

	private ResponseEntity<FileRecord> processFileStream(FileRecord fileInfo, InputStream file,
		ServletRequest req, Long waitMillis) throws IOException, NoSuchAlgorithmException, InterruptedException
	{
		File f = svc.prepareFilePath(fileInfo.getId(), fileInfo.getName());
		fileInfo.setLocalFile(f);
		try (InputStream is = file; RandomAccessFile out = new RandomAccessFile(f, "rw")) {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] buff = new byte[4096];
			FileChannel ch = out.getChannel();
			int n = 0;
			long bytesRead = 0;
			ByteArrayOutputStream guessFileTypeStream = new ByteArrayOutputStream();
			while ((n = is.read(buff)) > 0) {
				if (n > 0) {
					if (guessFileTypeStream.size()< 1024)
						guessFileTypeStream.write(buff, 0, n);
				}
				bytesRead += n;
				md.update(buff, 0, n);
				out.write(buff, 0, n);
				if (out.length() > cfg.getMaxFileSize()) {
					out.close();
					Files.delete(f.toPath());
					fileInfo.setStatus(FileRecordType.rejected);
					return new ResponseEntity<>(fileInfo, HttpStatus.PAYLOAD_TOO_LARGE);
				}
			}
			String hash = Hex.toHexString(md.digest());
			fileInfo.setSha512(hash);
			fileInfo.setQueuedAt(new Date());
			fileInfo.setStatus(FileRecordType.accepted);
			fileInfo.setSize(bytesRead);
			ScanStrategy.guessFileType(guessFileTypeStream.toByteArray(), fileInfo);
			try {
				ch.force(true);
			}
			catch (Exception e) {
				RuntimeException toRethrow = null;
				try {
					FileStore fs = Files.getFileStore(f.toPath());
					if (fs.isReadOnly() || fs.getUsableSpace() == 0) {
						toRethrow = new RuntimeException("File write error", e);
					}
				}
				catch (Exception ex) {}
				if (toRethrow!=null)
					throw toRethrow;
			}
			fileInfo.setSource(getSource(req));
			svc.queueFileForScanning(fileInfo);
			fileInfo.waitForCallback(waitMillis!=null ? waitMillis : 0L);
			return new ResponseEntity<>(fileInfo, HttpStatus.ACCEPTED);
		}
	}

	

	@Secured("ROLE_USER")
	@RequestMapping(value = "/icap/{name}", 
		method = RequestMethod.POST,
		produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> scan(ServletRequest req, InputStream file, @PathVariable("name") String name) throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.put("Service", List.of("ICAP scanner AVREST"));
		map.put("ISTag", List.of(System.currentTimeMillis()/3600 + ""));
		map.put("Encapsulated", List.of("null-body=0"));
		String fileId = UUID.randomUUID().toString();
		FileRecord fileInfo = new FileRecord();
		fileInfo.setId(fileId);
		fileInfo.setName(name);
		processFileStream(fileInfo, file, req, 60000L);
		map.put("X-SHA-512", List.of(fileInfo.getSha512()));
		map.put("X-Size", List.of(fileInfo.getSize()+""));
		map.put("X-File-Name", List.of(fileInfo.getName()));
		map.put("X-Analyzed-At", List.of(fileInfo.getAnalyzedAt()+""));
		map.put("X-Source", List.of(fileInfo.getSource()));

		if (FileRecord.FileRecordType.clean.equals(fileInfo.getStatus())) {
			logger.info("Clean file: "+fileInfo);
			return new ResponseEntity<>(map, HttpStatus.NO_CONTENT);
		}
		else if (FileRecord.FileRecordType.infected.equals(fileInfo.getStatus())) {
			logger.info("Malicious file: "+fileInfo);
			map.put("X-Infection-Found", List.of("Type=0; Resolution=2; Threat=infected;"));
			map.put("X-Virus-ID", List.of(fileInfo.getId()));
			map.put("X-Response-Desc", List.of("File was deleted by AV"));
			return new ResponseEntity<>(map, HttpStatus.FORBIDDEN);
		}	
		return new ResponseEntity<>(map, fileInfo.getHttpStatus());
	}

	@Secured("ROLE_USER")
	@RequestMapping(value = "/icap", 
		method = RequestMethod.OPTIONS,
		produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> scanOptions(ServletRequest req, @RequestHeader Map<String, String> headers) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.put("ISTag", List.of(System.currentTimeMillis()/3600 + ""));
		map.put("Service", List.of("ICAP scanner AVREST"));
		map.put("Methods", List.of("RESPMOD"));
		map.put("Allow", List.of("204"));
		map.put("Max-Connections", List.of("10"));
		map.put("Options-TTL", List.of("3600"));
		map.put("Encapsulated", List.of("null-body=0"));
		System.out.println("Responding to options, request contains headers: "+headers);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}
}
