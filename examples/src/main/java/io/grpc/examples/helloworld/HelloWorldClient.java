/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldClient.class);

  private final ManagedChannel channel;
  private final GreeterGrpc.GreeterBlockingStub blockingStub;
  private final GreeterGrpc.GreeterStub asyncStub;
  private final GreeterGrpc.GreeterFutureStub futureStub;

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public HelloWorldClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build());
  }

  /** Construct client for accessing HelloWorld server using the existing channel. */
  HelloWorldClient(ManagedChannel channel) {
    this.channel = channel;
    blockingStub = GreeterGrpc.newBlockingStub(channel);
    asyncStub = GreeterGrpc.newStub(channel);
    futureStub = GreeterGrpc.newFutureStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /** Say hello to server. */
  public void greet(String name) {
    logger.info("Will try to greet {} ...", name);
    HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    HelloReply response;
    try {
      response = blockingStub.sayHello(request);
    } catch (StatusRuntimeException e) {
      logger.warn("RPC failed: {}", e.getStatus());
      return;
    }
    logger.info("Greeting: {}", response.getMessage());
  }

  public void greetAsync(String name) {
    logger.info("Will try to greet {} ... (async)", name);
    final HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    try {
      StreamObserver<HelloReply> helloReplyStreamObserver = new StreamObserver<HelloReply>() {
        @Override
        public void onNext(HelloReply value) {
          logger.info("Greeting: {}", value.getMessage());
        }

        @Override
        public void onError(Throwable t) {
          logger.error("", t);
        }

        @Override
        public void onCompleted() {
          logger.info("Greeting completed");
        }
      };
      asyncStub.sayHello(request, helloReplyStreamObserver);
    } catch (StatusRuntimeException e) {
      logger.warn("RPC failed: {}", e.getStatus());
    }
  }

  public void greetFuture(String name) throws ExecutionException, InterruptedException {
    logger.info("Will try to greet {} ... (future)", name);
    final HelloRequest request = HelloRequest.newBuilder().setName(name).build();
    final ListenableFuture<HelloReply> helloReplyListenableFuture = futureStub.sayHello(request);
    final HelloReply helloReply = helloReplyListenableFuture.get();
    logger.info("Greeting: {}", helloReply.getMessage());
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws Exception {
    // Access a service running on the local machine on port 50051
    HelloWorldClient client = new HelloWorldClient("localhost", 50051);
    try {
      String user = "world";
      // Use the arg as the name to greet if provided
      if (args.length > 0) {
        user = args[0];
      }
      client.greetAsync(user + " async");
      client.greetFuture(user + " future");
      client.greet(user + " blocking");
    } finally {
      client.shutdown();
    }
  }
}
