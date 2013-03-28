/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.hivertica;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.MetaException;

import static org.apache.hadoop.hive.serde.serdeConstants.FIELD_DELIM;

public class VerticaExporter {
	private static final Log LOG =
		LogFactory.getLog(VerticaExporter.class);

	// TODO: Move into xml configuration
	private static final String WEBHDFS_URI = "http://localhost:50070/webhdfs/v1";

	// TODO: Add other types that need converting between Hive and Vertica
	private static final Map<String, String> _typesMap =
		new HashMap<String, String>() {{
			put("string", "varchar(65000)");
		}};

	public boolean useTempTables = false;
	private HiveConf hiveConf;

	public VerticaExporter(HiveConf hiveConf) {
		this.hiveConf = hiveConf;
	}

	public String emitTable(String db, String tbl) throws ExporterException {
		HiveMetaStoreClient metastore;
		StorageDescriptor sd;
		try {
			metastore = new HiveMetaStoreClient(hiveConf);
			sd = metastore.getTable(db, tbl).getSd();
		} catch (Exception e) {
			throw new ExporterException("Error reading metastore", e);
		}

		if (!sd.getInputFormat().equals("org.apache.hadoop.mapred.TextInputFormat")) {
			throw new ExporterException("Only text format is exportable to Vertica");
		}

		StringBuilder s = new StringBuilder();
		// Vertica only supports webhdfs
		URI loc;
		try {
			loc = new URI(sd.getLocation());
		} catch (URISyntaxException e) {
			throw new ExporterException("Error reading storage location", e);
		}
		String location = WEBHDFS_URI + loc.getPath();
		String delim = sd.getSerdeInfo().getParameters().get(FIELD_DELIM);

		s.append("CREATE EXTERNAL TABLE ");
		s.append(db);
		s.append(".");
		s.append(tbl);
		s.append(" (");

		List<String> fields = new LinkedList<String>();
		try {
			for (FieldSchema f : metastore.getFields(db, tbl)) {
				StringBuilder fs = new StringBuilder();
				String type = _typesMap.get(f.getType());
				fs.append(f.getName());
				fs.append(" ");
				if (type != null) {
					fs.append(type);
				} else {
					fs.append(f.getType());
				}
				fields.add(fs.toString());
			}
		} catch (TException e) {
			throw new ExporterException("Error reading metastore", e);
		}
		s.append(StringUtils.join(fields, ", "));
		s.append(") AS COPY WITH SOURCE Hdfs(url='");
		s.append(location);
		s.append("') DELIMITER '");
		s.append(delim);
		s.append("';\n");

		return s.toString();
	}

	public String emitDB(String db) throws ExporterException {
		HiveMetaStoreClient metastore;
		try {
			metastore = new HiveMetaStoreClient(hiveConf);
		} catch (MetaException e) {
			throw new ExporterException("Error reading metastore", e);
		}
		StringBuilder s = new StringBuilder();
		s.append("CREATE SCHEMA ");
		s.append(db);
		s.append(";\n");
		try {
			for (String tbl : metastore.getAllTables(db)) {
				s.append(emitTable(db, tbl));
			}
		} catch (MetaException e) {
			throw new ExporterException("Error reading metastore", e);
		}
		return s.toString();
	}

	public String emitAll() throws ExporterException {
		HiveMetaStoreClient metastore;
		try {
			metastore = new HiveMetaStoreClient(hiveConf);
		} catch (MetaException e) {
			throw new ExporterException("Error reading metastore", e);
		}
		StringBuilder s = new StringBuilder();
		try {
			for (String db : metastore.getAllDatabases()) {
				s.append(emitDB(db));
			}
		} catch (MetaException e) {
			throw new ExporterException("Error reading metastore", e);
		}
		return s.toString();
	}
}
