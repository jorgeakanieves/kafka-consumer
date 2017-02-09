============== KAFKA PRODUCER:
./kafka-console-producer.sh --broker-list ZOOKEEPER_HOST:9092 --topic inputTopic

============== EMPLOYEE DATA:
{"id":"1","name":"Mark","firstName":"Polosnikov","lastName":"Doyle"}

============== RUN ON SARPK SUBMIT:
/spark/bin/spark-submit --class "com.mypackage.KafkaConsumerNoSjs" --master local[*] /home/job-standalone.jar

============== RUN ON JOBSERVER:
curl --data-binary @/home/job-standalone.jar JOBSERVER_HOST:8090/jars/com.mypackage.KafkaConsumerSjs
curl 'JOBSERVER_HOST:8090/jobs?appName=com.mypackage.KafkaConsumerSjs&classPath=com.mypackage.KafkaConsumerSjs' -d '{}'
