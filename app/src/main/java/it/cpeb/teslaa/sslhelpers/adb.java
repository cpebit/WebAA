package it.cpeb.teslaa.sslhelpers;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

import it.cpeb.teslaa.helpers.Log;

final class adb
        implements X509TrustManager {
    adb() {
    }

    @Override
    public final void checkClientTrusted(X509Certificate[] arrx509Certificate, String string2) {
        Log.d((String) "NaiveTrustManager", (String) ("Check client: " + Arrays.toString(arrx509Certificate)));
    }

    @Override
    public final void checkServerTrusted(X509Certificate[] arrx509Certificate, String string2) {
//        Log.d((String)"NaiveTrustManager", (String)("Check server: " + Arrays.toString(arrx509Certificate)));
    }

    @Override
    public final X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
