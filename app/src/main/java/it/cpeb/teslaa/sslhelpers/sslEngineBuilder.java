package it.cpeb.teslaa.sslhelpers;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import it.cpeb.teslaa.helpers.Log;

/**
 * Created by Emil on 25/03/2018.
 */

public class sslEngineBuilder {

    /**
     * File name of the Android Auto head unit certificate (PKCS#12, certificate + private key).
     * No certificate is bundled with the app: users must provide their own and place it in the
     * app's external files folder, i.e. Android/data/it.cpeb.teslaa/files/aa_cert.p12
     * See README.md for details.
     */
    public static final String AA_CERT_FILE = "aa_cert.p12";

    /** Password of the PKCS#12 file (use it with -passout when creating the file, see README). */
    public static final String AA_CERT_PASSWORD = "webaa";

    public static File getAACertFile(Context context) {
        return new File(context.getExternalFilesDir(null), AA_CERT_FILE);
    }

    /**
     * Builds the SSL engine for the Android Auto connection using the user-provided certificate.
     * Throws a FileNotFoundException with a user-readable message if no certificate is installed.
     */
    public static SSLEngine Builder(Context context) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException, KeyManagementException {
        File certFile = getAACertFile(context);
        if (!certFile.exists()) {
            throw new FileNotFoundException("No Android Auto certificate found. Copy your own head unit certificate (PKCS#12) to "
                    + certFile.getAbsolutePath() + " - see the README for details.");
        }

        KeyStore instance = KeyStore.getInstance("PKCS12");
        try (InputStream certStream = new FileInputStream(certFile)) {
            instance.load(certStream, AA_CERT_PASSWORD.toCharArray());
        }
        KeyManagerFactory instance2 = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        instance2.init(instance, AA_CERT_PASSWORD.toCharArray());
        Log.d("SSL", "Before creating ssl context");

        SSLContext instance3 = SSLContext.getInstance("TLSv1.2");
        Log.d("SSL", "Alfter creating ssl context");
        instance3.init(instance2.getKeyManagers(), new TrustManager[]{new adb()}, new SecureRandom());
        Log.d("SSL", "After init");
        SSLEngine createSSLEngine = instance3.createSSLEngine();
        Log.d("SSL", "After create engine");
        createSSLEngine.setEnabledProtocols(new String[]{"TLSv1.2"});
        Log.d("SSL", "After enable protocol");
        createSSLEngine.setNeedClientAuth(true);
        return createSSLEngine;

    }


    public static SSLContext getSSlContext(InputStream openRawResource, String pass) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        try (openRawResource) {
            KeyStore instance = KeyStore.getInstance("PKCS12");
            instance.load(openRawResource, pass.toCharArray());
            KeyManagerFactory instance2 = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            instance2.init(instance, "aa".toCharArray());
            Log.d("SSL", "Before creating ssl context");

            SSLContext instance3 = SSLContext.getInstance("TLSv1.2");
            Log.d("SSL", "Alfter creating ssl context");
            instance3.init(instance2.getKeyManagers(), new TrustManager[]{new adb()}, new SecureRandom());
            Log.d("SSL", "After init");
            return instance3;
        }
    }


}
