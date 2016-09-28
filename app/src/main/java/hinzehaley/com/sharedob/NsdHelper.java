
package hinzehaley.com.sharedob;

/*
Used sample code from
https://android.googlesource.com/platform/development/+/master/samples/training/NsdChat/src/com/example/android/nsdchat
 */

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class NsdHelper {

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    private NsdListener nsdListener;

    public static final String TAG = "NsdHelper";
    private String mUniqueServiceName = "NsdBirthdayCommunication";
    NsdServiceInfo mService;

    public NsdHelper(Context context, NsdListener nsdListener) {
        mContext = context;
        this.nsdListener = nsdListener;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    /**
     * Creates listener for when service is resolved
     */
    public void initializeNsd() {
        initializeResolveListener();
    }

    /**
     * Creates listener for when service is discovered
     */
    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            /**
             * If service is found, checks to see if it is the correct service coming from a different
             * device. If so, tries to resolve it
             * @param service
             */
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(Constants.SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mUniqueServiceName)) {
                    Log.d(TAG, "Same device: " + mUniqueServiceName);
                } else if (service.getServiceName().contains(Constants.SERVICE_NAME)){
                    try {
                        mNsdManager.resolveService(service, mResolveListener);
                        Log.d(TAG, "trying to resolve service");
                        if(nsdListener != null){
                            nsdListener.resolvingService();
                        }
                    }catch (IllegalArgumentException e){
                        e.printStackTrace();
                        Log.d(TAG, "unable to resolve");
                    }
                }
            }

            /**
             * If a service was lost, checks to see if it was the birthday sharing service for current
             * device or for another device. If so, lets NsdBirthdaySharingActivity know via nsdListener
             * @param service
             */
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    //Dropped own service
                    mService = null;
                    mUniqueServiceName = Constants.SERVICE_NAME;
                    if (nsdListener != null) {
                        nsdListener.droppedOwnService();
                    }
                }else if (mService != null && service != null) {
                    //dropped birthday sharing service from another device
                    if (service.getServiceName().contains(Constants.SERVICE_NAME) && !(service.getServiceName().equals(mService.getServiceName()))) {
                        if (nsdListener != null) {
                            nsdListener.droppedService();
                        }
                    }
                }else if (mService == null && service.getServiceName().contains(Constants.SERVICE_NAME)){
                    //dropped birthday sharing service from another device
                    if (nsdListener != null) {
                        nsdListener.droppedService();
                    }
                }

            }

            /**
             * stopped trying to discover services
             * @param serviceType
             */
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    /**
     * Initializes a listener to listen for resolution of service
     */
    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mUniqueServiceName)) {
                    //If it was own service, don't use it
                    Log.d(TAG, "Own service");
                    return;
                }
                mService = serviceInfo;
                Log.d(TAG, "resolved service");
                if(nsdListener != null){
                    nsdListener.foundService(true);
                }
            }
        };
    }

    /**
     * Listener for when a service is registered
     */
    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            /**
             * Service registered successfully. Gets name of service
             * @param NsdServiceInfo
             */
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mUniqueServiceName = NsdServiceInfo.getServiceName();
                Log.d(TAG, "Service registered: " + mUniqueServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.d(TAG, "Service registration failed: " + arg1);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.d(TAG, "Service unregistered: " + arg0.getServiceName());
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Service unregistration failed: " + errorCode);
            }

        };
    }

    /**
     * Attempts to register a service to the specified port. If
     * a registration request already exists, cancels it. Sets a registration
     * listener to tell if registration was successful
     * @param port
     */
    public void registerService(int port) {
        Log.d(TAG, "register service ");
        tearDown();
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(Constants.SERVICE_NAME);
        serviceInfo.setServiceType(Constants.SERVICE_TYPE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    /**
     * Attempts to discover services. If already trying to discover services,
     * cancels discovery request and requests again. Sets a discoveryListener
     * to tell if discovery was successful
     */
    public void discoverServices() {
        stopDiscovery();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    /**
     * Attempts to stop trying to discover services
     */
    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    /**
     * Unregisters the service if one is registered
     */
    public void tearDown() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }
}
