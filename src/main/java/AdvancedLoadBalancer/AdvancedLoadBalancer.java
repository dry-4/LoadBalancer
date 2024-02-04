package AdvancedLoadBalancer;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

// Load balancer with round-robin algorithm, AtomicInteger and ReentrantLock

/**
 Highlights:
 The AtomicInteger is used to ensure atomic increment operations for the currentIndex, preventing race conditions in the round-robin selection.

 A ReentrantLock is used to create a mutual exclusion (mutex) around the critical section where the currentIndex is incremented, ensuring that only one thread can modify it at a time.

 The java.net.Proxy class is used for creating a reverse proxy by forwarding requests to backend servers.
**/

public class AdvancedLoadBalancer {
    private final List<String> backendServers;
    private final AtomicInteger currentIndex;
    private final ReentrantLock lock;

    public AdvancedLoadBalancer(List<String> backendServers) {
        this.backendServers = backendServers;
        this.currentIndex = new AtomicInteger(0);
        this.lock = new ReentrantLock();
    }

    public void start(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Load Balancer started on port " + port);
            while (true) {
                Socket clientSocket = socket.accept();
                String backendServer = getBackendServer();
                System.out.println("Redirecting request to backend server: " + backendServer);
                new RequestHandler(clientSocket, backendServer).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getBackendServer() {
        lock.lock();
        try {
            int currentIndex = this.currentIndex.getAndIncrement() % backendServers.size();
            return backendServers.get(currentIndex);
        } finally {
            lock.unlock();
        }
    }

    private static class RequestHandler extends Thread {
        private final Socket clientSocket;
        private final String backendServer;

        public RequestHandler(Socket clientSocket, String backendServer) {
            this.clientSocket = clientSocket;
            this.backendServer = backendServer;
        }

        @Override
        public void run() {
            try {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(backendServer.split(":")[0],
                        Integer.parseInt(backendServer.split(":")[1])));
                Socket backendSocket = new Socket(proxy);
                backendSocket.connect(new InetSocketAddress(backendServer.split(":")[0],
                        Integer.parseInt(backendServer.split(":")[1])));

                // Implement request forwarding logic here
                // You can use InputStream/OutputStream or other appropriate classes for data transfer
                // For example, you can use BufferedReader/PrintWriter for line-oriented I/O
                // You can also use BufferedInputStream/BufferedOutputStream for binary data transfer

                new Thread(() -> {
                    try {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = clientSocket.getInputStream().read(buffer)) != -1) {
                            backendSocket.getOutputStream().write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                // Don't forget to close the sockets
                clientSocket.close();
                backendSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        AdvancedLoadBalancer loadBalancer = new AdvancedLoadBalancer(List.of("localhost:3000", "localhost:3001", "localhost:3002", "localhost:3003", "localhost:3004"));
        loadBalancer.start(8080);
    }

}
