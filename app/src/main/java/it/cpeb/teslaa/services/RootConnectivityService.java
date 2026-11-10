package it.cpeb.teslaa.services;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import it.cpeb.teslaa.helpers.SettingConsts;

public class RootConnectivityService extends RootService implements Handler.Callback {

    private final static String TAG = "HU-Connectivity";
    static final int MSG_START = 1;
    static final int MSG_STOP = 2;

    static final int PORT = 80;
    static final int DNS_PORT = 1053;

    private Thread httpThread = null;
    private Thread dnsThread = null;

    private String tetherIfaceName = "";
    private Process dnsProc = null;
    private ConnectivityManager.NetworkCallback netCb = null;
    private Timer checkTimer = null;
    private String apiIp = null;
    private String apiHostname = null;

    @Override
    public boolean handleMessage(@NonNull Message message) {
        if (message.what == MSG_START) {
            loadData(message);
            startDnsServer();
            startHttpServer();
            return false;
        }

        if (message.what == MSG_STOP) {
            stopDnsServer();
            stopHttpServer();
            return false;
        }

        return false;
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Handler h = new Handler(Looper.getMainLooper(), this);
        Messenger m = new Messenger(h);
        return m.getBinder();
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        return false; // don't rebind
    }

    private void loadData(Message message) {
        if (message != null && message.getData() != null) {
            Bundle data = message.getData();
            apiIp = data.getString(SettingConsts.API_IP_KEY);
            apiHostname = data.getString(SettingConsts.API_HOSTNAME_KEY);
        } else {
            apiIp = SettingConsts.DEFAULT_API_IP;
            apiHostname = SettingConsts.DEFAULT_API_HOSTNAME;
        }
    }

    void startDnsServer() {
        try {
            if (checkTimer != null) {
                checkTimer.cancel();
                checkTimer = null;
            }
        } catch (IllegalStateException e) {
        }

        tetherIfaceName = "";
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        netCb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.e(TAG, "Net available: " + network);
                checkStartDns();
            }

