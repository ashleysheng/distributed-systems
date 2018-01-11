package app_kvEcs;

public interface ECSCommInterface {

    /**
     * @param numberOfNodes nodes to initiate
     * @param cacheSize cache size for each server node
     * @param replacementStrategy FIFO, LRU or LFU strategy
     * Randomly choose <numberOfNodes> servers from the available machines and start the KVServer
     * by issuing an SSH call to the respective machine. This call launches the storage server
     * with the specified cache size and replacement strategy. For simplicity, locate the KVServer.jar
     * in the same directory as the ECS. All storage servers are initialized with the metadata and
     * remain in state stopped.
     */
    public void initService(int numberOfNodes, int cacheSize, String replacementStrategy);

    /**
     * Starts the storage service by calling start()
     * on all KVServer instances that participate in the service.
     */
    public void start();

    /**
     Stops the service; all participating KVServers are stopped
     * for processing client requests but the processes remain running.
     */
    public void stop();


    /**
     *Stops all server instances and exits the remote processes.
     */
    public void shutDown();


    /**
     * @param cacheSize size of cache to add
     * @param replacementStrategy replacement strategy
     *
     * Create a new KVServer with the specified cache size and replacement strategy
     * and add it to the storage service at an arbitrary position.
     */
    public void addNode(int cacheSize, String replacementStrategy) throws Exception;

    public void removeNode(int index) throws Exception;
}
