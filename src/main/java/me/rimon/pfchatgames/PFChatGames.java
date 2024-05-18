package me.rimon.pfchatgames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PFChatGames extends JavaPlugin implements Listener {

    private Map<Player, Long> reactionTimes;
    private List<String> scrambleWords;
    private Random random;

    private int interval; // in minutes
    private int timeoutSeconds; // in seconds
    private BukkitRunnable timerTask;

    @Override
    public void onEnable() {
        reactionTimes = new HashMap<>();
        scrambleWords = new ArrayList<>();
        random = new Random();

        saveDefaultConfig();
        loadScrambleWords();
        loadInterval();
        loadTimeout();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("scramble").setExecutor(this);

        // Schedule the automatic game every interval
        int intervalTicks = interval * 60 * 20; // Convert minutes to ticks
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                startScrambleGame();
            }
        };
        timerTask.runTaskTimer(this, intervalTicks, intervalTicks);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("scramble")) {
            startScrambleGame();
            resetTimer();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final String guessedWord = event.getMessage().trim().toUpperCase();

        // Schedule the removal of the reaction time after the message has been sent
        Bukkit.getScheduler().runTask(this, () -> {
            if (reactionTimes.containsKey(player)) {
                long startTime = reactionTimes.get(player);
                reactionTimes.remove(player);
                long reactionTime = System.currentTimeMillis() - startTime;

                // Check if the guessed word is correct
                if (scrambleWords.contains(guessedWord)) {
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + ChatColor.WHITE + " unscrambled the word " + ChatColor.YELLOW + guessedWord + ChatColor.WHITE + " in " + ChatColor.GREEN +  (reactionTime / 1000.0) + ChatColor.WHITE + " seconds!");
                    Bukkit.broadcastMessage("");
                    // Cancel the timeout task if the correct word is guessed
                    cancelTimeoutTask();
                } else {
                }
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        reactionTimes.remove(player); // Remove player's old reaction time if exists
    }

    private void loadScrambleWords() {
        scrambleWords.clear();
        List<String> words = getConfig().getStringList("scrambleWords");
        for (String word : words) {
            scrambleWords.add(word.toUpperCase());
        }
    }

    private void loadInterval() {
        interval = getConfig().getInt("gameInterval");
    }

    private void loadTimeout() {
        timeoutSeconds = getConfig().getInt("timeoutSeconds");
    }

    private String currentWord; // Variable to store the current word being scrambled

    private void startScrambleGame() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Choose a random word to scramble
            currentWord = scrambleWords.get(random.nextInt(scrambleWords.size()));
            // Scramble the word
            char[] chars = currentWord.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                int randomIndex = random.nextInt(chars.length);
                char temp = chars[i];
                chars[i] = chars[randomIndex];
                chars[randomIndex] = temp;
            }
            String scrambledWord = new String(chars);
            player.sendMessage("Rimon's Chat games!!!");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Unscramble this word: " + ChatColor.WHITE + scrambledWord);
            player.sendMessage(ChatColor.YELLOW + "to win a prize!");
            player.sendMessage("");
            player.sendMessage("");
            reactionTimes.put(player, System.currentTimeMillis());
        }

        // Schedule the timeout task
        new BukkitRunnable() {
            @Override
            public void run() {
                endGameDueToTimeout();
            }
        }.runTaskLater(this, timeoutSeconds * 20); // Convert seconds to ticks
    }

    private void endGameDueToTimeout() {
        if (currentWord != null) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "No one guessed the correct word in time. The word was: " + currentWord);
            currentWord = null; // Reset the current word
        }
    }

    private void resetTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            int intervalTicks = interval * 60 * 20; // Convert minutes to ticks
            timerTask = new BukkitRunnable() {
                @Override
                public void run() {
                    startScrambleGame();
                }
            };
            timerTask.runTaskTimer(this, intervalTicks, intervalTicks);
        }
    }

    private void cancelTimeoutTask() {
        // Cancel the timeout task if it's still scheduled
        Bukkit.getScheduler().cancelTasks(this);
    }
}