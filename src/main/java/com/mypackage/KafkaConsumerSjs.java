package com.mypackage;

import com.mypackage.executors.MessageAndMetadataStringFunction;
import com.mypackage.executors.ParseJsonExecutor;
import com.mypackage.executors.SendKafkaExecutor;
import com.mypackage.model.Employee;
import com.typesafe.config.Config;
import consumer.kafka.MessageAndMetadata;
import consumer.kafka.ProcessedOffsetManager;
import consumer.kafka.ReceiverLauncher;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import spark.jobserver.JavaSparkJob;
import spark.jobserver.SparkJobValid$;
import spark.jobserver.SparkJobValidation;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/**
 * Created by me on 07/11/2016.
 */

public class KafkaConsumerSjs extends JavaSparkJob implements Serializable {

    final static Logger logger = Logger.getLogger(KafkaConsumerSjs.class);
    private static final String JOB = "KafkaConsumerSjs";

    private static final boolean DEV = true;
    private static final String ZOOKEEPER_HOST = "mydomain.com";
    private static final String ZOOKEEPER_IP = "127.0.0.1";

    private static final String ZOOKEEPER_PORT = "2181";

    private static final String KAFKA_INPUT_TOPIC = "inputTopic";
    private static final String KAFKA_TARGET_TOPIC = "employees";
    private static final String KAFKA_TARGET_GROUPID = "KafkaConsumerSjs";
    private static final String KAFKA_PRODUCER_PORT = "9092";

    @Override
    public Object runJob(JavaSparkContext sc, Config jobConfig) {
        logging(" --> Starting!!! Please, be sure that topic existed previously with data!!",2,null);

        if (logger.isInfoEnabled())
            logger.info("<o>============== STARTING readStreaming");

        // 1. config
        String targetTopic = "employees";
        int numPartitions = 1;
        final String groupId = "KafkaConsumerSjs";

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
                .setAppName("KafkaConsumerSjs")
                //.set("key.eventLog.enabled", "true")
                //.setMaster("local[*]")
                //.setMaster("yarn-cluster")
                //.set("spark.executor.memory", "2g")
                //.set("spark.cores.max", "2")
                ;

        JavaStreamingContext streamingContext = new JavaStreamingContext(sc, new Duration(Long.parseLong("2000")));


        JavaDStream<MessageAndMetadata> unionStreams = ReceiverLauncher.launch(streamingContext, props, numPartitions,
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
            logger.info("<o>============== STARTING sendJSONtoKafka :: targetOutput");

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
        //streamingContext.stop();

        logging(" -->  END JOB",1,null);

        return this;
    }


    private static void logging(String message, int level, Exception obj){
        if (DEV) {
            if (level == 1) logger.info("<o>============== Job: " + JOB + message);
            else if (level == 2) logger.debug("<o>============== Job: " + JOB + message);
            else logger.error("<o>============== Job: " + JOB + message + " Exception: " + obj.toString(), obj);
        }
    }
    
	protected void createTopicIfNotExist(String topic, String zookeeper, String kafkaServer, String kafkaPort) {
		KafkaUtil kafkaTopicService = new KafkaUtil(zookeeper, kafkaServer, Integer.valueOf(kafkaPort), 6000, 6000);

		kafkaTopicService.createTopicIfNotExist(topic, 1, 1);
		try {
			kafkaTopicService.close();
		} catch (IOException io) {
			System.out.println("Error closing kafkatopicservice");
			io.printStackTrace();
		}
	}
/*
    @Override
    public SparkJobValidation validate(SparkContext sc, Config config) {
        return SparkJobValid$.MODULE$;
    }

    @Override
    public String invalidate(JavaSparkContext jsc, Config config) {
        return null;
    }
*/
}