            @Override
            public void onLost(Network network) {
                Log.e(TAG, "The application no longer has a default network. The last default network was " + network);
                checkStartDns();
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                Log.e(TAG, "The default network changed capabilities: " + networkCapabilities);
                checkStartDns();
            }

            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                Log.e(TAG, "The default network changed link properties: " + linkProperties);
                checkStartDns();
            }
        };

        NetworkRequest req = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(req, netCb);

        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "checking DNS start on timer");
                checkStartDns();
                if (!tetherIfaceName.isEmpty()) {
                    try {
                        if (checkTimer != null) {
                            checkTimer.cancel();
                            checkTimer = null;
                        }
                    } catch (IllegalStateException e) {
                    }
                }
            }
        };

        if (checkTimer == null) {
            checkTimer = new Timer();
        }
        try {
            checkTimer.schedule(tt, 0, 1000);
        } catch (IllegalStateException e) {
            checkTimer = new Timer();
            checkTimer.schedule(tt, 0, 1000);
        }
    }

    void checkStartDns() {
        if (!tetherIfaceName.isEmpty()) {
            try {
                Thread.sleep(5000);
                String disableCmd = "sysctl net.ipv6.conf." + tetherIfaceName + ".disable_ipv6=1";
                Log.i(TAG, "disabling IPV6: " + disableCmd);
                Process proc = Runtime.getRuntime().exec(disableCmd);
                proc.waitFor();
                if (proc.exitValue() != 0) {
                    Log.e(TAG, "error disabling IPv6: " + proc.getOutputStream().toString() + proc.getErrorStream().toString());
                }
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "error disabling ipv6", e);
            }
            return;
        }
        long start = System.currentTimeMillis();
        String tetherIfaceSubnet = "";
        while (tetherIfaceName.isEmpty() && System.currentTimeMillis() - start < 1000) {
            try {
                ConnectivityManager manager = (ConnectivityManager) this.getApplicationContext().getSystemService(ConnectivityManager.class);
                Method method = manager.getClass().getDeclaredMethod("getTetheredIfaces");
                String[] interfaces = (String[]) method.invoke(manager);
                if (interfaces != null) {
                    for (String intf : interfaces) {
                        Log.i(TAG, "TetherInterface: " + intf);
                        tetherIfaceName = intf;
                        break;
                    }
                } else {
                    Log.i(TAG, "NO TETHERING INTERFACES FOUND");
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(TAG, "Failed to get tethered interfaces", e);
            }

            if (tetherIfaceName.isEmpty()) {
                try {
                    Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                    while (ifaces.hasMoreElements()) {
                        NetworkInterface iface = ifaces.nextElement();
                        if (iface.isUp() && iface.getName().startsWith("ap_br_")) {
                            Log.i(TAG, "TetherInterface: " + iface.getName());
                            tetherIfaceName = iface.getName();
                            break;
                        }
                    }
                } catch (SocketException e) {
                    Log.e(TAG, "Failed to get interfaces", e);
                }
            }

            Log.i(TAG, "WAITING FOR TETHER");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        if (tetherIfaceName.isEmpty()) {
            Log.e(TAG, "tether not found");
            return;
        }

        NetworkInterface tetherIf = null;
        try {
            tetherIf = NetworkInterface.getByName(tetherIfaceName);
            for (InterfaceAddress a : tetherIf.getInterfaceAddresses()) {
                if (a.getAddress() instanceof Inet4Address) {
                    String addr = a.getAddress().getHostAddress();
                    String[] comps = addr.split("\\.");
                    addr = comps[0] + "." + comps[1] + "." + comps[2] + ".0";
                    tetherIfaceSubnet = addr + "/" + a.getNetworkPrefixLength();
                    break;
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "error getting tether iface: ", e);
        }
        Log.i(TAG, "tether subnet: " + tetherIfaceSubnet);
        if (tetherIfaceSubnet.isEmpty()) {
            Log.i(TAG, "tether subnet missing");
            tetherIfaceName = "";
            return;
        }

        NetworkInterface tunIf = null;
        try {
            tunIf = NetworkInterface.getByName("tun0");
        } catch (SocketException e) {
        }
        boolean hasTun = tunIf != null;

        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        connectivityManager.unregisterNetworkCallback(netCb);

        String[] iptablesSetupCommands = new String[]{
                "iptables -t nat -A PREROUTING -i " + tetherIfaceName + " -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:" + DNS_PORT,
                "iptables -t nat -A PREROUTING -i " + tetherIfaceName + " -p tcp --dport 443 -d "
                        + apiIp + " -j DNAT --to-destination 127.0.0.1:" + (JavaHTTPServer.PORT + 1),
                "iptables -t nat -A PREROUTING -i " + tetherIfaceName + " -p tcp ! --dport 443 -d "
                        + apiIp + " -j DNAT --to-destination 127.0.0.1",
                "sysctl -w net.ipv4.conf.all.route_localnet=1"
        };

        for (String iptablesSetupCommand : iptablesSetupCommands) {
            Log.d(TAG, "Setting up iptables: " + iptablesSetupCommand);

            try {
                Runtime.getRuntime().exec(iptablesSetupCommand);
            } catch (IOException e) {
                Log.e(TAG, "error setting up iptables", e);
            }
        }

        if (dnsProc == null || !dnsProc.isAlive()) {
            String[] localDnsNames = new String[]{
                    apiHostname,
                    "teslaa.androidwheels.com",
                    "connman.vn.tesla.services",
                    "hermes-prd.vn.tesla.services",
                    "www.tesla.com",
                    "www.teslamotors.com"
            };

            StringBuilder dnsmasqCmd = new StringBuilder("dnsmasq --port 1053 -k -d -R -S 8.8.8.8 ");
            for (String localDnsName : localDnsNames) {
                dnsmasqCmd.append(" -A /").append(localDnsName).append("/").append(apiIp).append(" ");
            }

            try {
                dnsProc = Runtime.getRuntime().exec(dnsmasqCmd.toString());
            } catch (IOException e) {
                Log.e(TAG, "dnsmasq error", e);
            }
        }

        while (true) {
            Log.i(TAG, "clearing table 61 rules");
            try {
                Process proc = Runtime.getRuntime().exec("ip rule del table 61");
                proc.waitFor();
                if (proc.exitValue() != 0) {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                break;
            }
        }
        try {
            Process proc = Runtime.getRuntime().exec("ip route flush table 61");
        } catch (IOException e) {
            Log.e(TAG, "error flushing table 61 routes");
        }
        Log.i(TAG, "cleared table 61 rules");

        try {
            Runtime.getRuntime().exec("ip rule add priority 0 from " + tetherIfaceSubnet + " lookup 61");
        } catch (IOException e) {
            Log.e(TAG, "error setting up WAP local routing rule", e);
        }
        try {
            Runtime.getRuntime().exec("ip rule add priority 0 to " + tetherIfaceSubnet + " lookup 61");
        } catch (IOException e) {
            Log.e(TAG, "error setting up WAP local routing rule2", e);
        }
        Log.i(TAG, "setup table 61 rules");

        if (hasTun) {
            // push traffic through tun0
            // NAT rules
            try {
                Runtime.getRuntime().exec("iptables -A POSTROUTING -t nat -o tun0 -j MASQUERADE ");
            } catch (IOException e) {
                Log.e(TAG, "error adding tun0 masquerade", e);
            }

            // iptables forward IN/OUT
            try {
                Process c = Runtime.getRuntime().exec("iptables -C FORWARD -i " + tetherIfaceName + " -o tun0 -j ACCEPT");
                c.waitFor();
                if (c.exitValue() != 0) {
                    Log.i(TAG, "iptables forward rule 1 doesn't exist, adding");
                    Runtime.getRuntime().exec("iptables -I FORWARD -i " + tetherIfaceName + " -o tun0 -j ACCEPT");
                }
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "error adding iptables forward rule", e);
            }
            try {
                Process c = Runtime.getRuntime().exec("iptables -C FORWARD -o " + tetherIfaceName + " -i tun0 -j ACCEPT");
                c.waitFor();
                if (c.exitValue() != 0) {
                    Log.i(TAG, "iptables forward rule 2 doesn't exist, adding");
                    Runtime.getRuntime().exec("iptables -I FORWARD -o " + tetherIfaceName + " -i tun0 -j ACCEPT");
                }
            } catch (IOException | InterruptedException e) {
                Log.e(TAG, "error adding iptables forward rule", e);
            }

            // IP routing
            try {
                Runtime.getRuntime().exec("ip route add default dev tun0 scope link table 61");
            } catch (IOException e) {
                Log.e(TAG, "error adding ip route 1", e);
            }
        } else {
            Log.e(TAG, "connectivity service may not work without user provided VPN");
        }

        // generic routing configs - applicable with/without VPN
        try {
            Runtime.getRuntime().exec("ip route add " + tetherIfaceSubnet + " dev " + tetherIfaceName + " scope link table 61");
        } catch (IOException e) {
            Log.e(TAG, "error adding ip route 2", e);
        }
        try {
            Runtime.getRuntime().exec("ip route add broadcast 255.255.255.255 dev " + tetherIfaceName + " scope link table 61");
        } catch (IOException e) {
            Log.e(TAG, "error adding ip route 2", e);
        }

        // disable ipv6
        try {
            String disableCmd = "sysctl net.ipv6.conf." + tetherIfaceName + ".disable_ipv6=1";
            Log.i(TAG, "disabling IPV6: " + disableCmd);
            Process proc = Runtime.getRuntime().exec(disableCmd);
            proc.waitFor();
            if (proc.exitValue() != 0) {
                Log.e(TAG, "error disabling IPv6: " + proc.getOutputStream().toString() + proc.getErrorStream().toString());
            }
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "error disabling ipv6", e);
        }
    }

    void stopDnsServer() {
        Log.d(TAG, "cleaning up dns");
        if (dnsProc == null) return;

        dnsProc.destroy();
        dnsProc = null;

        String[] iptablesDestroyCommands = new String[]{
                "iptables -t nat -D PREROUTING -i " + tetherIfaceName + " -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:" + DNS_PORT,
                "iptables -t nat -D PREROUTING -i " + tetherIfaceName + " -p tcp --dport 443 -d "
                        + apiIp + " -j DNAT --to-destination 127.0.0.1:" + (JavaHTTPServer.PORT + 1),
                "iptables -t nat -D PREROUTING -i " + tetherIfaceName + " -p tcp ! --dport 443 -d "
                        + apiIp + " -j DNAT --to-destination 127.0.0.1",
        };
        for (String iptablesDestroyCommand : iptablesDestroyCommands) {
            boolean done = false;
            while (!done) {
                try {

                    Process exit = Runtime.getRuntime().exec(iptablesDestroyCommand);
                    exit.waitFor();
                    if (exit.exitValue() != 0) {
                        done = true;
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "error cleaning up iptables", e);
                    done = true;
                }
            }
        }
        Log.d(TAG, "done cleaning up dns");
    }

    void startHttpServer() {
        if (httpThread != null) return;

        httpThread = new Thread(() -> {
            try {
                ServerSocket serverConnect = new ServerSocket(PORT);
                while (true) {
                    handleClientConnection(serverConnect.accept());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            httpThread = null;
        });
        httpThread.start();
    }

    void handleClientConnection(Socket connect) {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            // we get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());

            in.readLine();

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("Content-Length: 45");
            out.println("Connection: keep-alive");
            out.println("Server: nginx");
            out.println("Date: " + new Date());
            out.println("X-ConnMan-Status: online");
            out.println(); // blank line between headers and content, very important !
            out.print("<html>\n<head>\n</head>\n<body>\n</body>\n</html>\n");
            out.flush(); // flush character output stream buffer
            out.close();
            in.close();
            connect.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stopHttpServer() {
        if (httpThread != null) {
            httpThread.interrupt();
        }
    }
}