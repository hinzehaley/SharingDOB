package hinzehaley.com.sharedob;

/**
 * Created by haleyhinze on 9/27/16.
 */
public interface NsdListener {

    void foundService(boolean serviceFound);
    void droppedService();
    void resolvingService();
    void droppedOwnService();
    void isConnected();
}
