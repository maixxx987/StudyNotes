# canal

> canal是由阿里巴巴开源的框架，主要用途是基于 MySQL 数据库增量日志解析，提供增量数据订阅和消费



## 工作原理

- canal 模拟 MySQL slave 的交互协议，伪装自己为 MySQL slave ，向 MySQL master 发送dump 协议
- MySQL master 收到 dump 请求，开始推送 binary log 给 slave (即 canal )
- canal 解析 binary log 对象(原始为 byte 流)



## 主要用途

基于日志增量订阅和消费的业务包括

- 数据库镜像
- 数据库实时备份
- 索引构建和实时维护(拆分异构索引、倒排索引等)
- 业务 cache 刷新
- 带业务逻辑的增量数据处理



## 搭建canal

### 配置MySQL

- 修改my.cnf，开启MySQL的binlog，并配置binlog为ROW模式

  ```ini
  [mysqld]
  # 开启 binlog
  log-bin=/var/lib/mysql/mysql-bin 
  # 选择 ROW 模式
  binlog-format=ROW 
  # 配置 MySQL replaction 需要定义，不要和 canal 的 slaveId 重复
  server_id=1 
  ```
  
  改完后重启MySQL，检查binlog是否开启
  
  ```bash
  # 检查binlog是否开启及路径
  mysql> SHOW VARIABLES LIKE 'log_bin%';
  +---------------------------------+--------------------------------+
  | Variable_name                   | Value                          |
  +---------------------------------+--------------------------------+
  | log_bin                         | ON                             |
  | log_bin_basename                | /var/lib/mysql/mysql-bin       |
  | log_bin_index                   | /var/lib/mysql/mysql-bin.index |
  | log_bin_trust_function_creators | OFF                            |
  | log_bin_use_v1_row_events       | OFF                            |
  +---------------------------------+--------------------------------+
  5 rows in set (0.01 sec)
  
  # 检查binlog模式
  mysql> SHOW VARIABLES LIKE 'binlog_format';
  +---------------+-------+
  | Variable_name | Value |
  +---------------+-------+
  | binlog_format | ROW   |
  +---------------+-------+
  1 row in set (0.00 sec)  
  
  # 查看日志文件列表
  mysql> SHOW BINARY LOGS;
  +------------------+-----------+
  | Log_name         | File_size |
  +------------------+-----------+
  | mysql-bin.000001 |       891 |
  | mysql-bin.000002 |      4260 |
  | mysql-bin.000003 |      1641 |
  +------------------+-----------+
  3 rows in set (0.00 sec)
  
  # 查看当前写入日志
  mysql> SHOW master status;
  +------------------+----------+--------------+------------------+-------------------+
  | File             | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set |
  +------------------+----------+--------------+------------------+-------------------+
  | mysql-bin.000003 |     1641 |              |                  |                   |
  +------------------+----------+--------------+------------------+-------------------+
  1 row in set (0.00 sec)
  ```
  
  ​	

- 在MySQL中创建canal账户

  ```sql
  -- 创建用户名canal，密码canal
  CREATE USER canal IDENTIFIED BY 'canal';  
  -- 授权
  GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
  -- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
  FLUSH PRIVILEGES;
  ```




### 配置canal

