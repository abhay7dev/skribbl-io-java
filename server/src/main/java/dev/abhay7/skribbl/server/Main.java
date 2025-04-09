package dev.abhay7.skribbl.server;

public class Main {

    public static void main(String... args) {
        
        int PORT = 80;

        if(args.length == 1) {
            if(args[0].equals("--help")) System.out.println("Use --port <PORT> to specify port to run on. Runs on port 80 by default.");
            else if(args[0].equals("--port")) System.out.println("Use --port <PORT> to specify port to run on.");
            else {
                System.err.println("Unknown argument: " + args[0] + ". Use --help for help.");
                System.exit(1);
            }
        } else if(args.length == 2) {
            if(args[0].equals("--port")) {
                try {
                    PORT = Integer.parseInt(args[1]);
                } catch(Exception e) {
                    System.err.println("Failed to parse PORT `" + args[1] + "`");
                    System.exit(1);
                }
            }
            else if(args[0].equals("--help"))  System.out.println("Use --port <PORT> to specify port to run on. Runs on port 80 by default.");
            else {
                System.err.println("Unknown argument. Use --help for help.");
                System.exit(1);
            }
        } else if(args.length != 0) {
            System.err.println("Disallowed number of arguments (" + args.length + ")");
            System.exit(1);
        }

        new Server(PORT);
    }

}
