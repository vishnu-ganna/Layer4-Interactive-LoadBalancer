import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {
    private static final int[] ALL_BACKEND_PORTS = {8081, 8082, 8083};
    private static final CopyOnWriteArrayList<Integer> healthyPorts = new CopyOnWriteArrayList<>();
    private static final AtomicInteger nextServerIndex = new AtomicInteger(0);
    
    // Default load balancing policy
    private static String schedulingPolicy = "ROUND_ROBIN"; 

    public static void main(String[] args) {
        int listeningPort = 8080;

        for (int port : ALL_BACKEND_PORTS) {
            healthyPorts.add(port);
        }

        // --- 1. INTERACTIVE INPUT THREAD FOR INTERVIEW DEMOS ---
        Thread cliThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("\n⌨️ INTERACTIVE MANAGER CONSOLE ONLINE");
            System.out.println("Commands available: ");
            System.out.println("  'status'       - View live server pool state");
            System.out.println("  'kill <port>'  - Forcefully simulate a crashed server (e.g., kill 8082)");
            System.out.println("  'revive <port>'- Recover a crashed server (e.g., revive 8082)");
            System.out.println("  'policy <type>'- Change scheduling logic dynamically (e.g., policy RANDOM)\n");

            while (true) {
                try {
                    System.out.print("LB-Admin> ");
                    String input = reader.readLine();
                    if (input == null) continue;
                    input = input.trim();

                    if (input.equalsIgnoreCase("status")) {
                        System.out.println("📊 Current Active Backend Server Pool: " + healthyPorts);
                        System.out.println("⚙️ Active Scheduling Policy: " + schedulingPolicy);
                    } 
                    else if (input.toLowerCase().startsWith("kill ")) {
                        int port = Integer.parseInt(input.substring(5).trim());
                        if (healthyPorts.contains(port)) {
                            healthyPorts.remove(Integer.valueOf(port));
                            System.out.println("💥 Forcefully crashed server on port " + port + ". It has been isolated.");
                        } else {
                            System.out.println("❌ Port " + port + " is not in the active pool.");
                        }
                    } 
                    else if (input.toLowerCase().startsWith("revive ")) {
                        int port = Integer.parseInt(input.substring(7).trim());
                        if (!healthyPorts.contains(port)) {
                            healthyPorts.add(port);
                            System.out.println("💖 Server on port " + port + " revived and re-added to the routing pool.");
                        } else {
                            System.out.println("❌ Port " + port + " is already healthy.");
                        }
                    }
                    else if (input.toLowerCase().startsWith("policy ")) {
                        String policy = input.substring(7).trim().toUpperCase();
                        if (policy.equals("ROUND_ROBIN") || policy.equals("RANDOM")) {
                            schedulingPolicy = policy;
                            System.out.println("🎛️ Scheduling policy dynamically switched to: " + schedulingPolicy);
                        } else {
                            System.out.println("❌ Unknown policy. Use 'ROUND_ROBIN' or 'RANDOM'.");
                        }
                    }
                    else {
                        System.out.println("❓ Unknown command. Try 'status', 'kill <port>', 'revive <port>', or 'policy <type>'.");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Invalid command format.");
                }
            }
        });
        cliThread.setDaemon(true);
        cliThread.start();


        // --- 2. MAIN TRAFFIC INTERCEPTION LOOP ---
        try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {
            System.out.println("🚀 L4 API Gateway Booted on port " + listeningPort);

            while (true) {
                Socket clientSocket = serverSocket.accept(); 
                
                if (healthyPorts.isEmpty()) {
                    sendErrorResponse(clientSocket, "503 Service Unavailable", "503 System Error: Zero upstream backends available.");
                    continue;
                }

                // Choose target server based on dynamically selected scheduling policy
                int targetBackendPort;
                if (schedulingPolicy.equals("RANDOM")) {
                    int randomIndex = (int) (Math.random() * healthyPorts.size());
                    targetBackendPort = healthyPorts.get(randomIndex);
                } else {
                    // Default: Round Robin
                    int index = nextServerIndex.getAndIncrement() % healthyPorts.size();
                    targetBackendPort = healthyPorts.get(Math.abs(index));
                }

                Thread workerThread = new Thread(new RoutingTask(clientSocket, targetBackendPort));
                workerThread.start(); 
            }
        } catch (IOException e) {
            System.err.println("❌ Main server error loop failure: " + e.getMessage());
        }
    }

    private static void sendErrorResponse(Socket socket, String status, String message) {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("HTTP/1.1 " + status);
            out.println("Content-Type: text/plain");
            out.println();
            out.println(message);
            socket.close();
        } catch (IOException ignored) {}
    }
}

class RoutingTask implements Runnable {
    private Socket clientSocket;
    private int backendPort;

    public RoutingTask(Socket socket, int port) {
        this.clientSocket = socket;
        this.backendPort = port;
    }

    @Override
    public void run() {
        long threadId = Thread.currentThread().getId();
        try {
            // Forward connection dummy simulation response
            PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            clientWriter.println("HTTP/1.1 200 OK");
            clientWriter.println("Content-Type: text/plain");
            clientWriter.println();
            clientWriter.println("Success! Your network connection handled by server port: " + backendPort);
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("❌ Worker thread exception on port " + backendPort);
        }
    }
}
