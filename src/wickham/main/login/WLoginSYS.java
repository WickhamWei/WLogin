package wickham.main.login;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import wickham.main.WLogin;
import wickham.main.mysql.tables.PlayerPassword;
import wickham.main.mysql.tables.PlayerPlayingTime;

public abstract class WLoginSYS {

	private static HashMap<String, Timestamp> loginListHashMap = new HashMap<String, Timestamp>(); // 存放登录的玩家名字和登录的时间戳

	public static boolean isLogin(String playerNameString) { // 判断玩家是否登录
		return loginListHashMap.containsKey(playerNameString);
	}
	
	public static boolean isLogin(Player player) {
		return isLogin(player.getName());
	}

	public static boolean register(Player player, String passwordString) {// 玩家注册
		String playerNameString = player.getName();
		Statement statement;
		try {
			statement = WLogin.mySQL.getConnection().createStatement();
			statement.executeUpdate("INSERT INTO playerpassword(playername,password,ip) VALUES ('" + playerNameString
					+ "','" + passwordString + "',inet_aton('" + getPlayerIPAddress(player) + "'))");
			statement.close();
		} catch (SQLException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean isRegister(String playerNameString) {
		Statement statement;
		String playerNameFromDBString = null;
		try {
			statement = WLogin.mySQL.getConnection().createStatement();
			ResultSet result = statement
					.executeQuery("SELECT * FROM playerpassword WHERE playername = '" + playerNameString + "';");
			while (result.next()) {
				playerNameFromDBString = result.getString(1);
			}
			statement.close();
			if (playerNameFromDBString == null || playerNameFromDBString.length() == 0) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return false;
		}
	}

	public static boolean chackPassword(Player player, String passwordString) {// 检查密码是否正确
		String playerNameString = player.getName();
		Statement statement;
		String passwordFromDBString = null;
		try {
			statement = WLogin.mySQL.getConnection().createStatement();
			ResultSet result = statement
					.executeQuery("SELECT password FROM playerpassword WHERE playername = '" + playerNameString + "';");
			while (result.next()) {
				passwordFromDBString = result.getString(1);
			}
			if (passwordFromDBString == null || passwordFromDBString.length() == 0) {
				return false;
			} else {
				if (passwordFromDBString.equals(passwordString)) {
					return true;
				}
			}
		} catch (SQLException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public static void login(Player player) {
		String playerNameString=player.getName();
		// TODO Auto-generated method stub
		try {
			Statement statement = WLogin.mySQL.getConnection().createStatement();
			String sql = "INSERT INTO playerlogindata(playername,logintime,loginable,ip) VALUES ('" + playerNameString
					+ "', now() ," + true + ",inet_aton('" + getPlayerIPAddress(player) + "'))";
			statement.executeUpdate(sql);
			statement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			WLogin.main.getLogger().warning("玩家 " + playerNameString + " 的登陆数据记录失败");
		}
		loginListHashMap.put(playerNameString, getNowTimestamp());
		return;
	}

	public static boolean unlogin(Player player) {// 使玩家退出登录
		// TODO Auto-generated method stub
		if(player==null) {
			return false;
		}
		String playerNameString=player.getName();
		Statement statement;
		PlayerPlayingTime playerplayingtime = new PlayerPlayingTime();
		try {
			statement = WLogin.mySQL.getConnection().createStatement();
			ResultSet result = statement
					.executeQuery("SELECT * FROM playerplayingtime WHERE playername = '" + playerNameString + "';");
			while (result.next()) {
				playerplayingtime.setPlayerNameString(result.getString(1));
				playerplayingtime.setMin(result.getInt(2));
			}
			int playingtime = getTimeDifferenceMinutes(loginListHashMap.get(playerNameString), getNowTimestamp());
			if (playerplayingtime.getPlayerNameString() == null
					|| playerplayingtime.getPlayerNameString().length() == 0) {
				statement.executeUpdate("INSERT INTO playerplayingtime(playername,min)VALUES('" + playerNameString
						+ "'," + playingtime + ")");
				result.close();
				statement.close();
			} else {
				playingtime = playingtime + playerplayingtime.getMin();
				statement.executeUpdate("UPDATE playerplayingtime SET min = " + playingtime + " WHERE playername = '"
						+ playerNameString + "'");
				result.close();
				statement.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			WLogin.main.getLogger().warning("更新玩家 " + playerNameString + " 的已游玩时间失败");
		}
		loginListHashMap.remove(playerNameString);
		return true;
	}

	public static void changePassword(Player playerSender,String targePlayerNameString, String newPasswordString) {// 修改密码
		// TODO Auto-generated method stub
		PlayerPassword playerPassword = new PlayerPassword();
		Statement statement;
		try {
			statement = WLogin.mySQL.getConnection().createStatement();
			ResultSet result = statement
					.executeQuery("SELECT * FROM playerpassword WHERE playername = '" + targePlayerNameString + "';");
			while (result.next()) {
				playerPassword.setPlayerNameString(result.getString(1));
				playerPassword.setPlayerPasswordString(result.getString(2));
			}
			statement.executeUpdate("INSERT INTO playeroldpassword(playername,time,oldpassword,ip)VALUES('"
					+ targePlayerNameString + "', now() ,'" + playerPassword.getPlayerPasswordString() + "',inet_aton('"
					+ getPlayerIPAddress(playerSender) + "'))");
			statement.executeUpdate("UPDATE playerpassword SET playerpassword = '" + newPasswordString
					+ "' WHERE playername = '" + targePlayerNameString + "'");
			result.close();
			statement.close();
		} catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
			WLogin.main.getLogger().warning("更新玩家 " + targePlayerNameString + " 的密码失败");
		}
		return;
	}

	public static Timestamp getNowTimestamp() { // 获得现在的时间戳
		Timestamp timestamp=new Timestamp(new Date().getTime());
		return timestamp;
	}

	public static String getPlayerIPAddress(Player player) {// 获得玩家的IP地址
		return player.getAddress().getAddress().getHostAddress();
	}

	public static int getTimeDifferenceMinutes(Timestamp formatTime1, Timestamp formatTime2) {// 获得时间戳的分钟差
		long t1 = formatTime1.getTime();
		long t2 = formatTime2.getTime();
		int hours = (int) ((t1 - t2) / (1000 * 60 * 60));
		int minutes = (int) (((t1 - t2) / 1000 - hours * (60 * 60)) / 60 + hours * 60);
		return minutes;
	}

	public static void initTable() {// 初始化表
		BukkitRunnable bukkitRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Statement statement;
				try {
					statement = WLogin.mySQL.getConnection().createStatement();
					String sql = "CREATE TABLE IF NOT EXISTS `playerpassword`(" + "`playername` VARCHAR(40) NOT NULL,"
							+ "`password` VARCHAR(40) NOT NULL," + "`ip` bigint(20) NOT NULL,"
							+ "PRIMARY KEY ( `playername` )" + ")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
					statement.executeUpdate(sql);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WLogin.main.getLogger().warning("创建 playerpassword 失败");
				}
				try {
					statement = WLogin.mySQL.getConnection().createStatement();
					String sql = "CREATE TABLE IF NOT EXISTS `playerlogindata`(" + "`playername` VARCHAR(40) NOT NULL,"
							+ "`logintime` TIMESTAMP NOT NULL," + "`loginable` TINYINT(1) NOT NULL,"
							+ "`ip` bigint(20) NOT NULL,"
							+ "FOREIGN KEY (playername) REFERENCES playerpassword(playername)"
							+ ")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
					statement.executeUpdate(sql);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WLogin.main.getLogger().warning("创建 playerlogindata 失败");
				}
				try {
					statement = WLogin.mySQL.getConnection().createStatement();
					String sql = "CREATE TABLE IF NOT EXISTS `playerplayingtime`("
							+ "`playername` VARCHAR(40) NOT NULL," + "`min` INT NOT NULL,"
							+ "FOREIGN KEY (playername) REFERENCES playerpassword(playername)," + "UNIQUE (playername)"
							+ ")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
					statement.executeUpdate(sql);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WLogin.main.getLogger().warning("创建 playerplayingtime 失败");
				}
				try {
					statement = WLogin.mySQL.getConnection().createStatement();
					String sql = "CREATE TABLE IF NOT EXISTS `playeroldpassword`("
							+ "`playername` VARCHAR(40) NOT NULL," + "`time` TIMESTAMP NOT NULL,"
							+ "`oldpassword` VARCHAR(40) NOT NULL," + "`ip` bigint(20) NOT NULL,"
							+ "FOREIGN KEY (playername) REFERENCES playerpassword(playername)"
							+ ")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
					statement.executeUpdate(sql);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WLogin.main.getLogger().warning("创建 playeroldpassword 失败");
				}
				try {
					statement = WLogin.mySQL.getConnection().createStatement();
					String sql = "CREATE TABLE IF NOT EXISTS `playeristeenagers`("
							+ "`playername` VARCHAR(40) NOT NULL,"
							+ "FOREIGN KEY (playername) REFERENCES playerpassword(playername),"
							+ "UNIQUE ( `playername` )" + ")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
					statement.executeUpdate(sql);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WLogin.main.getLogger().warning("创建 playeristeenagers 失败");
				}
				try {
					statement = WLogin.mySQL.getConnection().createStatement();
					String sql = "CREATE TABLE IF NOT EXISTS `banplayerdata`(" + "`playername` VARCHAR(40) NOT NULL,"
							+ "`reason` VARCHAR(300) NOT NULL," + "`time` TIMESTAMP NOT NULL," + "`time_long` INT,"
							+ "FOREIGN KEY (playername) REFERENCES playerpassword(playername)"
							+ ")ENGINE=InnoDB DEFAULT CHARSET=utf8;";
					statement.executeUpdate(sql);
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WLogin.main.getLogger().warning("创建 banplayerdata 失败");
				}
				WLogin.main.getLogger().info("表初始化完成");
			}
		};

		bukkitRunnable.runTaskAsynchronously(WLogin.main);
	}
}