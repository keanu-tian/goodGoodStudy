package com.cxd.zookeeper.deadLock;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 * ZooKeeper API 获取子节点列表，使用同步(sync)接口。
 * @author [银时](mailto:nileader@gmail.com)
 */
public class ZooKeeper_GetChildren_API_Sync_Usage implements Watcher {

    private CountDownLatch connectedSemaphore = new CountDownLatch( 1 );
    private static CountDownLatch _semaphore = new CountDownLatch( 1 );
    private ZooKeeper zk;

    ZooKeeper createSession( String connectString, int sessionTimeout, Watcher watcher ) throws IOException {
        ZooKeeper zookeeper = new ZooKeeper( connectString, sessionTimeout, watcher );
        try {
            connectedSemaphore.await();
        } catch ( InterruptedException e ) {
        }
        return zookeeper;
    }

    /** create path by sync */
    void createPath_sync( String path, String data, CreateMode createMode ) throws IOException, KeeperException, InterruptedException {

        if ( zk == null ) {
            zk = this.createSession( "localhost:2181", 5000, this );
        }
        zk.create( path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, createMode );
    }

    /** Get children znodes of path and set watches */
    List getChildren( String path ) throws KeeperException, InterruptedException, IOException{

        System.out.println( "===Start to get children znodes.===" );
        if ( zk == null ) {
            zk = this.createSession( "localhost:2181", 5000, this );
        }
        return zk.getChildren( path, true );
    }

    /**
     * console print:
     11-->Receive watched event：WatchedEvent state:SyncConnected type:None path:null
     ===Start to get children znodes.===
     [c1]
     11-->Receive watched event：WatchedEvent state:SyncConnected type:NodeChildrenChanged path:/get_children_test
     ===Start to get children znodes.===
     11--->[c1, c2]
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main( String[] args ) throws IOException, InterruptedException {

        ZooKeeper_GetChildren_API_Sync_Usage sample = new ZooKeeper_GetChildren_API_Sync_Usage();
        String path = "/get_children_test";

        try {

            sample.createPath_sync( path, "", CreateMode.PERSISTENT );
            sample.createPath_sync( path + "/c1", "", CreateMode.PERSISTENT );
            List childrenList = sample.getChildren( path );
            System.out.println( childrenList );
            //Add a new child znode to test watches event notify.
            sample.createPath_sync( path + "/c2", "", CreateMode.PERSISTENT );
            _semaphore.await();
        } catch ( KeeperException e ) {
            System.err.println( "error: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    /**
     * Process when receive watched event
     */
    @Override
    public void process( WatchedEvent event ) {
        System.out.println( Thread.currentThread().getId() + "-->Receive watched event：" + event );
        if ( KeeperState.SyncConnected == event.getState() ) {

            if( EventType.None == event.getType() && null == event.getPath() ){
                connectedSemaphore.countDown();
            }else if( event.getType() == EventType.NodeChildrenChanged ){
                //children list changed
                try {
                    System.out.println(  Thread.currentThread().getId() + "--->" + this.getChildren( event.getPath() ) );
                    _semaphore.countDown();
                } catch ( Exception e ) {}
            }

        }
    }
}