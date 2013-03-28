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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.common.LogUtils;
import org.apache.hadoop.hive.common.LogUtils.LogInitializationException;
import static org.apache.hadoop.hive.serde.serdeConstants.FIELD_DELIM;

public class MetaStoreExportTool {
     private static final Log LOG =
          LogFactory.getLog(MetaStoreExportTool.class.getName()); 

     // TODO: Add other types that need converting between Hive and Vertica
     private static final Map<String, String> _typesMap =
          new HashMap<String, String>() {{
               put("string", "varchar(65000)");
          }};

     public static void main(String[] args) throws Exception {
          boolean logInitFailed = false;
          String logInitDetailMessage;
          try {
               logInitDetailMessage = LogUtils.initHiveLog4j();
          } catch (LogInitializationException e) {
               logInitFailed = true;
               logInitDetailMessage = e.getMessage();
          }

          System.err.println(logInitDetailMessage);

          HiveConf hiveConf = new HiveConf(MetaStoreExportTool.class);
					VerticaExporter exporter = new VerticaExporter(hiveConf);
          System.out.println(exporter.emitAll());
     }
}
