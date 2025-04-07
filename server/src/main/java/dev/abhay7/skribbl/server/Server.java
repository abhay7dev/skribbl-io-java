package dev.abhay7.skribbl.server;

public class Server {
    
    public Server(int PORT, boolean HEADLESS) {
        if(HEADLESS) {
            System.out.println("Running skribbl server on port " + PORT + " in headless mode!");
        }
    }

}
