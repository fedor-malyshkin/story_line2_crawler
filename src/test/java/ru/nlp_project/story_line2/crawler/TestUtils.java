package ru.nlp_project.story_line2.crawler;

import java.io.IOException;
import java.net.ServerSocket;

class TestUtils {

  static int getFreePort() throws IOException {
    ServerSocket serverSocket = new ServerSocket(0);
    int localPort = serverSocket.getLocalPort();
    serverSocket.close();
    return localPort;
  }
}
