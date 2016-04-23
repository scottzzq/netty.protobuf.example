package netty.protobuf.proxy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class ProxyServerMain {
	private static Logger logger = LoggerFactory.getLogger(ProxyServerMain.class.getName());
	public static void main(String[] args) throws InterruptedException, IOException, ParseException {
		// Create a Parser
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("h", "help", false, "Print this usage information");
		options.addOption("conf", "configure", true, "Print out confgure file information");
		options.addOption("s", "size", true, "Print out pool size information");
		options.addOption("p", "port", true, "Print out pool size information");
		// Parse the program arguments
		CommandLine commandLine = parser.parse(options, args);
		// Set the appropriate variables based on supplied option
		if (commandLine.hasOption('h')) {
			System.out.println("Help Message");
			System.exit(0);
		}

		int pool_size = 10;
		if (commandLine.hasOption("size")) {
			pool_size = Integer.parseInt(commandLine.getOptionValue("size"));
		}
		logger.info("ServerGroupPool Pool Size:" + pool_size);
		
		int port = 8088;
		if (commandLine.hasOption('p')){
			port =  Integer.parseInt(commandLine.getOptionValue("p"));
		}
		
		Properties pro = new Properties();
		InputStream in = null;
		if (commandLine.hasOption("conf")) {
			in = new FileInputStream(commandLine.getOptionValue("conf"));
			pro.load(in);
		} else {
			in = ProxyServerMain.class.getClassLoader().getResourceAsStream("server.conf");
			pro.load(in);
		}

		List<List<String>> svrList = new ArrayList<List<String>>();
		int total_level = Integer.parseInt(pro.getProperty("LEVEL_NUM"));
		for (int level_index = 0; level_index < total_level; ++level_index) {
			List<String> currentLevelSvrList = new ArrayList<String>();
			int level_num = Integer.parseInt(pro.getProperty("LEVEL_" + level_index + "_NUM"));
			for (int machine_index = 0; machine_index < level_num; ++machine_index) {
				String machine = pro.getProperty("LEVEL_" + level_index + "_IP_" + machine_index);
				currentLevelSvrList.add(machine);
			}
			svrList.add(currentLevelSvrList);
		}
		in.close();

		try {
			ServerGroupPool groupPool = new ServerGroupPool();
			for (int pool_index = 0 ; pool_index < pool_size; ++pool_index) {
				EventLoopGroup group = new NioEventLoopGroup(1);
				ServerGroup svrGroup = new ServerGroup();
				for (int level = 0; level < svrList.size(); ++level) {
					ServantGroup servantGroup = new ServantGroup();
					List<String> levelSvrList = svrList.get(level);
					for (String addr: levelSvrList){
						String pairs[] = addr.split("\\$");
						Servant svr = new Servant(group, new InetSocketAddress(pairs[0], Integer.parseInt(pairs[1])), 2);
						servantGroup.addServant(svr);
						logger.info("Level " + level + " Add Machine:" + addr);
					}
					svrGroup.addServantGroup(servantGroup);
				}
				groupPool.addGroup(svrGroup);
			}
			EventLoopGroup group = new NioEventLoopGroup();
			ProxyServer proxyServer = new ProxyServer();
			proxyServer.setPool(groupPool);
			proxyServer.start(group, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
