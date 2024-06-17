import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Main extends JavaPlugin {
    String token;
    int delay = 2000;
    String[] userStrings;
    String message;
    String command = "ngrok tcp 25565";
    JDA jda;
    ArrayList<User> users = new ArrayList<>();

    public void onEnable() {
        try {
            this.run();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void onDisable() {
        this.stopNgrok();
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, @NotNull String[] args) {
        if (sender == null) {
            Main.$$$reportNull$$$0(0);
        }
        if (command == null) {
            Main.$$$reportNull$$$0(1);
        }
        if (args == null) {
            Main.$$$reportNull$$$0(2);
        }
        if (label.equalsIgnoreCase("sendip")) {
            if (!sender.isOp()) {
                return false;
            }
            try {
                this.sendMessages(this.getIp());
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Failed to get ip from ngrok");
            }
        } else if (label.equalsIgnoreCase("getip")) {
            try {
                String ip = this.getIp();
                sender.sendMessage(ChatColor.GREEN + "The server IP is: " + ip);
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Failed to get ip from ngrok");
            }
        }
        return false;
    }

    public void run() throws InterruptedException {
        File configFile = new File("config.cfg");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                this.getLogger().log(Level.SEVERE, "Failed to create config file");
            }
        }
        try {
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("#")) continue;
                try {
                    String prefix = line.split("=")[0];
                    String contents = line.split("=")[1];
                    switch (prefix) {
                        case "token": {
                            this.token = contents;
                            break;
                        }
                        case "delay": {
                            this.delay = Integer.parseInt(contents);
                            break;
                        }
                        case "users": {
                            this.userStrings = contents.split(",");
                            break;
                        }
                        case "message": {
                            this.message = contents;
                            break;
                        }
                        case "command": {
                            this.command = contents;
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException prefix) {}
            }
        } catch (FileNotFoundException e) {
            this.getLogger().log(Level.SEVERE, "Config file couldn't be found");
            return;
        }
        try {
            this.startNgrok();
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Ngrok failed to load. Check command in config");
            return;
        }
        this.getLogger().log(Level.INFO, this.token);
        try {
            this.initJDA();
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "JDA failed to load. Have you added your token in the config?");
            return;
        }
        this.addUsers();
        Thread.sleep(this.delay);
        boolean success = false;
        String ip = "";
        while (!success) {
            try {
                ip = this.getIp();
                success = true;
            } catch (IOException e) {
                this.getLogger().log(Level.INFO, "Failed to get ip from ngrok, trying again...");
                Thread.sleep(1000L);
            }
        }
        this.sendMessages(ip);
    }

    void initJDA() {
        this.jda = JDABuilder.createDefault(this.token).build();
    }

    void stopNgrok() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            this.getLogger().log(Level.INFO, "OS detected: " + osName);
            if (osName.contains("nix") || osName.contains("nux")) {
                Runtime.getRuntime().exec("pkill ngrok");
            } else if (osName.toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("taskkill /f /im ngrok.exe");
            }
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, e.toString());
            this.getLogger().log(Level.SEVERE, "FAILED TO KILL NGROK");
        }
    }

    void startNgrok() throws IOException {
        Runtime.getRuntime().exec(this.command);
    }

    void addUsers() {
        for (String userString : this.userStrings) {
            this.jda.retrieveUserById(userString).queue(user -> this.users.add((User)user));
        }
    }

    String getIp() throws IOException {
        String tunnelData = this.getTunnelData();
        return tunnelData.split("tcp://")[1].split("\"")[0];
    }

    String getTunnelData() throws IOException {
        String inputLine;
        URL uri = new URL("http://localhost:4040/api/tunnels");
        URLConnection ec = uri.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(ec.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder a = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            a.append(inputLine);
        }
        in.close();
        return a.toString();
    }

    void sendMessages(String ip) {
        for (User user : this.users) {
            if (user == null) {
                this.getLogger().log(Level.SEVERE, "User is null");
                return;
            }
            user.openPrivateChannel().queue(channel -> channel.sendMessage(this.message + " " + ip).queue());
        }
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2 = new Object[3];
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[0] = "sender";
                break;
            }
            case 1: {
                objectArray = objectArray2;
                objectArray2[0] = "command";
                break;
            }
            case 2: {
                objectArray = objectArray2;
                objectArray2[0] = "args";
                break;
            }
        }
        objectArray[1] = "Main";
        objectArray[2] = "onCommand";
        throw new IllegalArgumentException(String.format("Argument for @NotNull parameter '%s' of %s.%s must not be null", objectArray));
    }
}
