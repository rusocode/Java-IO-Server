# ChatIO
TCP communication *with blocking I/O* between client/server where **one connection is handled by one thread**.

This works on a local network, and to test them with many clients it is necessary to run them on a virtual machine.

## The Server Loop
A singlethreaded server is not the most optimal design for a server, but the code illustrates the life cycle of a 
server very well.

```java
while (!isStopped()) {

  console.append("Waiting for a connection on the port " + server.getLocalPort() + "...\n");

  acceptClientRequest();
  
  processClientRequest();

}
```
In short, what the server does is this:
1. Wait for a client request
2. Process client request
3. Repeat from 1.

## Multithreaded-Server
Rather than processing the incoming requests in the same thread that accepts the client connection, the connection is handed off to a worker thread that will process the request.

```java
while (!isStopped()) {

  console.append("Waiting for a connection on the port " + server.getLocalPort() + "...\n");

  acceptClientRequest();

  new Thread(new WorkerRunnable(socketIn)).start();

}
```
That way the thread listening for incoming requests spends as much time as possible in the serverSocket.accept() call. That way the risk is minimized for clients being denied access to the server because the listening thread is not inside the accept() call.

## [IO vs NIO](http://tutorials.jenkov.com/java-nio/nio-vs-io.html#pageToc)
In actual Java on Linux measurements, multiprocessing of classic I/O designs outperforms NIO by about 30% (see "Java Network Programming Book").

Java NIO outperforms a classic I/O design only if:
1. You have a huge number of clients > 20,000.
2. The interval between data packets to send is very short.<br>

**Conclusion**: You need a server for > 20,000 clients with high frequency communication.
