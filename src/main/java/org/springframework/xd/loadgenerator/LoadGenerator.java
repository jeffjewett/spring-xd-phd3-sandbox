/*
 * Copyright 2015 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.loadgenerator;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

/**
 * Is a source module that generates a series of fixed size messages to be
 * dispatched to a stream.  The load-generator is used to test the performance of
 * XD in different environments.
 *
 * @author Janne Valkealahti
 */
public class LoadGenerator extends MessageProducerSupport {

	private int producers;
	private int messageCount;
	private int recordCount;
	private String recordDelimiter;
	private boolean recordType;
	private long sleepTime;
	private int sleepCount;
	private final boolean sleep;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private ExecutorService executorService;
	
	private SimpleDateFormat outDateFormat;

	Logger logger = LoggerFactory.getLogger(LoadGenerator.class);

	public LoadGenerator(int producers, int messageCount, int recordCount, String recordDelimiter, String recordType, long sleepTime, int sleepCount) {
		this.producers = producers;
		this.messageCount = messageCount;
		this.recordCount = recordCount;
		
		if ("\\t".equals(recordDelimiter)) {
			this.recordDelimiter = "\t";
		} else {
			this.recordDelimiter = recordDelimiter;
		}
		
		if ("counter".equals(recordType)) {
			this.recordType = true;
		}
		this.sleepTime = sleepTime;
		this.sleepCount = sleepCount;
		this.sleep = sleepTime > 0 && sleepCount > 0;
		
		this.outDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
	}

	@Override
	protected void doStart() {
		executorService = Executors.newFixedThreadPool(producers);
		if (running.compareAndSet(false, true)) {
			for (int x = 0; x < producers; x++) {
				executorService.submit(new Producer(Integer.toString(x)));
			}
		}
	}

	@Override
	protected void doStop() {
		if (running.compareAndSet(true, false)) {
			executorService.shutdown();
		}
	}

	protected class Producer implements Runnable {
		private String producerPrefix;
		private Random rand = new Random();
		

		public Producer(String prefix) {
			this.producerPrefix = prefix;
		}

		private void send() {
				
			logger.info("Sending " + messageCount + " messages");
			
			for (int x = 0; x < messageCount; x++) {
				StringBuilder buf = new StringBuilder();
				buf.append(producerPrefix);                 // producer id
				for (int i = 0; i < recordCount; i++) {
					buf.append(recordDelimiter);
					if (recordType) {
					       
						java.sql.Timestamp sqlTimestamp =
							      new java.sql.Timestamp(System.currentTimeMillis());
						
						buf.append(sqlTimestamp); //buf.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS").format(new Date())); // timestamp
						buf.append(recordDelimiter);
						
						buf.append(Thread.currentThread().getId()); // thread id
						buf.append(recordDelimiter);
						
						buf.append(rand.nextFloat()); // data
						buf.append(recordDelimiter);
						
						buf.append(x); // sequence
					} else {
						buf.append(System.nanoTime());
					}
				}
				
				sendMessage(MessageBuilder.withPayload(buf.toString()).build());
				if (sleep && ((x + 1) % sleepCount) == 0) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
					}
				}
			}
			logger.info("All Messages Dispatched");
		}

		public void run() {
			send();
		}
	}
}
