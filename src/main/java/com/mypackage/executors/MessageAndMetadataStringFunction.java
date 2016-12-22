package com.mypackage.executors;

import consumer.kafka.MessageAndMetadata;
import org.apache.spark.api.java.function.FlatMapFunction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MessageAndMetadataStringFunction implements FlatMapFunction<Iterator<MessageAndMetadata>, String> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8982218689518850837L;

	@Override
	public Iterable<String> call(Iterator<MessageAndMetadata> it) throws Exception {

		List<String> data = new ArrayList<String>();
		while (it.hasNext()) {
			String payload = new String(it.next().getPayload());
			//System.out.println( "<o> " + payload);
			data.add(payload);
		}
		System.out.println ( "<o> Size::" + data.size() );
		return data;

	}

}