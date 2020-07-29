package com.marklogic.envision.upload;

import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class UploadController extends AbstractController {

	final private UploadService uploadService;
	final private SimpMessagingTemplate template;

	@Autowired
	UploadController(HubConfigImpl hubConfig, UploadService uploadService, SimpMessagingTemplate template) {
		super(hubConfig);
		this.uploadService = uploadService;
		this.template = template;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> uploadFile(@RequestParam("collection") String collection, @RequestParam("file") MultipartFile file) throws IOException {
		uploadService.uploadFile(file.getInputStream(), collection);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
