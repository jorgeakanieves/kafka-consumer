package com.mypackage;

import com.mypackage.executors.MessageAndMetadataStringFunction;
import com.mypackage.executors.ParseJsonExecutor;
import com.mypackage.executors.SendKafkaExecutor;
import com.mypackage.model.Employee;

import consumer.kafka.MessageAndMetadata;
import consumer.kafka.ProcessedOffsetManager;
import consumer.kafka.ReceiverLauncher;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.io.IOException;
import java.util.Properties;


/**
 * Created by me on 07/11/2016.
 */
public class KafkaConsumerNoSjs {

    final static Logger logger = Logger.getLogger(KafkaConsumerNoSjs.class);
    private static final String JOB = "KafkaConsumerNoSjs";
    final static String SCHEMA_DEF = "{"
            + "\"type\":\"record\","
            + "\"name\":\"TOPIC_name"+"\","
            + "\"fields\":["
            + "  { \"name\":\"row\", \"type\":\"string\" }"
            + "]}";

    private static final boolean DEV = true;
    private static final String ZOOKEEPER_HOST = "mydomain.com";
    private static final String ZOOKEEPER_IP = "127.0.0.1";
    private static final String ZOOKEEPER_PORT = "2181";
    private static final String KAFKA_INPUT_TOPIC = "inputTopic";
    private static final String KAFKA_TARGET_TOPIC = "employees";
    private static final String KAFKA_TARGET_GROUPID = "KafkaConsumerNoSjs";
    private static final String KAFKA_PRODUCER_PORT = "9092";

    private static final int KAFKA_NUM_PARTITIONS = 1;


    public static void main(String[] args) throws Exception {
        logging(" --> Starting!!! Please, be sure that topic existed previously with data!!",2,null);

        if (logger.isInfoEnabled())
            logger.info("<o>============== STARTING readStreaming");

        // 1. config
        Properties props = new Properties();
        props.put("zookeeper.hosts", ZOOKEEPER_HOST);
        props.put("zookeeper.port", ZOOKEEPER_PORT);
        props.put("zookeeper.broker.path", "/brokers");
        props.put("kafka.topic", KAFKA_INPUT_TOPIC);
        props.put("kafka.consumer.id", KAFKA_TARGET_GROUPID);
        props.put("zookeeper.consumer.connection", ZOOKEEPER_HOST+":"+ZOOKEEPER_PORT);
        props.put("zookeeper.consumer.path", "/spark-kafka");
        props.put("consumer.forcefromstart", "false");
        // Optional Properties
        // props.put("consumer.forcefromstart", "false");
        props.put("consumer.fetchsizebytes", "102400");

        SparkConf conf = new SparkConf()
                .setAppName("KafkaConsumerNoSjs")
                //.set("key.eventLog.enabled", "true")
                //.setMaster("local[*]")
                //.setMaster("yarn-cluster")
                //.set("spark.executor.memory", "2g")
                //.set("spark.cores.max", "2")
                ;

        JavaStreamingContext streamingContext = new JavaStreamingContext(conf, new Duration(Long.parseLong("2000")));

        JavaDStream<MessageAndMetadata> unionStreams = ReceiverLauncher.launch(streamingContext, props, KAFKA_NUM_PARTITIONS,
                StorageLevel.MEMORY_ONLY());

        // Get the Max offset from each RDD Partitions. Each RDD Partition
        // belongs to One Kafka Partition
        JavaPairDStream<Integer, Iterable<Long>> partitionOffset = ProcessedOffsetManager.getPartitionOffset(unionStreams);

        JavaDStream<Employee> dStream = null;

        final Class<?> inputClass = null;
        MessageAndMetadataStringFunction mmFunction = new MessageAndMetadataStringFunction();
        JavaDStream<String> stringStream = unionStreams.mapPartitions(mmFunction);

        ParseJsonExecutor<Employee> parse = new ParseJsonExecutor<Employee>(Employee.class);
        dStream = stringStream.mapPartitions(parse);

        ProcessedOffsetManager.persists(partitionOffset, props);


        if (logger.isInfoEnabled())
            logger.info("<o>============== STARTING sendJSONtoKafka :: targetOutputTest");

        createTopicIfNotExist(KAFKA_TARGET_TOPIC, ZOOKEEPER_IP+":"+ZOOKEEPER_PORT, ZOOKEEPER_IP, KAFKA_PRODUCER_PORT);

        final Class<?> outputClass = null;

        final SendKafkaExecutor<Employee> sendJSON = new SendKafkaExecutor<Employee>(KAFKA_TARGET_TOPIC,
                ZOOKEEPER_IP+":"+KAFKA_PRODUCER_PORT, false, Employee.class);

        dStream.foreachRDD(new VoidFunction<JavaRDD<Employee>>() {
            private static final long serialVersionUID = -2864481467046526620L;

            @Override
            public void call(JavaRDD<Employee> rdd) throws Exception {
                rdd.foreachPartition(sendJSON);
                logging(" -->  Saved data to kafka",1,null);

            }
        });


        streamingContext.start();
        streamingContext.awaitTermination();
//        streamingContext.stop();

        logging(" -->  END JOB",1,null);
    }
    
    protected static void createTopicIfNotExist(String topic, String zookeeper, String kafkaServer, String kafkaPort) {
		KafkaUtil kafkaTopicService = new KafkaUtil(zookeeper, kafkaServer, Integer.valueOf(kafkaPort), 6000, 6000);

		kafkaTopicService.createTopicIfNotExist(topic, 1, 1);
		try {
			kafkaTopicService.close();
		} catch (IOException io) {
			System.out.println("Error closing kafkatopicservice");
			io.printStackTrace();
		}

	}


    private static void logging(String message, int level, Exception obj){
        if (DEV) {
            if (level == 1) logger.info("<o>============== Job: " + JOB + message);
            else if (level == 2) logger.debug("<o>============== Job: " + JOB + message);
            else logger.error("<o>============== Job: " + JOB + message + " Exception: " + obj.toString(), obj);
        }
    }


}