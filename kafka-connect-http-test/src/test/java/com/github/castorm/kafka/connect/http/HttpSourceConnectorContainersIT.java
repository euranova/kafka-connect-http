package com.github.castorm.kafka.connect.http;

/*-
 * #%L
 * kafka-connect-http
 * %%
 * Copyright (C) 2020 CastorM
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.castorm.kafka.connect.ConnectorUtils;
import com.github.castorm.kafka.connect.Infrastructure;
import com.github.castorm.kafka.connect.KafkaClient;
import com.github.castorm.kafka.connect.KafkaConnectClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
class HttpSourceConnectorContainersIT {

    @RegisterExtension
    Infrastructure infra = new Infrastructure().start();

    KafkaConnectClient kafkaConnectClient = new KafkaConnectClient(infra.getKafkaConnectExternalUrl());

    KafkaClient kafkaClient = new KafkaClient(infra.getKafkaBootstrapServers());

    @Test
    @Timeout(60)
    void whenConnector1_thenRecordsInTopic() {

        String configJson = ConnectorUtils.replaceVariables(ConnectorUtils.readConnectorConfig("connectors/connector1.json"), ImmutableMap.of("server.url", infra.getWiremockInternalUrl()));

        Map<String, String> config = kafkaConnectClient.createConnector(configJson);

        Assertions.assertThat(kafkaClient.observeTopic(config.get("kafka.topic"))
                .take(2)
                .doOnNext(record -> log.info("{} {} {}", record.timestamp(), record.key(), record.value()))
                .collect(toList())
                .blockingGet())
                .extracting(ConsumerRecord::key)
                .containsExactly("Struct{key=TICKT-0002}", "Struct{key=TICKT-0003}");
    }
}
