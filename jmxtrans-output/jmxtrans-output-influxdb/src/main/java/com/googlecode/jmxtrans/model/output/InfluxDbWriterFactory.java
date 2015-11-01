/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.OutputWriter;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.output.support.ResultTransformerOutputWriter;
import org.apache.commons.lang.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

import static com.google.common.collect.Sets.immutableEnumSet;

public class InfluxDbWriterFactory implements OutputWriterFactory {

	private static final Logger LOG = LoggerFactory.getLogger(InfluxDbWriterFactory.class);

	/**
	 * The deault <a href=
	 * "https://influxdb.com/docs/v0.9/concepts/key_concepts.html#retention-policy">
	 * The retention policy</a> for each measuremen where no retentionPolicy
	 * setting is provided in the json config
	 */
	private static final String DEFAULT_RETENTION_POLICY = "default";

	private final String database;
	private final InfluxDB.ConsistencyLevel writeConsistency;

	private final String retentionPolicy;
	private final InfluxDB influxDB;
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;
	private final boolean booleanAsNumber;

	/**
	 * @param typeNames
	 * @param booleanAsNumber
	 * @param debugEnabled
	 * @param url
	 *            - The url e.g http://localhost:8086 to InfluxDB
	 * @param username
	 *            - The username for InfluxDB
	 * @param password
	 *            - The password for InfluxDB
	 * @param database
	 *            - The name of the database (created if does not exist) on
	 *            InfluxDB to write the measurements to
	 */
	@JsonCreator
	public InfluxDbWriterFactory(@JsonProperty("typeNames") ImmutableList<String> typeNames,
			@JsonProperty("booleanAsNumber") boolean booleanAsNumber, @JsonProperty("debug") Boolean debugEnabled,
			@JsonProperty("url") String url, @JsonProperty("username") String username,
			@JsonProperty("password") String password, @JsonProperty("database") String database,
			@JsonProperty("writeConsistency") String writeConsistency,
			@JsonProperty("retentionPolicy") String retentionPolicy,
			@JsonProperty("resultTags") List<String> resultTags) {
		this.booleanAsNumber = booleanAsNumber;
		this.database = database;

		this.writeConsistency = StringUtils.isNotBlank(writeConsistency)
				? InfluxDB.ConsistencyLevel.valueOf(writeConsistency) : InfluxDB.ConsistencyLevel.ALL;

		this.retentionPolicy = StringUtils.isNotBlank(retentionPolicy) ? retentionPolicy : DEFAULT_RETENTION_POLICY;

		this.resultAttributesToWriteAsTags = initResultAttributesToWriteAsTags(resultTags);

		LOG.debug("Connecting to url: {} as: username: {}", url, username);

		influxDB = InfluxDBFactory.connect(url, username, password);
	}

	private ImmutableSet<ResultAttribute> initResultAttributesToWriteAsTags(List<String> resultTags) {
		EnumSet<ResultAttribute> resultAttributes = EnumSet.noneOf(ResultAttribute.class);
		if (resultTags != null) {
			for (String resultTag : resultTags) {
				resultAttributes.add(ResultAttribute.fromAttribute(resultTag));
			}
		} else {
			resultAttributes = EnumSet.allOf(ResultAttribute.class);
		}

		ImmutableSet<ResultAttribute> result = immutableEnumSet(resultAttributes);
		LOG.debug("Result Tags to write set to: {}", result);
		return result;
	}

	@Override
	public OutputWriter create() {
		return ResultTransformerOutputWriter.booleanToNumber(booleanAsNumber, new InfluxDbWriter(influxDB, database,
				writeConsistency, retentionPolicy, resultAttributesToWriteAsTags));
	}
}