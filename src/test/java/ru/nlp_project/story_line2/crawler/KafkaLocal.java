package ru.nlp_project.story_line2.crawler;

import com.google.common.collect.Iterators;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import lombok.Getter;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;
import org.assertj.core.api.Assertions;


public class KafkaLocal {

  private static final int DEFAULT_TIMEOUT_MILLS = 5 * 1_000;
  private static final String DEFAULT_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
  private final Properties initialProps;
  private final int initialPort;
  private final ZooKeeperLocal zookeeperLocal;
  private KafkaServerStartable kafka;

  @Getter
  private int usedPort;
  @Getter
  private String connectionUrl;
  private Properties usedProps;
  @Getter
  private AdminClient adminClient;
  @Getter
  private KafkaConsumer<String, String> consumer;

  public KafkaLocal(int port, Properties properties, ZooKeeperLocal zookeeperLocal) {
    this.initialProps = properties;
    this.initialPort = port;
    this.zookeeperLocal = zookeeperLocal;
  }


  public void stop() {
    adminClient.close();
    consumer.close();
    kafka.shutdown();
  }

  public void start() throws Exception {
    this.usedPort = preparePort();
    Properties props = prepareProps(usedPort, zookeeperLocal.getUsedPort());
    KafkaConfig kafkaConfig = new KafkaConfig(props);
    //start local kafka broker
    kafka = new KafkaServerStartable(kafkaConfig);
    kafka.startup();
    adminClient = createAdminClient();
    consumer = createConsumer();
  }

  private Properties prepareProps(int kafkaUsedPort, int zkUsedPort) throws Exception {
    usedProps = new Properties();
    usedProps.setProperty(KafkaConfig.BrokerIdProp(), String.valueOf(0));
    usedProps.setProperty(KafkaConfig.OffsetsTopicReplicationFactorProp(), String.valueOf(1));
    usedProps.setProperty(KafkaConfig.OffsetsTopicPartitionsProp(), String.valueOf(1));
    usedProps.setProperty(KafkaConfig.TransactionsTopicReplicationFactorProp(), String.valueOf(1));
    usedProps.setProperty(KafkaConfig.AutoCreateTopicsEnableProp(), Boolean.TRUE.toString());
    // The total memory used for log deduplication across all cleaner threads, keep it small to not exhaust suite memory
    usedProps.setProperty(KafkaConfig.LogCleanerDedupeBufferSizeProp(), String.valueOf(1_048_577));

    Path tempDirectory = Files.createTempDirectory("kafka");
    usedProps.setProperty(KafkaConfig.LogDirProp(), tempDirectory.toString());

    if (initialProps != null) {
      initialProps.forEach((k, v) -> usedProps.setProperty((String) k, (String) v));
    }

    this.connectionUrl = "localhost:" + kafkaUsedPort;
    usedProps.setProperty(KafkaConfig.ListenersProp(), "PLAINTEXT://:" + kafkaUsedPort);
    usedProps.setProperty(KafkaConfig.ZkConnectProp(), "localhost:" + zkUsedPort);
    return usedProps;
  }

  private int preparePort() throws IOException {
    if (initialPort != 0) {
      return initialPort;
    }
    return TestUtils.getFreePort();
  }


  private AdminClient createAdminClient() {
    Properties props = new Properties();
    props.put("bootstrap.servers", getConnectionUrl());
    return AdminClient.create(props);
  }


  private KafkaConsumer<String, String> createConsumer() {
    Properties props = new Properties();
    props.put("bootstrap.servers", getConnectionUrl());
    props.put("group.id", "test-consumer");
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", "100");
    props.put("key.deserializer", DEFAULT_DESERIALIZER);
    props.put("value.deserializer", DEFAULT_DESERIALIZER);
    return new KafkaConsumer<>(props);
  }

  private Set<String> getTopics() {
    ListTopicsResult listTopicsResult = adminClient.listTopics();
    KafkaFuture<Set<String>> names = listTopicsResult.names();
    return new TryCatchCaller<>(() -> names.get(DEFAULT_TIMEOUT_MILLS, TimeUnit.MILLISECONDS)).call();
  }

  public void deleteTopic(String topic) {
    deleteTopics(Collections.singletonList(topic));
  }

  public void deleteAllTopics() {
    deleteTopics(getTopics());
  }

  private void deleteTopics(Collection<String> topics) {
    DeleteTopicsResult deleteTopics = adminClient.deleteTopics(topics);
    KafkaFuture<Void> all = deleteTopics.all();
    new TryCatchCaller<>(() -> all.get(DEFAULT_TIMEOUT_MILLS, TimeUnit.MILLISECONDS)).call();
  }


  public ConsumerRecord<String, String> getMessageFromTopic(String topic) {
    return getMessageFromTopic(topic, false);
  }

  public ConsumerRecord<String, String> getMessageFromTopic(String topic, boolean seekToStart) {
    return getMessageFromTopic(topic, seekToStart, DEFAULT_TIMEOUT_MILLS);
  }

  public ConsumerRecord<String, String> getMessageFromTopic(String topic, boolean seekToStart, int mills) {
    consumer.subscribe(Collections.singletonList(topic));
    if (seekToStart) {
      consumer.seekToBeginning(Collections.emptyList());
    }
    ConsumerRecords<String, String> consumerRecords = consumer.poll(duration(mills));
    Assertions.assertThat(consumerRecords.count()).isGreaterThan(0);
    Iterable<ConsumerRecord<String, String>> records = consumerRecords.records(topic);
    return Iterators.get(records.iterator(), 0);
  }

  private Duration duration(int mills) {
    return Duration.ofMillis(mills);
  }

  public void createTopic(String topic) {
    NewTopic newTopic = new NewTopic(topic, 1, (short) 1);
    CreateTopicsResult createTopicsResult = adminClient.createTopics(Arrays.asList(newTopic));
    new TryCatchCaller<>(createTopicsResult::all).call();
  }


  @FunctionalInterface
  public interface SupplierWithException<T> {

    T get() throws ExecutionException, InterruptedException, TimeoutException;
  }


  static class TryCatchCaller<T> {

    private final SupplierWithException<T> sp;

    TryCatchCaller(SupplierWithException<T> sp) {
      this.sp = sp;
    }

    T call() {
      try {
        return sp.get();
      } catch (InterruptedException | ExecutionException e) {
        Assertions.fail(e.getMessage());
      } catch (TimeoutException e) {
        Assertions.fail("Timeout:" + e.getMessage());
      }
      return null;
    }
  }

}