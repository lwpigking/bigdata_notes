package Calculation

import java.sql.{Connection, DriverManager, PreparedStatement}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import ru.yandex.clickhouse.{ClickHouseConnection, ClickHouseDataSource}

import java.util.Properties

/**
 * Project:  Bigdata
 * Create date:  2023/4/13
 * Created by fujiahao
 */
class ClickHouseSink(url: String, username: String, password: String) extends RichSinkFunction[(String, Int)]{
  private var connection: ClickHouseConnection = _

  private val properties: Properties = new Properties()
  properties.put("username", username)
  properties.put("password", password)

  override def open(parameters: Configuration): Unit = {
    val dataSource: ClickHouseDataSource = new ClickHouseDataSource(url, properties)
    connection = dataSource.getConnection
  }

  override def invoke(value: (String, Int), context: SinkFunction.Context): Unit = {

    val statement: PreparedStatement = connection.prepareStatement(
      "insert into station_flow (station, flow) values (?, ?)"
    )

    statement.setString(1, value._1)
    statement.setInt(2, value._2)
    statement.executeUpdate()
    statement.close()
  }

  override def close(): Unit = {
    if (connection != null) {
      connection.close()
    }
  }

}




