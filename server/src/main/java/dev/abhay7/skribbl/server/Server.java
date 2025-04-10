package dev.abhay7.skribbl.server;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.abhay7.skribbl.server.datapacks.ClientVerificationPack;
import dev.abhay7.skribbl.server.datapacks.DataPackage;

public class Server {

    private ServerSocket serverSocket;
    private boolean isRunning;

    private CopyOnWriteArrayList<Thread> connectedClientList = new CopyOnWriteArrayList<Thread>();
    private CopyOnWriteArrayList<ClientCommsHandler> connectedClientListRunnables = new CopyOnWriteArrayList<ClientCommsHandler>();

    public Server(int PORT) {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Skribbl Server running on port " + PORT + "!");
        } catch (Exception e) {
            System.err.println("Fatal Error. Failed to open ServerSocket on port " + PORT + "\n" + e);
        }
        runServer();
    }

    public void runServer() {
        while (isRunning) {

            Socket clientSocket = null;
            ObjectOutputStream writer = null;
            ObjectInputStream reader = null;

            try {
                clientSocket = serverSocket.accept();
                writer = new ObjectOutputStream(clientSocket.getOutputStream());
                reader = new ObjectInputStream(clientSocket.getInputStream());
            } catch (Exception e) {
                System.err.println("Failed to accept client socket.");
            }

            if (clientSocket != null && writer != null && reader != null) {
                System.out.println("New user connected with following Inet Address: " + clientSocket.getInetAddress());

                ClientCommsHandler cch = new ClientCommsHandler(clientSocket, writer, reader);
                Thread userThread = new Thread(cch);
                cch.setThisRunnableWrapper(userThread);
                connectedClientListRunnables.add(cch);
                connectedClientList.add(userThread);
                userThread.start();
            }

        }
    }

    private class ClientCommsHandler implements Runnable {

        private Socket clientSocket;
        private ObjectOutputStream writer;
        private ObjectInputStream reader;
        private Thread thisRunnableWrapper;

        private boolean verified = false;

        private ClientCommsHandler(Socket cs, ObjectOutputStream writer, ObjectInputStream reader) {
            this.clientSocket = cs;
            this.writer = writer;
            this.reader = reader;
        }

        // TODO, handle errors/disconnects, etc
        @Override
        public void run() {
            ClientVerificationPack cvp = new ClientVerificationPack(Math.random() + "");
            
            int waittime = 10000;
            String verifyString = "_VERIFIEDCONNECTION";

            try {

                writer.writeObject(cvp);
                writer.flush();
                
                this.clientSocket.setSoTimeout(waittime);
                ClientVerificationPack responseVerification = (ClientVerificationPack) this.reader.readObject();

                if(!responseVerification.getVerificationString().equals(cvp.getVerificationString() + verifyString)) throw new Exception("Invalid verification response");
                else {
                    System.out.println("Client successfully verified");
                    this.verified = true;
                }

            } catch(Exception ste) {
                System.out.println("A client failed to respond to verification packet in time, will be removed.");
                this.disconnectAndTerminateUser();
            }

            DataPackage receivedClientData;
            
            try {
                
                while(((receivedClientData = (DataPackage) reader.readObject())) != null) {
                    handleDataPackage(receivedClientData);
                }

            } catch(EOFException e) {
                System.out.println("Client socket disconnected");
            } catch(Exception e) {
                System.out.println("Error in recieving client data: " + e);
            } finally {
                this.disconnectAndTerminateUser();
            }

        }

        private void setThisRunnableWrapper(Thread t) {
            this.thisRunnableWrapper = t;
        }

        private void disconnectAndTerminateUser() {
            try {
                this.writer.close();
                this.reader.close();
                this.clientSocket.close();
                if(this.thisRunnableWrapper != null) {
                    connectedClientList.remove(this.thisRunnableWrapper);
                    connectedClientListRunnables.remove(this);
                    thisRunnableWrapper.join();
                }
            } catch(Exception e) {
                System.out.println("Failed to disconnect and terminate a user");
            }
        }

    }

    private void handleDataPackage(DataPackage dataPackage) {
        System.out.println("Received Data Package: " + dataPackage);
    }

}
