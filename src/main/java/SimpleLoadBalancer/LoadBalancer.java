package SimpleLoadBalancer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

// SimpleLoadBalancer is a simple implementation of a load balancer that distributes,
// incoming requests to a list of backend servers.
public class LoadBalancer {

    private final List<String> backendServers;
    private int currentServerIndex;

    public LoadBalancer(List<String> backendServers) {
        this.backendServers = backendServers;
        this.currentServerIndex = 0;
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Load Balancer started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String backendServer = getNextBackendServer();
                System.out.println("Redirecting request to backend server: " + backendServer);
                new RequestHandler(clientSocket, backendServer).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized String getNextBackendServer() {
        String backendServer = backendServers.get(currentServerIndex);
        currentServerIndex = (currentServerIndex + 1) % backendServers.size();
        return backendServer;
    }

    public static void main(String[] args) {
        LoadBalancer loadBalancer = new LoadBalancer(List.of("localhost:3000", "localhost:3001", "localhost:3002", "localhost:3003", "localhost:3004"));
        loadBalancer.start(8080);
    }
}