- 下载[canal-deployer](https://github.com/alibaba/canal/releases)，下载完成后解压缩，进入目录，可看到如下配置

  ``` bash
  total 28
  drwxr-xr-x 2 root root 4096 Jun 28 17:03 bin
  drwxr-xr-x 5 root root 4096 Jun 28 17:03 conf
  drwxr-xr-x 2 root root 4096 Jun 28 17:03 lib
  drwxrwxrwx 2 root root 4096 Jun 13 09:44 logs
  ```



- 打开canal的配置文件conf/canal.properties，需要关注的信息如下

  详细配置解释可参考[Canal配置详解 · 大专栏 (dazhuanlan.com)](https://www.dazhuanlan.com/wendyemir/topics/1422034)

  ```bash
  vim conf/canal.properties
  ```

  ```properties
  # TCPm模式下canal的端口号
  canal.port = 11111
  # 用户名密码
  canal.user = canal
  canal.passwd = 123456
  # canal server模式 tcp, kafka, RocketMQ
  canal.serverMode = tcp
  
  # binlog filter config　　
  #binlog过滤的配置，指定过滤那些SQL
  canal.instance.filter.druid.ddl = true       
  # 是否忽略DCL的query语句，比如grant/create user等，默认false
  canal.instance.filter.query.dcl = false　　
  # 是否忽略DML的query语句，比如insert/update/delete table.(mysql5.6的ROW模式可以包含statement模式的query记录),默认false
  canal.instance.filter.query.dml = false　　
  # 是否忽略DDL的query语句，比如create table/alater table/drop table/rename table/create index/drop index. 
  # (目前支持的ddl类型主要为table级别的操作，create databases/trigger/procedure暂时划分为dcl类型), 默认false
  canal.instance.filter.query.ddl = false
  
  # canal默认的实例名，如果有多个的话，将实例名以逗号分割
  canal.destinations = example,example2
  ```

  如果配置了多个实例，需要在conf下有多份配置，可以复制默认的example，然后修改即可

  ```bash
  cp -R example example2/
  ```

  

- 打开实例的配置文件conf/example/instance.properties，需要修改配置信息如下：

  ```bash
  vim conf/example/instance.properties
  ```
  
  ```properties
  # 数据库地址
  canal.instance.master.address=127.0.0.1:3306
  # binlog日志名称
  canal.instance.master.journal.name=mysql-bin.000003
  # mysql主库链接时起始的binlog偏移量
  # binlog和偏移量也可以不指定，则canal-server会从当前的位置开始读取
  canal.instance.master.position=
  canal.instance.master.timestamp=
  canal.instance.master.gtid=
  
  # 数据库用户名和密码
  canal.instance.dbUsername=canal
  canal.instance.dbPassword=canal
  # 字符集
  canal.instance.connectionCharset = UTF-8
  
  # 这里采用正则表达式，按shcema.table的形式
  # .*\\..*表示监听所有表
  # 如，关注test下的所有表 则test\\..*
  # 多个用逗号分隔
  # 如 test\\..*,test1\\.school,test2\\.class
  canal.instance.filter.regex=.*\\..*
  # mysql 数据解析表的黑名单，多个表用，隔开
  canal.instance.filter.black.regex=
  ```



- 启动canal

  ```bash
  ./bin/startup.sh 
  ```

  

- 查看Server日志

  ```bash
  tail -f logs/canal/canal.log
  ```

  ```log
  2022-06-29 16:08:07.688 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## set default uncaught exception handler
  2022-06-29 16:08:07.719 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## load canal configurations
  2022-06-29 16:08:07.723 [main] INFO  c.a.o.c.d.monitor.remote.RemoteConfigLoaderFactory - ## load local canal configurations
  2022-06-29 16:08:07.731 [main] INFO  com.alibaba.otter.canal.deployer.CanalStater - ## start the canal server.
  2022-06-29 16:08:07.764 [main] INFO  com.alibaba.otter.canal.deployer.CanalController - ## start the canal server[172.24.153.88:11111]
  2022-06-29 16:08:08.661 [main] INFO  com.alibaba.otter.canal.deployer.CanalStater - ## the canal server is running now ......
  ```

  

- 查看 instance 的日志

  ```bash
  tail -f logs/example/example.log
  ```

  ```log
  2022-06-29 16:12:55.772 [main] INFO  c.a.o.c.i.spring.support.PropertyPlaceholderConfigurer - Loading properties file from class path resource [canal.properties]
  2022-06-29 16:12:55.775 [main] INFO  c.a.o.c.i.spring.support.PropertyPlaceholderConfigurer - Loading properties file from class path resource [example/instance.properties]
  2022-06-29 16:12:56.266 [main] INFO  c.a.otter.canal.instance.spring.CanalInstanceWithSpring - start CannalInstance for 1-example 
  2022-06-29 16:12:56.353 [main] INFO  c.a.otter.canal.instance.core.AbstractCanalInstance - start successful....
  ```

  

- 关闭canal server

  ```bash
  ./bin/stop.sh 
  ```

  

## QuickStart

- TODO 测试数据表 

- TODO 测试数据




- 测试代码[官方ClientExample]([ClientExample · alibaba/canal Wiki (github.com)](https://github.com/alibaba/canal/wiki/ClientExample))

  ```java
  import com.alibaba.otter.canal.client.CanalConnector;
  import com.alibaba.otter.canal.client.CanalConnectors;
  import com.alibaba.otter.canal.protocol.CanalEntry.*;
  import com.alibaba.otter.canal.protocol.Message;
  
  import java.net.InetSocketAddress;
  import java.util.List;
  
  public class CanalOfficialDemo {
  
      public static void main(String[] args) {
          // 创建链接(canal server所在的ip，port，实例名字，用户名及密码)
          // 默认实例为example，默认端口为111111，默认没有用户名和密码
          CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress("debian.wsl", 11113),
                  "test1", "canal", "123456");
          try {
              //打开连接
              connector.connect();
              //订阅数据库表,全部表
              connector.subscribe(".*\\..*");
              //回滚到未进行ack的地方，下次fetch的时候，可以从最后一个没有ack的地方开始拿
              connector.rollback();
              // 循环监听MySQL日志
              while (true) {
                  // 获取指定数量的数据
                  Message message = connector.getWithoutAck(1000);
                  // 获取批量ID
                  long batchId = message.getId();
                  // 获取批量的数量
                  int size = message.getEntries().size();
                  if (batchId == -1 || size == 0) {
                      // 如果没有数据
                      try {
                          // 线程休眠2秒
                          Thread.sleep(2000);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  } else {
                      // 如果有数据,处理数据
                      printEntry(message.getEntries());
                  }
                  // 进行 batch id 的确认。确认之后，小于等于此 batchId 的 Message 都会被确认。
                  connector.ack(batchId);
              }
          } catch (Exception e) {
              e.printStackTrace();
          } finally {
              connector.disconnect();
          }
      }
  
      /**
       * 打印canal server解析binlog获得的实体类信息
       */
      private static void printEntry(List<Entry> entrys) {
          for (Entry entry : entrys) {
              if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                  // 开启/关闭事务的实体类型，跳过
                  continue;
              }
              // RowChange对象，包含了一行数据变化的所有特征
              // 比如isDdl 是否是ddl变更操作 sql 具体的ddl sql beforeColumns afterColumns 变更前后的数据字段等等
              RowChange rowChange;
              try {
                  rowChange = RowChange.parseFrom(entry.getStoreValue());
              } catch (Exception e) {
                  throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(), e);
              }
              // 获取操作类型：insert/update/delete类型
              EventType eventType = rowChange.getEventType();
              // 打印Header信息
              System.out.println(String.format("================》; binlog[%s:%s] , name[%s,%s] , eventType : %s",
                      entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                      entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                      eventType));
              // 判断是否是DDL语句
              if (rowChange.getIsDdl()) {
                  System.out.println("================》;isDdl: true,sql:" + rowChange.getSql());
              }
              // 获取RowChange对象里的每一行数据，打印出来
              for (RowData rowData : rowChange.getRowDatasList()) {
                  System.out.println("============= rowData start ============");
                  if (eventType == EventType.DELETE) {
                      // 如果是删除语句
                      System.out.println("type  ---> delete");
                      printColumn(rowData.getBeforeColumnsList());                    
                  } else if (eventType == EventType.INSERT) {
                      // 如果是新增语句
                      System.out.println("type  ---> insert");
                      printColumn(rowData.getAfterColumnsList());                    
                  } else {
                      // 如果是更新的语句
                      System.out.println("type  ---> update");
                      //变更前的数据
                      System.out.println("------->; before");
                      printColumn(rowData.getBeforeColumnsList());
                      //变更后的数据
                      System.out.println("------->; after");
                      printColumn(rowData.getAfterColumnsList());
                  }
                  System.out.println("============= rowData end ============");
              }
          }
      }
  
      private static void printColumn(List<Column> columns) {
          for (Column column : columns) {
              System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
          }
      }
  }
  ```



- TODO 添加一行数据，观察日志


- TODO 更新**两行**数据，观察记录（会输出两行）

- TODO 删除一行记录，观察日志

  

## 基于JFinal的canal demo

### 实现思路

- 将Canal以JFinal插件的形式，集成到JFinal中。由JFinal负责Canal客户端的创建，启动，关闭，且配置内容可写在配置文件，正常开发只需要实现核心业务即可。
- 由于每个Canal都是以循环的形式监听binlog日志，因此需要额外创建一个线程池，专门管理所有的Canal客户端。
- 具体业务实现类通过配置文件中的反射创建，继承ICanalHandler接口，重写新增，修改，删除方法即可。



### JFinal插件相关说明

#### 插件核心代码 CanalPlugin.java 

> 基于JFinal的Canal插件，实现IPlugin接口，在JFinal启动时添加此插件，链接Canal服务。

```java
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.jfinal.kit.Prop;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.IPlugin;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * canal插件
 */
public class CanalPlugin implements IPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(CanalPlugin.class);

  /**
   * canal客户端集合
   */
  private final List<CanalThread> canalThreadList = new ArrayList<>();

  /**
   * 配置文件
   */
  private final Prop configProp;

  /**
   * canal配置前缀
   */
  private static final String CONFIG_PREFIX = "canal.";
  /**
   * canal server 域名
   */
  private static final String HOSTNAME = CONFIG_PREFIX + "hostname";
  /**
   * canal server 端口
   */
  private static final String PORT = CONFIG_PREFIX + "port";
  /**
   * canal server 用户名
   */
  private static final String USERNAME = CONFIG_PREFIX + "username";
  /**
   * canal server 密码
   */
  private static final String PASSWORD = CONFIG_PREFIX + "password";
  /**
   * canal server 实例名
   */
  private static final String DESTINATION = CONFIG_PREFIX + "destination";
  /**
   * 类后缀
   */
  private static final String CLASS_SUFFIX = ".class";
  /**
   * 取固定表，若有这个配置则覆盖了服务端的配置
   */
  private static final String FILTER_SUFFIX = ".filter";
  /**
   * 单次消费数量
   */
  private static final String BATCH_SIZE_SUFFIX = ".batchSize";
  /**
   * 消费间隔
   */
  private static final String INTERVAL_SUFFIX = ".interval";
  /**
   * 默认消费数量
   */
  private static final Integer DEFAULT_BATCH_SIZE = 1000;
  /**
   * 默认获取所有表
   */
  private static final String DEFAULT_FILTER = ".*\\..*";
  /**
   * 默认间隔3秒
   */
  private static final Integer DEFAULT_INTERVAL = 3 * 1000;

  /**
   * Canal客户端线程池
   */
  private ThreadPoolExecutor executor;

  public CanalPlugin(Prop configProp) {
    this.configProp = configProp;
    try {
      addInstance();
    } catch (RuntimeException e) {
      LOG.info("[CanalPlugin#CanalPlugin]connect error ==> {}", e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOG.info("[CanalPlugin#CanalPlugin]connect error ==> {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }


  /**
   * 添加canal server实例配置
   */
  private void addInstance() throws Exception {
    // 获取canal配置文件中的参数
    String hostname = getNotNullConfigValue(HOSTNAME);
    String port = getNotNullConfigValue(PORT);
    String username = getNullConfigValue(USERNAME);
    String password = getNullConfigValue(PASSWORD);
    String destination = getNotNullConfigValue(DESTINATION);

    // 遍历canal实例名
    String[] canalInstanceNameArray = destination.split(",");
    for (String canalInstanceName : canalInstanceNameArray) {
      String canalClassPath = getNotNullConfigValue(CONFIG_PREFIX + canalInstanceName + CLASS_SUFFIX);
      // 反射获取canal处理对象
      Object canalClient = Class.forName(canalClassPath).newInstance();
      if (!(canalClient instanceof ICanalHandler)) {
        throw new IllegalArgumentException("canal客户端必须继承ICanalClient");
      }
      // 过滤表
      String filter = getNullConfigValue(CONFIG_PREFIX + canalInstanceName + FILTER_SUFFIX);
      filter = StrKit.isBlank(filter) ? DEFAULT_FILTER : filter;
      // 单次消费数量
      String batchSizeStr = getNullConfigValue(CONFIG_PREFIX + canalInstanceName + BATCH_SIZE_SUFFIX);
      Integer batchSize = StrKit.isBlank(batchSizeStr) ? DEFAULT_BATCH_SIZE : Integer.parseInt(batchSizeStr);
      // 消费间隔
      String intervalStr = getNullConfigValue(CONFIG_PREFIX + canalInstanceName + INTERVAL_SUFFIX);
      Integer interval = StrKit.isBlank(intervalStr) ? DEFAULT_INTERVAL : Integer.parseInt(intervalStr);
      // 添加canal到实例列表
      canalThreadList.add(new CanalThread(hostname, port, username, password, canalInstanceName, filter, batchSize,
              interval, (ICanalHandler) canalClient));
      LOG.info("[CanalPlugin#addInstance]add canal instance ==> {}", canalInstanceName);
    }

    // 根据实例数量创建对应的线程池(DefaultThreadFactory引用自canal下的netty依赖)
    this.executor = new ThreadPoolExecutor(canalInstanceNameArray.length,
            canalInstanceNameArray.length,
            0,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1),
            new DefaultThreadFactory("canal-pool"));
  }

  /**
   * 获取不为空的配置值
   *
   * @param configKey 配置key
   * @return 配置值
   */
  private String getNotNullConfigValue(String configKey) {
    return getConfigValue(configKey, false);
  }

  /**
   * 获取可以空的配置值
   *
   * @param configKey 配置key
   * @return 配置值
   */
  private String getNullConfigValue(String configKey) {
    return getConfigValue(configKey, true);
  }

  /**
   * 获取配置值
   *
   * @param configKey          配置key
   * @param isConfigValueBlank 配置值是否允许为空
   * @return 配置值
   */
  private String getConfigValue(String configKey, Boolean isConfigValueBlank) {
    String configValue = configProp.get(configKey);
    if (null != configValue) {
      configValue = configValue.trim();
    }
    if (!isConfigValueBlank && StrKit.isBlank(configValue)) {
      throw new IllegalArgumentException("参数名称: " + configKey + " 对应的参数值不能为空");
    }
    return configValue;
  }

  /**
   * 插件启动，每个canal客户端开始监听binlog并消费
   */
  public boolean start() {
    for (CanalThread canalThread : canalThreadList) {
      executor.execute(canalThread);
    }
    return true;
  }

  /**
   * 插件停止，每个canal客户端停止监听binlog，并关闭线程池
   */
  public boolean stop() {
    for (CanalThread canalThread : canalThreadList) {
      canalThread.stop();
    }
    executor.shutdown();
    return true;
  }

  /**
   * canal客户端线程类
   */
  private static class CanalThread implements Runnable {
    /**
     * 实例名
     */
    private final String destination;
    /**
     * canal 链接对象
     */
    private CanalConnector connector;
    /**
     * canal消费间隔
     */
    private final Integer batchSize;
    /**
     * canal消费间隔
     */
    private final Integer interval;
    /**
     * canal客户端
     */
    private final ICanalHandler canalHandler;
    /**
     * 运行状态
     */
    private Boolean isRunning;

    public CanalThread(String hostname, String port, String username, String password, String destination,
                       String filter, Integer batchSize, Integer interval, ICanalHandler canalHandler) {
      // 创建链接
      createConnector(hostname, port, username, password, destination, filter);
      this.destination = destination;
      this.batchSize = batchSize;
      this.interval = interval;
      this.canalHandler = canalHandler;
      this.isRunning = true;
    }

    private void createConnector(String hostname, String port, String username, String password, String destination,
                                 String filter) {
      this.connector = CanalConnectors.newSingleConnector(
              new InetSocketAddress(hostname, Integer.parseInt(port)),
              destination,
              username,
              password);
      // 打开连接
      this.connector.connect();
      // 订阅数据库表
      this.connector.subscribe(filter);
      // 回滚到未进行ack的地方，下次fetch的时候，可以从最后一个没有ack的地方开始拿
      this.connector.rollback();
    }

    @Override
    public void run() {
      LOG.info("[CanalThread#run]start canal instance ==> {}", destination);
      while (isRunning && !Thread.currentThread().isInterrupted()) {
        try {
          // 获取指定数量的数据
          Message message = connector.getWithoutAck(batchSize);
          //获取批量ID
          long batchId = message.getId();
          //获取批量的数量
          int size = message.getEntries().size();
          //如果没有数据
          if (batchId == -1 || size == 0) {
            // 没数据，则睡眠
            Thread.sleep(interval);
          } else {
            //如果有数据,处理数据
            processData(message.getEntries());
          }
          // 进行 batch id 的确认。确认之后，小于等于此 batchId 的 Message 都会被确认。
          connector.ack(batchId);
        } catch (Exception e) {
          LOG.error("[CanalThread#run]canal instance consume error ==> {}", e.getMessage(), e);
        }
      }
      LOG.info("[CanalThread#run]stop canal instance ==> {}", destination);
      // 断开链接
      connector.disconnect();
    }

    /**
     * 处理数据，
     *
     * @param entries 数据表对象
     */
    private void processData(List<CanalEntry.Entry> entries) throws InvalidProtocolBufferException {
      for (CanalEntry.Entry entry : entries) {
        if (CanalEntry.EntryType.TRANSACTIONBEGIN.equals(entry.getEntryType()) || CanalEntry.EntryType.TRANSACTIONEND.equals(entry.getEntryType())) {
          //开启/关闭事务的实体类型，跳过
          continue;
        }
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
          switch (rowChange.getEventType()) {
            case INSERT:
              // 新增
              canalHandler.insert(entry, rowData);
              break;
            case UPDATE:
              // 更新
              canalHandler.update(entry, rowData);
              break;
            case DELETE:
              // 删除
              canalHandler.delete(entry, rowData);
              break;
            default:
              break;
          }
        }
      }
    }

    public void stop() {
      // 停止循环
      isRunning = false;
    }
  }
}
```

配置完后，仅需要在App.java中的configPlugin方法添加canalPlugin即可

```java
// canal
me.add(new CanalPlugin(PropKit.use("canal.properties")));
```





#### 业务处理接口 ICanalHandler.java

> Canal业务处理接口，实现接口重写方法即可编写对应的业务逻辑

```java
import com.alibaba.otter.canal.protocol.CanalEntry;

/**
 * Canal业务处理接口，实现接口重写方法即可编写对应的业务逻辑
 */
public interface ICanalHandler {
    /**
     * 新增方法
     *
     * @param entry   表对象
     * @param rowData 具体数据行
     */
    void insert(CanalEntry.Entry entry, CanalEntry.RowData rowData);

    /**
     * 更新方法
     *
     * @param entry   表对象
     * @param rowData 具体数据行
     */
    void update(CanalEntry.Entry entry, CanalEntry.RowData rowData);

    /**
     * 删除方法
     *
     * @param entry   表对象
     * @param rowData 具体数据行
     */
    void delete(CanalEntry.Entry entry, CanalEntry.RowData rowData);
}
```



#### 配置文件 canal.properties

```properties
# ======= canal服务配置 =======
# canal服务器域名
canal.hostname=debian.wsl
# canal服务器端口
canal.port=11113
# canal用户名
canal.username=canal
# canal密码
canal.password=123456
# canal实例名，多个以逗号分隔
canal.destination=test1,test2


# ======= canal实例配置 =======
# canal处理逻辑类路径，必填，一个实例对应一个类
canal.test1.class=com.ava.thirdparty.canal.impl.LiveCanalHandler
# 监听的表，若配置了这个，则服务端的监听配置失效，多个用逗号分隔
# 与服务器一样，采用正则配置
canal.test1.filter=test.V_PUB_SCHEDULE
# canal单次消费数量
canal.test1.batchSize=1000
# canal消费间隔
canal.test1.interval=2000


# canal处理逻辑类路径，必填，一个实例对应一个类
canal.test2.class=com.ava.thirdparty.canal.impl.LiveCanalHandler2
# 监听的表，若配置了这个，则服务端的监听配置失效，多个用逗号分隔
# 与服务器一样，采用正则配置
canal.test2.filter=test.V_PUB_SCHEDULE_2
# canal单次消费数量
canal.test2.batchSize=1000
# canal消费间隔
canal.test2.interval=2000
```



#### canal工具类

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * canal工具类
 *
 * @author AVA
 */
public class CanalUtil {

    /**
     * 格式化binlog日志
     *
     * @param entry 日志文件对象
     * @return binlog日志信息
     */
    public static String getBinlogInfo(CanalEntry.Entry entry) {
        return String.format(" binlog[%s:%s], name[%s,%s] ",
                entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                entry.getHeader().getSchemaName(), entry.getHeader().getTableName());
    }

    /**
     * 将有变动的行数据转换为Map
     *
     * @param columnList 需要转换的行数据
     */
    public static Map<String, String> transferUpdateRow2Map(List<CanalEntry.Column> columnList) {
        return transferRow2Map(columnList, true);
    }

    /**
     * 将所有行数据转换为Map
     *
     * @param columnList 需要转换的行数据
     */
    public static Map<String, String> transferRow2Map(List<CanalEntry.Column> columnList) {
        return transferRow2Map(columnList, null);
    }

    /**
     * 将行数据转换为Map
     *
     * @param columnList 需要转换的行数据
     * @param isUpdate   是否需要有变动的字段
     */
    public static Map<String, String> transferRow2Map(List<CanalEntry.Column> columnList, Boolean isUpdate) {
        Map<String, String> resultMap = new HashMap<>(columnList.size());
        for (CanalEntry.Column column : columnList) {
            if (null == isUpdate || column.getUpdated()) {
                resultMap.put(column.getName(), column.getValue());
            }
        }
        return resultMap;
    }
}

```



### 具体实现步骤

此处省略部署Canal步骤，和上面配置Canal相同

此处默认已经有了基于JFinal的Canal Plugin

1. 修改配置文件，链接Canal服务
2. 实现ICanalHandler，编写同步课表的具体逻辑



## 遇到的问题及解决方案

### canal消费binlog异常(CanalMetaManagerException: batchId:3 is not the firstly:1)

- 问题日志

  ```log
  com.alibaba.otter.canal.protocol.exception.CanalClientException: something goes wrong with reason: something goes wrong with channel:[id: 0x184cf2a7, /172.31.23.66:49436 => /172.31.23.66:11111], exception=com.alibaba.otter.canal.meta.exception.CanalMetaManagerException: batchId:355 is not the firstly:250
  
          at com.alibaba.otter.canal.client.impl.SimpleCanalConnector.receiveMessages(SimpleCanalConnector.java:245)
          at com.alibaba.otter.canal.client.impl.SimpleCanalConnector.getWithoutAck(SimpleCanalConnector.java:222)
          at com.alibaba.otter.canal.client.impl.SimpleCanalConnector.getWithoutAck(SimpleCanalConnector.java:207)
  ```

- 问题原因

  Canal读取MySQL的时候会记录读取的位置，保存在meta.dat文件中，canal程序长时间停止后又重启，最后一次记录的位置已经在MySQL的bin-log日志中丢失，导致读取不到数据。

- 解决方法

  删除服务端对应实例的meta.dat文件

  ```bash
  rm -rf conf/example/met
  ```

  




## 参考资料

- [alibaba/canal: 阿里巴巴 MySQL binlog 增量订阅&消费组件 (github.com)](https://github.com/alibaba/canal)
- [超详细canal入门，看这篇就够了 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/177001630)
- [Canal——增量同步MySQL数据到ElasticSearch - 曹伟雄 - 博客园 (cnblogs.com)](https://www.cnblogs.com/caoweixiong/p/11825303.html)
- [Canal配置详解 · 大专栏 (dazhuanlan.com)](https://www.dazhuanlan.com/wendyemir/topics/1422034)
