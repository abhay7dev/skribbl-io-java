package dev.abhay7.skribbl.server;

public class Main {
    
    private static int PORT = 80;
    private static boolean HEADLESS = false;

    public static void main(String... args) {
        if(args.length == 1) {
            if(args[0].equals("--headless")) HEADLESS = true;
            else if(args[0].equals("--help")) System.out.println("Use --headless to run in headless mode. Runs on port 80.");
            else System.err.println("Unknown argument: " + args[0]);
        } else if(args.length > 1) {
            System.err.println("Disallowed number of arguments (" + args.length + ")");
        }
        if(HEADLESS) System.out.println("Skribbl Server running on PORT " + PORT + "!");
    }

}
