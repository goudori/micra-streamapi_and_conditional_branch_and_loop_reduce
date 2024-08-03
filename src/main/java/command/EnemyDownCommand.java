package command;

import data.PlayerScore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.micra_thirty.Main;

/**
 * 制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するゲームです。 スコアは敵によって変わり、倒せた敵の合計によってスコアが変動します。
 * スコアの結果は、プレイヤー名、点数、日時が保存されます。
 */
public class EnemyDownCommand extends BaseCommand implements Listener {

  private final Main main;

  private final List<PlayerScore> playerScoreList = new ArrayList<>();

  public EnemyDownCommand(Main main) {
    this.main = main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player) {
    PlayerScore nowPlayerScore = getPlayerScore(player);

    initPlayerStatus(player);

    gamePlay(player, nowPlayerScore);
    return true;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender commandSender) {
    return false;
  }


  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();
    // イベント実行前と実行後で、プレイヤーがNullであれば、スキップする(Null対策)
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    /**
     * point　→倒した敵モンスターの点数
     */
    for (PlayerScore playerScore : playerScoreList) {
      if (playerScore.getPlayerName().equals(player.getName())) {
//        デフォルト点数
        int point = switch (enemy.getType()) {
          case ZOMBIE, ZOMBIFIED_PIGLIN, ZOMBIE_VILLAGER -> 10;
          case SPIDER -> 15;
          case SKELETON -> 30;
          default -> 0;
        };
        playerScore.setScore(playerScore.getScore() + point);
        player.sendMessage(
            ChatColor.YELLOW + "敵を倒した！ 現在のスコアは、" + playerScore.getScore() + "点です。");
      }
    }
  }

  /**
   * 現在実行しているプレイヤースコア情報を取得する
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　現在実行しているプレイヤーのスコア情報
   */
  private PlayerScore getPlayerScore(Player player) {
    PlayerScore playerScore = new PlayerScore(player.getName());
    if (playerScoreList.isEmpty()) {
      playerScore = addNewPlayer(player);
    } else {
      playerScore = playerScoreList.stream()
          .filter(ps -> ps.getPlayerName().equals(player.getName())).findFirst()
          .orElse(addNewPlayer(player));
    }
    playerScore.setGameTime(playerScore.getGameTime());
    return playerScore;
  }


  /**
   * 新規プレイヤー追加
   *
   * @param player 　コマンド実行プレイヤー
   * @return　新規プレイヤー
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore(player.getName());
    newPlayer.setGameTime(60);
    playerScoreList.add(newPlayer);
    return newPlayer;
  }

  private void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);
    player.setAllowFlight(true);
    player.setFlying(true);
    player.setFlySpeed(1f);

    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
    inventory.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
  }

  /**
   * ゲームを実行します。規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します
   *
   * @param player    コマンドを実行したプレイヤー
   * @param nowPlayer 　プレイヤーのスコア情報「
   */
  private void gamePlay(Player player, PlayerScore nowPlayer) {
    // 時間制限
    Bukkit.getScheduler().runTaskTimer(main, task -> {
      // 時間制限キャンセル
      if (nowPlayer.getGameTime() <= 0) {
        task.cancel();
        player.sendTitle("ゲーム終了",
            nowPlayer.getPlayerName() + "合計" + nowPlayer.getScore() + "点",
            0, 30, 0);
//          タイマー再設定
        nowPlayer.setScore(0);
//          敵の位置を取得
        List<Entity> nearbyEnemies = player.getNearbyEntities(50, 0, 50);
        for (Entity enemy : nearbyEnemies) {
//            敵を削除する
          switch (enemy.getType()) {
//              mergeをするとまとめられる
            case ZOMBIE, SKELETON, SPIDER, ZOMBIFIED_PIGLIN, ZOMBIE_VILLAGER -> enemy.remove();
          }

        }
        return;
      }
      // 時間制限開始
      player.getWorld().spawnEntity(getEnemySpawnLocation(player), getEnemy());
      nowPlayer.setGameTime(nowPlayer.getGameTime() - 5);
    }, 0, 5 * 20);
  }

  /**
   * 敵の出現場所を取得する
   *
   * @param player 　コマンドを実行し他プレイヤー
   * @return　敵の出現場所
   */

  private Location getEnemySpawnLocation(Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(20) - 10;
    int randomZ = new SplittableRandom().nextInt(20) - 10;

    double x = playerLocation.getX() + randomX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ() + randomZ;

    return new Location(player.getWorld(), x, y, z);
  }

  private EntityType getEnemy() {
    List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.SPIDER,
        EntityType.SKELETON, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIE_VILLAGER);
    int random = new SplittableRandom().nextInt(enemyList.size());
    return enemyList.get(random);
  }
}