package dev.tonysp.koth.commands;

import dev.tonysp.koth.KoTH;
import dev.tonysp.koth.game.Game;
import dev.tonysp.koth.game.GameParameter;
import dev.tonysp.koth.game.SimpleGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KoTHCommand implements CommandExecutor, Listener {

    private final KoTH plugin;

    private final String rewardInventoryName = "Insert reward";
    private final int rewardInventorySize = 9 * 3;

    public KoTHCommand (KoTH plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand (CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getGames().isSaving()) {
            sender.sendMessage(ChatColor.RED + "Saving in progress! Please try again.");
            return true;
        }

        plugin.getGames().startModifyingGames();
        boolean result = processCommand(sender, command, label, args);
        plugin.getGames().endModifyingGames();
        return result;
    }

    private boolean processCommand (CommandSender sender, Command command, String label, String[] args) {
        String usedCommand = label.toLowerCase();

        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        if (!sender.hasPermission("koth.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have a permission to do this!");
            return true;
        }

        if (args.length == 0) {
            printHelp(sender, usedCommand);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            listSubcommand(sender, usedCommand);
        } else if (args[0].equalsIgnoreCase("create")) {
            createSubcommand(sender, usedCommand, args);
        } else if (args[0].equalsIgnoreCase("info")) {
            infoSubcommand(sender, usedCommand, args);
        } else if (args[0].equalsIgnoreCase("start")) {
            startSubcommand(sender, usedCommand, args);
        } else {
            gameSubcommand(sender, usedCommand, args);
        }

        return true;
    }

    public void listSubcommand (CommandSender sender, String usedCommand) {
        if (plugin.getGames().getGameList().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "There are no games. Create one with /" + usedCommand + " create [name]");
            return;
        }
        sender.sendMessage(ChatColor.YELLOW + "Games:");
        plugin.getGames().getGameList().forEach(game -> {
            sender.sendMessage(ChatColor.YELLOW + "- " + game.getName());
        });
    }

    public void createSubcommand (CommandSender sender, String usedCommand, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + usedCommand + " create [name]");
            return;
        }

        Optional<Game> gameOptional = plugin.getGames().getGameByName(args[1]);
        if (gameOptional.isPresent()) {
            sender.sendMessage(ChatColor.RED + "This game already exists.");
            return;
        }

        Game game = plugin.getGames().create(args[1]);
        sender.sendMessage(ChatColor.GREEN + "Game created. Set up all parameters before starting it.");
        sender.sendMessage(game.getMissingParametersMessage());
    }

    public void infoSubcommand (CommandSender sender, String usedCommand, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + usedCommand + " info [name]");
            return;
        }

        Optional<Game> gameOptional = plugin.getGames().getGameByName(args[1]);
        if (!gameOptional.isPresent()) {
            sender.sendMessage(ChatColor.RED + "This game does not exists.");
            return;
        }

        SimpleGame simpleGame = (SimpleGame) gameOptional.get();
        sender.sendMessage(ChatColor.YELLOW + "Game name: " + simpleGame.getName());
        sender.sendMessage(ChatColor.YELLOW + "Game state: " + simpleGame.getGameState().toString());
        if (!simpleGame.isMissingParameter(GameParameter.REGION_CENTER)) {
            sender.sendMessage(ChatColor.YELLOW + "Region center: " + getLocationString(simpleGame.getRegionCenter()));
        }
        if (!simpleGame.isMissingParameter(GameParameter.REGION_RADIUS)) {
            sender.sendMessage(ChatColor.YELLOW + "Region radius: " + simpleGame.getRegionRadius());
        }
        if (!simpleGame.isMissingParameter(GameParameter.CAPTURE_TIME)) {
            sender.sendMessage(ChatColor.YELLOW + "Capture time: " + simpleGame.getCaptureTime());
        }
        if (!simpleGame.isMissingParameter(GameParameter.REWARD)) {
            sender.sendMessage(ChatColor.YELLOW + "Reward: *view with /" + usedCommand + " [name] setreward*");
        }
        if (gameOptional.get().isMissingParameters()) {
            sender.sendMessage(gameOptional.get().getMissingParametersMessage());
        }
    }

    public void startSubcommand (CommandSender sender, String usedCommand, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + usedCommand + " start [name]");
            return;
        }

        Optional<Game> gameOptional = plugin.getGames().getGameByName(args[1]);
        if (!gameOptional.isPresent()) {
            sender.sendMessage(ChatColor.RED + "This game does not exists.");
            return;
        }

        if (gameOptional.get().isMissingParameters()) {
            sender.sendMessage(ChatColor.RED + "Set up all parameters before starting the game.");
            sender.sendMessage(gameOptional.get().getMissingParametersMessage());
        } else {
            plugin.getGames().startGame(gameOptional.get());
            sender.sendMessage(ChatColor.GREEN + "Game started!");
        }
    }

    public void gameSubcommand (CommandSender sender, String usedCommand, String[] args) {
        Optional<Game> gameOptional = plugin.getGames().getGameByName(args[0]);
        if (!gameOptional.isPresent() || args.length <= 1) {
            sender.sendMessage(ChatColor.RED + "Invalid subcommand or specified game does not exists.");
            return;
        }

        Player player = (Player) sender;
        SimpleGame simpleGame = (SimpleGame) gameOptional.get();
        if (args[1].equalsIgnoreCase("setregion")) {
            simpleGame.setRegionCenter(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Capture region set!");
        } else if (args[1].equalsIgnoreCase("setradius")) {
            if (args.length <= 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + usedCommand + " [name] setradius [radius]");
                return;
            }

            int radius = parsePositiveNumber(args[2]);
            if (radius <= 0) {
                sender.sendMessage(ChatColor.RED + "Radius needs to be a positive number.");
                return;
            }

            simpleGame.setRegionRadius(radius);
            sender.sendMessage(ChatColor.GREEN + "Radius set!");
        } else if (args[1].equalsIgnoreCase("settime")) {
            if (args.length <= 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /" + usedCommand + " [name] settime [seconds]");
                return;
            }

            int time = parsePositiveNumber(args[2]);
            if (time <= 0) {
                sender.sendMessage(ChatColor.RED + "Time needs to be a positive number.");
                return;
            }

            simpleGame.setCaptureTime(time);
            sender.sendMessage(ChatColor.GREEN + "Capture time set!");
        } else if (args[1].equalsIgnoreCase("setreward")) {
            openSetRewardWindow(player, simpleGame);
        } else {
            printHelp(sender, usedCommand);
        }
    }


    private void printHelp (CommandSender sender, String usedCommand) {
        sender.sendMessage(ChatColor.GOLD + "-- King of The Hill --" );
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "list" + ChatColor.GRAY + " - Lists games");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "create [name]" + ChatColor.GRAY + " - Create a new game");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "info [name]" + ChatColor.GRAY + " - Prints info about game");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "start [name]" + ChatColor.GRAY + " - Starts the specified game");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "[name] setregion" + ChatColor.GRAY + " - Sets capture region");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "[name] setradius [radius]" + ChatColor.GRAY + " - Sets capture region size");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "[name] settime [seconds]" + ChatColor.GRAY + " - Sets capture time");
        sender.sendMessage(ChatColor.GOLD + "/" + usedCommand + " " + ChatColor.YELLOW + "[name] setreward" + ChatColor.GRAY + " - Sets capture reward");
    }

    private int parsePositiveNumber (String argument) {
        int number = -1;
        try {
            number = Integer.parseInt(argument);
        } catch (Exception ignored) { }
        return number;
    }

    private void openSetRewardWindow (Player player, SimpleGame simpleGame) {
        Inventory inventory = Bukkit.createInventory(player, rewardInventorySize, rewardInventoryName + " - " + simpleGame.getName());
        int i = 0;
        for (ItemStack item : simpleGame.getReward()) {
            inventory.setItem(i ++, item);
        }
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCloseEvent (InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (!inventory.getName().contains(rewardInventoryName)) {
            return;
        }
        Optional<Game> gameOptional = plugin.getGames().getGameByName(inventory.getName().split(" - ")[1]);
        if (!gameOptional.isPresent()) {
            return;
        }

        List<ItemStack> newReward = new ArrayList<>();
        for (int i = 0; i < rewardInventorySize; i++) {
            if (inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
                newReward.add(inventory.getItem(i));
            }
        }

        plugin.getGames().startModifyingGames();
        SimpleGame simpleGame = (SimpleGame) gameOptional.get();
        simpleGame.setReward(newReward);
        plugin.getGames().endModifyingGames();
        event.getPlayer().sendMessage(ChatColor.GREEN + "Reward set!");
    }

    private String getLocationString (Location location) {
        return location.getWorld().getName() + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
}
