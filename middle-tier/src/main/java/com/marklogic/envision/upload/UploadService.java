package com.marklogic.envision.upload;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.datamovement.DataMovementManager;
import com.marklogic.client.datamovement.JacksonCSVSplitter;
import com.marklogic.client.datamovement.WriteBatcher;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.envision.pojo.StatusMessage;
import com.marklogic.grove.boot.AbstractController;
import com.marklogic.hub.impl.HubConfigImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class UploadService extends AbstractController {

	final private SimpMessagingTemplate template;
	private  DatabaseClient client;
	private DataMovementManager dataMovementManager;
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	@Autowired
	UploadService(HubConfigImpl hubConfig, SimpMessagingTemplate template) {
		super(hubConfig);
		this.template = template;
	}

	private void init() {
		if (this.client == null) {
			this.client = getStagingClient();
			this.dataMovementManager = client.newDataMovementManager();
		}
	}

	@Async
	public void uploadFile(InputStream stream, String collectionName) {
		init();

		ServerTransform serverTransform = new ServerTransform("mlRunIngest");
		String jobId = UUID.randomUUID().toString();
		serverTransform.addParameter("job-id", jobId);
		String step = "1";
		serverTransform.addParameter("step", step);

		DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
		metadataHandle.withCollections(collectionName);
		DocumentMetadataHandle.DocumentMetadataValues metadataValues = metadataHandle.getMetadataValues();
		metadataValues.add("datahubCreatedByJob", jobId);
		metadataValues.add("datahubCreatedOn", DATE_TIME_FORMAT.format(new Date()));
		metadataValues.add("datahubCreatedBy", getHubConfig().getMlUsername());

		int threadCount = 4;
		int batchSize = 256;
		WriteBatcher writeBatcher = dataMovementManager.newWriteBatcher()
			.withDefaultMetadata(metadataHandle)
			.withBatchSize(batchSize)
			.withThreadCount(threadCount)
			.withJobId(jobId)
			.withTransform(serverTransform);

		BOMInputStream bis = new BOMInputStream(stream, false);
		CsvSchema schema = buildSchema(bis);
		JacksonCSVSplitter splitter = new JacksonCSVSplitter().withCsvSchema(schema);
		StatusMessage msg = StatusMessage.newStatus(collectionName);

		String username = getHubConfig().getMlUsername();
		try {
			if (!writeBatcher.isStopped()) {
				AtomicLong totalUris = new AtomicLong(0);
				updateStatus(msg.withMessage("Splitting " + collectionName + "..."));
				long startTime = System.nanoTime();

				Stream<JacksonHandle> contentStream = splitter.split(bis);
				contentStream.forEach(jacksonHandle -> {
					String uri = String.format("/ingest/%s/%s/%s.json", username, collectionName, UUID.randomUUID());
					try {
						writeBatcher.add(uri, jacksonHandle);
						totalUris.addAndGet(1);
					}
					catch (IllegalStateException e) {
						logger.error("WriteBatcher has been stopped");
					}
				});
				long endTime = System.nanoTime();
				long duration = (endTime - startTime);
				logger.error("Duration: " + (duration / 1000000) + " ms");
				updateStatus(
					msg.withMessage("Processing " + collectionName + "...")
						.withPercentComplete(0)
				);
				writeBatcher.onBatchSuccess(batch -> {
					long completed = batch.getJobWritesSoFar();
					int percentComplete = (int) (((double)completed / totalUris.get()) * 100.0);
					updateStatus(msg.withPercentComplete(percentComplete));
				})
					.onBatchFailure((batch, ex) -> {
						long completed = batch.getJobWritesSoFar();
						int percentComplete = (int) (((double)completed / totalUris.get()) * 100.0);
						updateStatus(msg.withPercentComplete(percentComplete));
					});
				writeBatcher.flushAndWait();
				dataMovementManager.stopJob(writeBatcher);
				updateStatus(msg.withPercentComplete(100));
			}
		} catch (Exception e) {
			IOUtils.closeQuietly(stream);
			updateStatus(msg.withError(e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	private void updateStatus(StatusMessage message) {
		template.convertAndSend("/topic/status", message);
	}

	private CsvSchema buildSchema(InputStream stream) {
		String delimiter = ",";
		try {

			StringBuilder headerLineBuilder = new StringBuilder();
			int data = stream.read();
			while(data != -1) {
				char theChar = (char)data;
				if (theChar == '\n') {
					break;
				}
				if (theChar != '\r') {
					headerLineBuilder.append(theChar);
				}
				data = stream.read();
			}

			String headerLine = headerLineBuilder.toString();

			// assume one of following delimiters
			Map<String, Integer> counts = new HashMap<>();
			List<String> possibleDelimiters = Arrays.asList(",", ";", "\t", "|");
			String tmpHeader = headerLine;
			for (String del : possibleDelimiters) {
				int index = tmpHeader.indexOf(del);
				int count = 0;
				while (index != -1) {
					count++;
					tmpHeader = tmpHeader.substring(index + 1);
					index = tmpHeader.indexOf(del);
				}
				counts.put(del, count);
			}

			Map.Entry<String, Integer> maxEntry = null;

			for (Map.Entry<String, Integer> entry : counts.entrySet()) {
				if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
					maxEntry = entry;
				}
			}
			if (maxEntry != null) {
				delimiter = maxEntry.getKey();
			}

			String[] headers = headerLine.split(Pattern.quote(delimiter));
			CsvSchema.Builder builder = CsvSchema.builder();
			for (String header : headers) {
				builder.addColumn(header);
			}
			return builder.setQuoteChar('"')
				.setUseHeader(false)
				.setColumnSeparator(delimiter.charAt(0))
				.build();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return CsvSchema.emptySchema().withColumnSeparator(delimiter.charAt(0));
	}
}
