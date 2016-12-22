package com.mypackage.executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.FlatMapFunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParseJsonExecutor<I> implements FlatMapFunction<Iterator<String>, I>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8843431558342910612L;
	private Class<?> inputClass;
	final static Logger logger = Logger.getLogger(ParseJsonExecutor.class);

	public ParseJsonExecutor(Class<?> inputClass) {
		super();
		this.inputClass = inputClass;
	}

	@Override
	public Iterable<I> call(Iterator<String> it) throws Exception {

		logger.info("<o> ---> parse executor : ");

		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
		List<I> l = new ArrayList<I>();
		while (it.hasNext()) {
			try {
				@SuppressWarnings("unchecked")
				String employe = it.next();
				logger.info("<o> ---> employe  : " + employe);
				I employee = (I) gson.fromJson(employe, inputClass);
				logger.info("<o> PARSING MESSAGE FROM TOPIC");
				l.add(employee);
			} catch (Exception e) {
				logger.error("Error parsing json", e);
			}
		}
		return l;
	}

}
