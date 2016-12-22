package com.mypackage;

import kafka.admin.AdminUtils;
import kafka.admin.BrokerMetadata;
import kafka.common.Topic;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.log4j.Logger;
import scala.collection.Map;
import scala.collection.Seq;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public class KafkaUtil implements Closeable {
	final static Logger logger = Logger.getLogger(KafkaUtil.class);

	private final ZkClient zkClient;
	private final SimpleConsumer simpleConsumer;

	private final static int CONSUMER_TIMEOUT = 100000;
	private final static int CONSUMER_BUFFER_SIZE = 64 * 1024;
	private final static String CONSUMER_CLIENT_ID = "leaderLookup";

	public KafkaUtil(String zokeeperCluster, String broker, int brokerPort, int connectionTimeout, int sessionTimeout) {
		this.zkClient = new ZkClient(zokeeperCluster, sessionTimeout, connectionTimeout, new ZkStringSerializer());
		this.simpleConsumer = new SimpleConsumer(broker, brokerPort, CONSUMER_TIMEOUT, CONSUMER_BUFFER_SIZE,
				CONSUMER_CLIENT_ID);
	}

	public void createTopicIfNotExist(String topic, int replicationFactor, int partitions) {
		ZkUtils utils = ZkUtils.apply(zkClient, false);
		if (!AdminUtils.topicExists(utils, topic)) {
			createOrUpdateTopic(topic, replicationFactor, partitions);
		} else {
			if (logger.isInfoEnabled())
				logger.info("Topic " + topic + " already exists");
		}
	}

	public void createOrUpdateTopic(String topic, int replicationFactor, int partitions) {
		if (logger.isDebugEnabled())
			logger.debug("Creating topic " + topic + " with replication " + replicationFactor + " and " + partitions
					+ " partitions");
		Topic.validate(topic);
		//Seq<Object> brokerList = ZkUtils.getSortedBrokerList(zkClient);
		ZkUtils utils = ZkUtils.apply(zkClient, false);
		
		Seq<BrokerMetadata> brokerMeta = AdminUtils.getBrokerMetadatas(utils, AdminUtils.getBrokerMetadatas$default$2(), AdminUtils.getBrokerMetadatas$default$3()); 
		
		Map<Object, Seq<Object>> partitionReplicaAssignment = AdminUtils.assignReplicasToBrokers(brokerMeta,
				partitions, replicationFactor, AdminUtils.assignReplicasToBrokers$default$4(),
				AdminUtils.assignReplicasToBrokers$default$5());
		
		
		AdminUtils.createOrUpdateTopicPartitionAssignmentPathInZK(utils, topic, partitionReplicaAssignment,
				AdminUtils.createOrUpdateTopicPartitionAssignmentPathInZK$default$4(),
				AdminUtils.createOrUpdateTopicPartitionAssignmentPathInZK$default$5());
		if (logger.isDebugEnabled())
			logger.debug("Topic " + topic + " created");
	}
	public Integer getNumPartitionsForTopic(String topic) {
		TopicMetadataRequest topicRequest = new TopicMetadataRequest(Arrays.asList(topic));
		TopicMetadataResponse topicResponse = simpleConsumer.send(topicRequest);
		for (TopicMetadata topicMetadata : topicResponse.topicsMetadata()) {
			if (topic.equals(topicMetadata.topic())) {
				int partitionSize = topicMetadata.partitionsMetadata().size();
				if (logger.isDebugEnabled())
					logger.debug("Partition size found (" + partitionSize + ") for " + topic + " topic");
				return partitionSize;
			}
		}
		logger.warn("Metadata info not found!. TOPIC " + topic);
		return null;
	}

	public void deleteTopics() {
		zkClient.deleteRecursive("/brokers/topics");
	}

	public void close() throws IOException {
		zkClient.close();
		simpleConsumer.close();
	}
}
