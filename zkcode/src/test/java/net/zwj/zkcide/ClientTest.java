package net.zwj.zkcide;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

public class ClientTest {
	
	@Test
	public void client() throws IOException, KeeperException, InterruptedException {
		ZooKeeper zk = new  ZooKeeper("localhost:2181", 5000, (e) -> {
			System.out.println(e);
		} );
		zk.create("/test", new String("zwj").getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}

}
