package common.communication;

import java.io.IOException;
import java.net.Socket;

public class ServerCommModule extends CommModule{

    /**
     * Instantiates a server communication module with the right client socket
     * @param clientSocket the socket representing the client
     */
    public ServerCommModule(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * set the output and input stream for the communication module
     * @throws IOException
     */
    public void initializeIO() throws IOException{
        this.output = this.clientSocket.getOutputStream();
        this.input = this.clientSocket.getInputStream();
    }

    /**
     * Close the connection with the client
     * @throws IOException If it cannot be properly closed
     */
    public void closeConnection() throws IOException{
        if (clientSocket != null) {
            input.close();
            output.close();
            clientSocket.close();
        }
    }
}