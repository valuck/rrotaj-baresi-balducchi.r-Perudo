package WebAdapter;

import Messaging.Message;
import Storage.ClientStorage;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.perudo.ClientInterface;
import org.perudo.Main;

import java.net.SocketException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class SessionAdapter extends ClientInterface {
    private static final Logger logger = LogManager.getLogger(SessionAdapter.class);
    public SessionAdapter(String address, Integer port) {
        super(address, port);
    }

    @Override
    public void run() {
        try {
            // Listen for server messages until closure
            while (this.running) {
                try {
                    String serverResponse = in.readLine();
                    if (serverResponse != null) { // On message received
                        System.out.println(STR."Server response: \{serverResponse}");

                        // Generate a new message from the received data
                        Message response = new Message(this.server, serverResponse);
                        String encoder = response.getEncodingKey();
                        String scope = response.getScope();
                        Object data = response.getData();

                        if (encoder != null) // Updates server's RSA key
                            this.server.setEncodingKey(encoder);

                        if (scope != null) {
                            // Do requested action or response
                            LinkedTreeMap<String, Object> castedData = null;
                            String err = "Unknown";

                            try {
                                castedData = (LinkedTreeMap) data;
                                if (!isSuccess(castedData) && castedData.containsKey("Error"))
                                    err = (String) castedData.get("Error");

                            } catch (Exception e) {
                                // Ignore
                            }

                            switch (scope) { // If needed to handle any action response
                                case "Connection": { // Exchange encryption keys
                                    if (isSuccess(castedData))
                                        Main.printLogin();
                                    else
                                        Main.printRestart(STR."Error while pairing with the server: \{err}");

                                    break;
                                }

                                case "Login": { // Send login data to the server
                                    if (isSuccess(castedData)) {
                                        Main.printSoloMessage("Logged in, loading..");
                                        this.sendMessage("Lobbies", null, true);
                                    } else
                                        Main.printRestart(STR."Failed to log in: \{err}");

                                    break;
                                }

                                case "Ping": { // Check for the server presence
                                    if (isSuccess(castedData))
                                        ping_end = new Timestamp(System.currentTimeMillis());

                                    break;
                                }

                                case "Lobbies": { // Request a list of the lobbies
                                    if (isSuccess(castedData) && castedData.containsKey("Public") && castedData.containsKey("Private"))
                                        Main.printLobbies((LinkedTreeMap) castedData.get("Public"), (LinkedTreeMap) castedData.get("Private"));
                                    else
                                        Main.printRestart(STR."Unable to load lobbies: \{err}");

                                    break;
                                }

                                case "Create": { // Request a lobby creation
                                    if (!isSuccess(castedData)) {
                                        Main.printSoloMessage("Failed to create the lobby, loading lobbies list..");
                                        Thread.sleep(2000);

                                        this.sendMessage("Lobbies", null, true);
                                    }

                                    break;
                                }

                                case "Members": { // Handles the members event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("Name") && castedData.containsKey("Players") && castedData.containsKey("Host") && castedData.containsKey("Size") && castedData.containsKey("Started") && castedData.containsKey("Pause")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("Pause"));
                                        list.add(castedData.get("Started"));

                                        Main.printLobbyRoom((String) castedData.get("Name"), (ArrayList) castedData.get("Players"), (String) castedData.get("Host"), (Number) castedData.get("Size"), list);
                                    } else {
                                        Main.printSoloMessage(STR."Failed to load the lobby: \{err}, loading lobbies list..");
                                        Thread.sleep(2000);

                                        this.sendMessage("Lobbies", null, true);
                                    }

                                    break;
                                }

                                case "Join": { // Handles the response or event form the server
                                    if (isSuccess(castedData) && castedData.containsKey("Token")) // Save the new token to access the new lobby
                                        ClientStorage.updateSetting(STR."\{Main.getUsername().toLowerCase()}-token", castedData.get("Token"), true);
                                    else {
                                        Main.printSoloMessage(STR."Unable to join the lobby: \{err}, loading lobbies list..");
                                        Thread.sleep(2000);

                                        this.sendMessage("Lobbies", null, true);
                                    }

                                    break;
                                }

                                case "Turn": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("User")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("User"));

                                        Main.printGame(scope, list);
                                    }

                                    break;
                                }

                                case "Dice": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("Dice")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("Dice"));

                                        Main.printGame(scope, list);
                                    }

                                    break;
                                }

                                case "Pick": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("Dudo") && castedData.containsKey("Amount") && castedData.containsKey("Value")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("Dudo"));
                                        list.add(castedData.get("Amount"));
                                        list.add(castedData.get("Value"));

                                        Main.printGame(scope, list);
                                    }

                                    break;
                                }

                                case "Picked": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("Picks")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("Picks"));

                                        Main.printGame(scope, list);
                                    }

                                    break;
                                }

                                case "Sock": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("Sock")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("Sock"));

                                        Main.printGame(scope, list);
                                    }

                                    break;
                                }

                                case "Dudo": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("Results") && castedData.containsKey("Victim") && castedData.containsKey("Verdict")) {
                                        ArrayList<Object> list = new ArrayList<>(); // Used to store multiple values for a shared function
                                        list.add(castedData.get("Results"));
                                        list.add(castedData.get("Victim"));
                                        list.add(castedData.get("Verdict"));

                                        Main.printGame(scope, list);
                                    }

                                    break;
                                }

                                case "Winner": { // Handles the event from the server
                                    if (isSuccess(castedData) && castedData.containsKey("User"))
                                        Main.printWinner((String) castedData.get("User"));

                                    break;
                                }

                                case "Shutdown": { // Handles the event from the server
                                    if (data != null) {
                                        String msg = STR."Disconnecting: \{(String) data}";
                                        Main.printRestart(msg);
                                        logger.warn(msg);
                                        close();
                                    }

                                    break;
                                }
                            }
                        }
                    }
                } catch (SocketException e) {
                    // Socket is closing, ignore.

                } catch (Exception e) {
                    logger.error("Error on processing server message", e);
                }
            }

            logger.info("Client closed");
        } catch (Exception e) {
            throw new RuntimeException(STR."Error while listening for the server\{e}");
        }
    }
}
