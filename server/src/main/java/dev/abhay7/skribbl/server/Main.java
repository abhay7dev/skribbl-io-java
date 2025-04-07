package dev.abhay7.skribbl.server;

public class Main {

    public static void main(String... args) {
        
        int PORT = 80;
        boolean HEADLESS = false;

        if(args.length == 1) {
            if(args[0].equals("--headless")) HEADLESS = true;
            else if(args[0].equals("--help")) System.out.println("Use --headless to run in headless mode. Runs on port 80 by default.\nUse --headless <PORT> to specify port to run on. Ex. `--headless 6060`.");
            else {
                System.err.println("Unknown argument: " + args[0] + ". Use --help for help.");
                System.exit(1);
            }
        } else if(args.length == 2) {
            if(args[0].equals("--headless")) {
                HEADLESS = true;
                try {
                    PORT = Integer.parseInt(args[1]);
                } catch(Exception e) {
                    System.err.println("Failed to parse PORT `" + args[1] + "`");
                    System.exit(1);
                }
            }
            else if(args[0].equals("--help")) System.out.println("Use --headless to run in headless mode. Runs on port 80 by default.\nUse --headless <PORT> to specify port to run on. Ex. `--headless 6060`.");
            else {
                System.err.println("Unknown argument: " + args[0] + ". Use --help for help.");
                System.exit(1);
            }

        } else if(args.length != 0) {
            System.err.println("Disallowed number of arguments (" + args.length + ")");
            System.exit(1);
        }

        new Server(PORT, HEADLESS);
    }

}
