package SimpleLoadBalancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

public class RequestHandler extends Thread{
    private final Socket clientSocket;
    private final String backendServer;
    public RequestHandler(Socket clientSocket, String backendServer) {
        this.clientSocket = clientSocket;
        this.backendServer = backendServer;
    }

    @Override
    public void run() {
        try {
            // Read the client's request
            InputStream clientInput = clientSocket.getInputStream();
            byte[] request = new byte[4096];
            int  bytesRead = clientInput.read(request);

            // Parse the client's request to extract the requested path
            String requestString = new String(request, 0, bytesRead);
            System.out.println("Request from client: " + requestString);
            String[] requestLines = requestString.split("\n");
            String[] requestTokens = requestLines[0].split(" ");
            String requestUri = requestTokens[1];

            // Build the new URL for the backend server
            URL url = new URL("http://" + backendServer + requestUri);

            // Connect to the backend server
            try (Socket serverSocket = new Socket(url.getHost(), url.getPort())) {
                // Forward the client's request to the backend server
                OutputStream serverOutput = serverSocket.getOutputStream();
                serverOutput.write(request, 0, bytesRead);

                // Read the backend server's response
                InputStream serverInput = serverSocket.getInputStream();
                byte[] response = new byte[4096];
                int serverBytesRead = serverInput.read(response);
                clientSocket.getOutputStream().write(response);

                // Forward the backend server's response to the client
                System.out.println("Response from backend server: " + new String(response, 0, serverBytesRead));
                OutputStream clientOutput = clientSocket.getOutputStream();
                clientOutput.write(response, 0, serverBytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Close clientSocket to release resources
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
