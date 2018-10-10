package ru.nlp_project.story_line2.crawler;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import lombok.Getter;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

public class ZooKeeperLocal {

  private static final String ZOO_PROP_TICK_TIME = "tickTime";
  private static final String ZOO_PROP_DATA_DIR = "dataDir";
  private static final String ZOO_PROP_CLIENT_PORT = "clientPort";
  private final Properties initialProps;
  private final int initialPort;

  private ZooKeeperServerMain zooKeeperServer;
  private Thread zooKeeperServerThread;
  @Getter
  private int usedPort;


  public ZooKeeperLocal(int port, Properties properties) {
    this.initialProps = properties;
    this.initialPort = port;
  }

  public void start() throws Exception {
    this.usedPort = preparePort();
    Properties props = prepareProps(usedPort);
    QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
    quorumConfiguration.parseProperties(props);
    zooKeeperServer = new ZooKeeperServerMain();
    final ServerConfig configuration = new ServerConfig();
    configuration.readFrom(quorumConfiguration);

    zooKeeperServerThread = new Thread() {
      public void run() {
        try {
          zooKeeperServer.runFromConfig(configuration);
        } catch (IOException e) {
          System.out.println("ZooKeeper Failed");
          e.printStackTrace(System.err);
        }
      }
    };
    zooKeeperServerThread.start();
  }

  private Properties prepareProps(int port) throws IOException {
    Properties properties = new Properties();
    properties.setProperty(ZOO_PROP_TICK_TIME, String.valueOf(1000));

    Path tempDirectory = Files.createTempDirectory("zoo");
    properties.setProperty(ZOO_PROP_DATA_DIR, tempDirectory.toString());

    if (initialProps != null) {
      initialProps.forEach((k, v) -> properties.setProperty((String) k, (String) v));
    }

    properties.setProperty(ZOO_PROP_CLIENT_PORT, String.valueOf(port));
    return properties;
  }

  private int preparePort() throws IOException {
    if (initialPort != 0) {
      return initialPort;
    }
    return TestUtils.getFreePort();
  }



  public void stop() {
    zooKeeperServerThread.interrupt();
  }
}