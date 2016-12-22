package com.mypackage.executors;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.api.java.function.VoidFunction;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Properties;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

//import com.twitter.bijection.Injection;
//import com.twitter.bijection.avro.GenericAvroCodecs;

public class SendKafkaExecutor<I> implements VoidFunction<Iterator<I>>, Serializable, ForeachPartitionFunction<I> {

	public SendKafkaExecutor(String targetTopic, String targetKafkaServerPort, boolean shouldUseAVRO, Class<?> inputClass) {
		this.targetTopic = targetTopic;
		this.targetKafkaServerPort = targetKafkaServerPort;
		this.shouldUseAVRO = shouldUseAVRO;
		this.inputClass = inputClass;
	}

	// final static Logger logger = LoggerFactory.getLogger(SendJSON.class);
	final static Logger logger = Logger.getLogger(SendKafkaExecutor.class);

	private static final long serialVersionUID = 1L;

	String targetTopic;
	String targetKafkaServerPort;
	boolean shouldUseAVRO;
	Class<?> inputClass;

	public String getTargetTopic() {
		return targetTopic;
	}

	public void setTargetTopic(String targetTopic) {
		this.targetTopic = targetTopic;
	}

	public String getTargetKafkaServerPort() {
		return targetKafkaServerPort;
	}

	public void setTargetKafkaServerPort(String targetKafkaServerPort) {
		this.targetKafkaServerPort = targetKafkaServerPort;
	}

	public boolean isShouldUseAVRO() {
		return shouldUseAVRO;
	}

	public void setShouldUseAVRO(boolean shouldUseAVRO) {
		this.shouldUseAVRO = shouldUseAVRO;
	}

	@Override
	public void call(Iterator<I> iterator) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("<o> ---> apply Method START - shouldUseAVRO: " + shouldUseAVRO);
		Properties propsNoAVRO = new Properties();
		propsNoAVRO.put("bootstrap.servers", targetKafkaServerPort);
		propsNoAVRO.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		propsNoAVRO.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		KafkaProducer<String, String> producerNoAVRO = new KafkaProducer<>(propsNoAVRO);
		//
		Gson gson = new Gson();
		while (iterator.hasNext()) {
			I employee = iterator.next();

			logger.debug("<o> ---> employee  : " + employee.toString());

			String json = gson.toJson(employee);

			logger.debug("<o> ---> employee  json: " + employee.toString());

			if (shouldUseAVRO) {
				// TODO To be revised AVRO case
				// if (logger.isDebugEnabled())
				// logger.debug("<o> ---> apply Method, toKafkaRawAVRO will be invoked");
				// toKafkaRawAVRO(json,targetTopic,targetKafkaServerPort,producerRawAVRO,recordInjection,avroRecord);
			} else {
				toKafkaNoAVRO(json, targetTopic, targetKafkaServerPort, producerNoAVRO);
			}
		}

		// Close producers
		// producerRawAVRO.close();
		producerNoAVRO.close();

		if (logger.isDebugEnabled())
			logger.debug("<o> ---> apply Method EXIT");

	}

	public void toKafkaNoAVRO(String jsonRowObject, String targetTopic, String targetKafkaServerPort,
							  KafkaProducer<String, String> producerNoAVRO) {
		if (logger.isDebugEnabled())
			logger.debug("<o> START toKafkaNoAVRO");
		ProducerRecord<String, String> record = new ProducerRecord<>(targetTopic, jsonRowObject);
		producerNoAVRO.send(record);
		if (logger.isDebugEnabled())
			logger.debug("<o>============== >> toKafkaNoAVRO: - string: --> " + record.toString());
	}




}