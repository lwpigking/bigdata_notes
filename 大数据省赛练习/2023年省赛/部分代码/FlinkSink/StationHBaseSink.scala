package Calculation

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.hadoop.conf
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants, TableName}

/**
 * Project:  BigdataCore
 * Create date:  2023/4/20
 * Created by fujiahao
 */
class StationHBaseSink extends RichSinkFunction[(String, String, Long)]{
  var conn: Connection = null

  override def open(parameters: Configuration): Unit = {
    val config: conf.Configuration = HBaseConfiguration.create
    config.set(HConstants.ZOOKEEPER_QUORUM,"master,slave1,slave2")
    config.set(HConstants.ZOOKEEPER_CLIENT_PORT,"2181")
    conn = ConnectionFactory.createConnection(config)
  }

  override def invoke(value: (String, String, Long), context: SinkFunction.Context): Unit = {
    val tableName: String = "station_flow"
    val rowKey: String = value._1
    val station: String = value._2
    val flow: Long = value._3
    val table: Table = conn.getTable(TableName.valueOf(tableName))
    val put: Put = new Put(Bytes.toBytes(rowKey))
    put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("station"), Bytes.toBytes(station))
    put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("flow"), Bytes.toBytes(flow.toString))
    table.put(put)
  }

  override def close(): Unit = {
    if (conn != null) {
      conn.close()
    }
  }
}