package minecraftnftterra.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class Plugin extends JavaPlugin {
    public LinkedBlockingQueue<Msg> MintQueue;
    public Queue<Msg> AddressQueue;
    Thread thread;

    @Override
    public void onEnable() {
        // Plugin startup login
        this.MintQueue = new LinkedBlockingQueue<>();
        this.AddressQueue = new LinkedList<>();

        this.thread = new mintThread(this, this.MintQueue, this.AddressQueue);
        this.thread.start();
        getServer().getScheduler().runTaskTimer(this, new checkTask(this.AddressQueue), 20, 20);
        EquipCheck e = new EquipCheck(this);
        Objects.requireNonNull(getCommand("check")).setExecutor(e);
        Objects.requireNonNull(getCommand("mint")).setExecutor(e);
    }

    @Override
    public void onDisable() {
        try {
            this.MintQueue.put(new Msg(true));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Plugin shutdown logic
    }
}

class Msg {
    boolean close = false;
    public ItemStack item;
    public Player player;
    public String S;

    public Msg(boolean close) {
        this.close = close;
    }

    public Msg(ItemStack item, Player player) {
        this.item = item;
        this.player = player;
    }

    public boolean isClose() {
        return close;
    }

}

class mintThread extends Thread {
    JavaPlugin plugin;
    LinkedBlockingQueue<Msg> queue;
    Queue<Msg> AddressQueue;

    public mintThread(JavaPlugin plugin, LinkedBlockingQueue<Msg> queue, Queue<Msg> AddressQueue) {
        this.plugin = plugin;
        this.queue = queue;
        this.AddressQueue = AddressQueue;
    }

    public void run() {
        while (true) {
            try {
                Msg m = this.queue.take();
                if (m.isClose()) {
                    break;
                }
                ItemStack i = m.item;
                String itemString = i.serialize().toString();
                itemString = Base64.getEncoder().encodeToString(itemString.getBytes());
                ProcessBuilder p = new ProcessBuilder().command("/bin/sh", "-i", "-c", "\"$(terrad tx wasm instantiate 1 '{\"equip\":\"" + itemString + "\"}' --from test1 --chain-id=localterra --fees=10000uluna --gas=auto --broadcast-mode=block -y)\"");
                Process proc = p.start();
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(proc.getErrorStream()));
                proc.waitFor();
                String s;
                while ((s = stdInput.readLine()) != null) {
                    if (s.contains("contract_address")) {
                        break;
                    }
                }
                s = stdInput.readLine().split(": ")[1];
                m.S = s;
                this.AddressQueue.add(m);

            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class checkTask implements Runnable {
    Queue<Msg> addressQueue;

    public checkTask(Queue<Msg> q) {
        this.addressQueue = q;
    }

    @Override
    public void run() {
        if (!this.addressQueue.isEmpty()) {
            Msg m = this.addressQueue.poll();
            PlayerInventory inventory = m.player.getInventory();
            m.player.sendMessage(Component.text("Click this to copy contract address").color(TextColor.color(240,128,128)).clickEvent(ClickEvent.copyToClipboard(m.S)));
            m.player.sendMessage("And this item is unbreakable now!");
            inventory.addItem(m.item);
        }
    }
}

class EquipCheck implements CommandExecutor {
    Plugin plugin;

    public EquipCheck(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean is_player = sender instanceof Player;
        if (is_player) {
            if (command.getName().equals("check")) {
                return this.check(sender);
            } else if (command.getName().equals("mint")) {
                return this.mint(sender);
            }
        }

        return false;
    }

    boolean mint(@NotNull CommandSender sender) {
        Player player = (Player) sender;
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItemInMainHand();
        player.sendMessage(Component.text("You are holding: ").append(item.displayName()));
        if (this.canBeMint(item)) {
            player.sendMessage("This will be mint");
        } else {
            player.sendMessage("This cannot be minted");
            return false;
        }

        inventory.remove(item);

        // mint it will make this unbreakable
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        // Put Item to mint queue
        try {
            this.plugin.MintQueue.put(new Msg(item, player));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    boolean check(@NotNull CommandSender sender) {
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        player.sendMessage(Component.text("You are holding: ").append(item.displayName()));
        if (this.canBeMint(item)) {
            player.sendMessage("This can be mint");
        } else {
            player.sendMessage("This cannot be minted");
        }
        return true;
    }

    boolean canBeMint(ItemStack item) {
        return (EnchantmentTarget.WEAPON.includes(item) ||
                EnchantmentTarget.WEARABLE.includes(item) ||
                EnchantmentTarget.TOOL.includes(item) ||
                EnchantmentTarget.TRIDENT.includes(item));
    }
}
