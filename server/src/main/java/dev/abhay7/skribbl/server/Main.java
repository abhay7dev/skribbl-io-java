package dev.abhay7.skribbl.server;

public class Main {

    public static void main(String... args) {
        
        int PORT = 8080;
        String wordListSource = "https://gist.githubusercontent.com/paraswtf/3f3351cf6d64f8a6ad1c2accf1604b44/raw/d31c918e6ab75ad15a590b4c579094e72f4bb966/Skribbl.io%2520words%2520list.csv";

        if(args.length == 1) {
            if(args[0].equals("--help")) System.out.println("Use --port <PORT> to specify port to run on. Runs on port 80 by default.\nUse --wordlist <file or url> to specify a resource to read a wordlist from.\n\tWord list uses https://gist.github.com/paraswtf/3f3351cf6d64f8a6ad1c2accf1604b44 by default\n\tYou can specify a file path (as outlined by java File() class) or a URI (Beginning with https://)");
            else if(args[0].equals("--port")) System.out.println("Use --port <PORT> to specify port to run on.");
            else if(args[0].equals("--wordlist")) System.out.println("Use --wordlist <file or url> to specify a resource to read a wordlist from.");
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
            } else if(args[0].equals("--wordlist")) {
                wordListSource = args[1];
            } else if(args[0].equals("--help"))  System.out.println("Use --port <PORT> to specify port to run on. Runs on port 80 by default.");
            else {
                System.err.println("Unknown argument. Use --help for help.");
                System.exit(1);
            }
        } else if(args.length == 4) {
            if(args[0].equals("--port") || args[2].equals("--port")) {
                try {
                    PORT = Integer.parseInt(args[0].equals("--port") ? args[1] : args[3]);
                } catch(Exception e) {
                    System.err.println("Failed to parse PORT `" + args[1] + "`");
                    System.exit(1);
                }
            }
            if(args[0].equals("--wordlist") || args[2].equals("--wordlist")) {
                wordListSource = (args[0].equals("--wordlist") ? args[1] : args[3]);
            }

            if((!args[0].equals("--port") && !args[2].equals("--port")) || 
               (!args[0].equals("--wordlist") && !args[2].equals("--wordlist"))) {
                System.err.println("Unknown argument. Use --help for help.");
                System.exit(1);
            }

        } else if(args.length == 3 || args.length != 0) {
            System.err.println("Disallowed number of arguments (" + args.length + ")");
            System.exit(1);
        }

        new Server(PORT, wordListSource);
    }

}
