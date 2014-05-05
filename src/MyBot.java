/**
 * Created by ubx64-laptop on 21/04/14.
 */

import org.jibble.pircbot.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyBot extends ConfigurablePircBot {


    private ArrayList<String> mastersList;

    private final String[] restrictedCommands = {"part", "join", "quit", "nick",
            "msg", "op", "voice", "kick", "ban",
            "verboseOn", "verboseOff"
    };

    private String commandPrefix;

    private boolean hasAskedNickServ = false;

    private Map<String, String> receivedMessageDetails;

    private boolean ctcpVerboseOnChan = false;

    private boolean hasRequestedCtcpInfo = false;

    private String onJoinedChannel = "";

    public MyBot(String[] masters, String commandPrefix){
        this.setAutoNickChange(true);
        if (masters != null)
            this.mastersList = new ArrayList<String>(Arrays.asList(masters));

        if (commandPrefix != null)
            this.commandPrefix = commandPrefix;
        else
            this.commandPrefix = "!";

        receivedMessageDetails = new HashMap<String, String>();
    }


    public synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {
        processMessage(channel, sender, login, hostname, message);
    }

    public synchronized void onPrivateMessage(String sender, String login, String hostname, String message) {
        processMessage(sender, sender, login, hostname, message);

    }

    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
        processMessage(sourceNick, sourceNick, sourceLogin, sourceHostname, notice);

        if (hasRequestedCtcpInfo && (notice.contains("TIME") || notice.contains("VERSION")) ){
            this.sendMessage(onJoinedChannel, notice.substring(1, notice.length() -1));

            if (notice.contains("VERSION")){
                 hasRequestedCtcpInfo = false;
            }
        }
    }


    private void processMessage(String channel, String sender, String login, String hostname, String message) {

        if (message.startsWith(commandPrefix)){

           for (String command : restrictedCommands){
               if (message.contains(command)){
                   for (String master : mastersList){
                       if (sender.toLowerCase().equals(master.toLowerCase())){
                           hasAskedNickServ = true;
                           receivedMessageDetails.put("channel", channel);
                           receivedMessageDetails.put("sender", sender);
                           receivedMessageDetails.put("login", login);
                           receivedMessageDetails.put("hostname", hostname);
                           receivedMessageDetails.put("message", message);
                           this.sendMessage("nickserv", "info " + sender);
                       }
                   }
               }
           }
        }

        if (hasAskedNickServ && sender.toLowerCase().equals("nickserv")){
            if (message.contains("Last seen  : now")){ //our master is identified with the nickserv
                processCommand();
                hasAskedNickServ = false;
            }
        }

    }

    public synchronized void onDisconnect() {
        int reconnectDelay = 30; // seconds
        while (!isConnected()) {
            try {
                this.log("*** Attempting to reconnect to server.");
                reconnect();
            }
            catch (Exception e) {
                this.log("*** Failed to reconnect to server. Sleeping " + reconnectDelay + " seconds.");
                try {
                    Thread.sleep(reconnectDelay * 1000);
                } catch (InterruptedException ie) {
                    // ignored
                }
            }
        }
        // Now that we're connected, rejoin channels, if specified
        if (this.getConfiguration().containsKey("Channels")) {
            joinChannel(this.getConfiguration().getString("Channels"));
        }
    }


    public synchronized void onKick(String channel, String kickerNick, String kickerLogin,
                                    String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) {
            int kickDelay = 5; // seconds
            this.log("*** Kicked from channel: " + channel);

            try {
                Thread.sleep(kickDelay * 1000);
            } catch (InterruptedException ie) {
                // ignored
            }

            joinChannel(channel);

        }
    }

    public synchronized void onJoin(String channel, String sender, String login, String hostname){
        if (ctcpVerboseOnChan){
            this.sendMessage(channel, hostname);
            this.sendCTCPCommand(sender, "time");
            this.sendCTCPCommand(sender, "version");
            this.hasRequestedCtcpInfo = true;
            onJoinedChannel = channel;
        }
    }

    private void processCommand(){

        String message = receivedMessageDetails.get("message");

        String firstArgument, secondArgument;
        firstArgument = secondArgument = null;


        //Split string on spaces, except if between quotes
        List<String> list = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(message);
        while (m.find()){
            String mTemp = m.group(1).replace("\"", "");
            list.add(mTemp);
        }
        String commandArguments[] = list.toArray(new String[list.size()]);


        if (commandArguments.length > 0){
            if (commandArguments.length > 1){
                firstArgument = commandArguments[1];
                if (commandArguments.length > 2){
                    secondArgument = commandArguments[2];
                }
            }
        }

        if (!receivedMessageDetails.isEmpty()){
            if (message.contains("part")){
                if (firstArgument != null)
                    this.partChannel(firstArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: part #channel");
            } else if (message.contains("join")){
                if (firstArgument != null)
                    this.joinChannel(firstArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: part #channel");
            } else if (message.contains("quit")){
                if (firstArgument != null)
                    this.quitServer(firstArgument);
                else
                    this.quitServer();
            } else if (message.contains("nick")){
                if (firstArgument != null)
                    this.changeNick(firstArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: nick newNick");
            } else if (message.contains("msg")){
                if (firstArgument != null && secondArgument != null)
                    this.sendMessage(firstArgument, secondArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: msg target message");
            } else if (message.contains("op")){
                if (firstArgument != null && secondArgument != null)
                    this.op(firstArgument, secondArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: op channel nick");
            } else if (message.contains("voice")){
                if (firstArgument != null && secondArgument != null)
                    this.voice(firstArgument, secondArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: op channel nick");
            }else if (message.contains("kick")){
                if (firstArgument != null && secondArgument != null)
                    this.kick(firstArgument, secondArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: kick channel nick");
            } else if (message.contains("ban")){
                if (firstArgument != null && secondArgument != null)
                    this.ban(firstArgument, secondArgument);
                else
                    this.sendMessage(receivedMessageDetails.get("sender"), "Syntax is: ban channel nick");
            } else if (message.contains("verboseOn")){
                    this.ctcpVerboseOnChan = true;
            } else if (message.contains("verboseOff")){
                    this.ctcpVerboseOnChan = false;
            }
        }
    }




}
