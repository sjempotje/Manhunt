package me.gaagjescraft.network.team.manhunt.events.bungee;

import me.gaagjescraft.network.team.manhunt.Manhunt;
import me.gaagjescraft.network.team.manhunt.events.custom.ManhuntBungeeMessageReceiveEvent;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BungeeSocketManager {

    public final List<Socket> socketsConnected = new ArrayList<>();
    private ServerSocket ss = null;
    public Socket s = null;
    private DataInputStream din = null;
    private DataOutputStream dout = null;
    private Thread thread = null;
    private int bukkitTaskId = -1;

    /**
     * This method sends a message to all the connected clients. (Only server side supported).
     */
    public boolean sendMessage(String... msg) {
        int count = 0;

        if (Manhunt.get().getCfg().debug) {
            Bukkit.getLogger().info("Sending a message: ");
            for (String s : msg) {
                Bukkit.getLogger().info("value = " + s);
            }
        }

        if (Manhunt.get().getCfg().isLobbyServer) {
            socketsConnected.removeIf((socket -> socket.isClosed() || !socket.isConnected() || socket.isOutputShutdown()));
            for (Socket socket : socketsConnected) {
                if (!socket.isClosed() && socket.isConnected() && !socket.isOutputShutdown()) {
                    try {
                        DataOutputStream socketDout = new DataOutputStream(socket.getOutputStream());
                        for (String s : msg) {
                            socketDout.writeUTF(s);
                        }
                        socketDout.flush();
                        if (Manhunt.get().getCfg().debug) Bukkit.getLogger().info("message sent.");
                        count++;
                    } catch (Exception ignored) {
                    }
                }
            }

            if (count == 0 && !socketsConnected.isEmpty()) {
                Bukkit.getLogger().severe("Failed to send a message to another server because the output stream returned an error:");
                return false;
            }

        } else {
            try {
                DataOutputStream socketDout = new DataOutputStream(s.getOutputStream());
                for (String s : msg) {
                    socketDout.writeUTF(s);
                }
                socketDout.flush();
                if (Manhunt.get().getCfg().debug) Bukkit.getLogger().info("message sent.");
            } catch (Exception e) {
                Bukkit.getLogger().severe("Failed to send a message to another server because the output stream returned an error:");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /*
        This method starts the socket server. (Server)
     */
    public void enableServer() {
        close();
        thread = new Thread(() -> {
            try {
                ss = new ServerSocket(Manhunt.get().getCfg().socketPort);

                while (true) {
                    Bukkit.getLogger().info("Attempting to accept a connection to the socket.");
                    s = ss.accept();
                    s.setKeepAlive(true);
                    Bukkit.getLogger().warning("Manhunt successfully connected to the socket on port " + Manhunt.get().getCfg().socketPort + " and is now ready to receive and send messages.");

                    socketsConnected.add(s);

                    if (bukkitTaskId != -1) Bukkit.getScheduler().cancelTask(bukkitTaskId);

                    bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Manhunt.get(), () -> {
                        try {
                            socketsConnected.removeIf((socket -> socket.isClosed() || !socket.isConnected() || socket.isOutputShutdown() || socket.isInputShutdown()));
                            for (Socket s : socketsConnected) {
                                DataInputStream din = new DataInputStream(s.getInputStream());
                                if (din.available() >= 2) {
                                    String subChannel = din.readUTF();
                                    String value = din.readUTF();
                                    if (Manhunt.get().getCfg().debug)
                                        Bukkit.getLogger().severe("Message from socket: " + subChannel + ", " + value);

                                    Bukkit.getPluginManager().callEvent(new ManhuntBungeeMessageReceiveEvent(subChannel, value));

                                    switch (subChannel) {
                                        case "createGameResponse" -> Manhunt.get().getBungeeMessenger().serverProcessCreateGameResponse(value);
                                        case "deleteGame" -> Manhunt.get().getBungeeMessenger().serverProcessDeleteGame(value);
                                        case "gameReady" -> Manhunt.get().getBungeeMessenger().serverProcessGameReady(value);
                                        case "gameEnded" -> Manhunt.get().getBungeeMessenger().serverProcessGameEnded(value);
                                        case "updateGame" -> Manhunt.get().getBungeeMessenger().serverProcessUpdateGame(value);
                                        case "endGame" -> Manhunt.get().getBungeeMessenger().serverProcessEndGame(value);
                                    }

                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, 0, 5);
                }


            } catch (Exception e) {
                Manhunt.get().getLogger().severe("Manhunt found an error while doing socket stuff:");
                e.printStackTrace();
                close();
            }
        });
        thread.start();
    }

    /*
        This method is being used to close the socket server and server?
     */

    public void close() {
        try {
            if (thread != null && thread.isAlive()) thread.interrupt();
            if (bukkitTaskId != -1) Bukkit.getScheduler().cancelTask(bukkitTaskId);
            if (din != null) din.close();
            if (dout != null) dout.close();
            if (s != null) s.close();
            if (ss != null) ss.close();
        } catch (Exception ignored) {
        }

        socketsConnected.clear();
    }

    /*
        This method is being used to connect from the client to the socketserver;
     */
    public void connectClientToServer() {
        close();
        thread = new Thread(() -> Bukkit.getScheduler().scheduleSyncRepeatingTask(Manhunt.get(), () -> {
            try {
                if (s == null || din == null || dout == null || s.isInputShutdown() || s.isOutputShutdown() || s.isClosed() || !s.isConnected()) {
                    close();
                    s = new Socket(Manhunt.get().getCfg().socketHostname, Manhunt.get().getCfg().socketPort);
                    din = new DataInputStream(s.getInputStream());
                    dout = new DataOutputStream(s.getOutputStream());
                    Bukkit.getLogger().warning("Manhunt successfully connected to the socket and is now ready to receive and send messages.");
                }

                if (din.available() >= 2) {
                    String subChannel = din.readUTF();
                    String value = din.readUTF();
                    if (Manhunt.get().getCfg().debug)
                        Bukkit.getLogger().severe("Message from socket: " + subChannel + ", " + value);

                    Bukkit.getPluginManager().callEvent(new ManhuntBungeeMessageReceiveEvent(subChannel, value));

                    switch (subChannel) {
                        case "createGame" -> Manhunt.get().getBungeeMessenger().clientProcessCreateGame(value);
                        case "endGame" -> Manhunt.get().getBungeeMessenger().clientProcessEndGame(value);
                        case "disconnect" -> clientProcessDisconnect();
                        case "addSpectator" -> Manhunt.get().getBungeeMessenger().clientProcessAddSpectator(value);
                    }
                }
            } catch (IOException ignored) {
                if (Manhunt.get().getCfg().debug)
                    Bukkit.getLogger().severe("Manhunt failed to connect to the socket. Trying again in 1/2 second.");
            }
        }, 0, 5));
        thread.start();
    }

    private void clientProcessDisconnect() {
        try {
            if (s != null && !s.isClosed()) s.close();
            if (din != null) din.close();
            if (dout != null) dout.close();
        } catch (Exception ignored) {
        }
    }


}