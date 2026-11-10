package it.cpeb.teslaa.sslhelpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Enumeration;

import it.cpeb.teslaa.R;
import it.cpeb.teslaa.helpers.Log;
import it.cpeb.teslaa.helpers.SettingConsts;

public class CertManager {
    private static final String DYNAMIC_SERVER_CRT_NAME = "dynamic-server-cert.p12";
    private static final String TAG = "CertManager";

    public static InputStream getServerSslCert(Context context) {
        File dynamicCert = new File(context.getFilesDir(), DYNAMIC_SERVER_CRT_NAME);
        if (dynamicCert.exists()) {
            try {
                return context.openFileInput(DYNAMIC_SERVER_CRT_NAME);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "dynamic cert not found, falling back to builtin cert");
            }
        }

        return context.getResources().openRawResource(R.raw.server);
    }

    public static void updateServerSslCert(Context context) throws IOException {
        InputStream crtFile = getServerSslCert(context);
        if (isCertExpiringSoon(crtFile, "aa")) {
            doUpdateServerSslCert(context);
        }
    }

    private static boolean isCertExpiringSoon(InputStream certP12, String password) {
        try (certP12) {
            KeyStore p12Store = KeyStore.getInstance("PKCS12");
            p12Store.load(certP12, password.toCharArray());
            Enumeration<String> aliases = p12Store.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                X509Certificate cert = (X509Certificate) p12Store.getCertificate(alias);
                if(cert == null) continue;

                cert.checkValidity(Date.from(LocalDate.now().plusDays(7)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant()));

            }
        } catch (CertificateExpiredException e) {
            return true;
        } catch (CertificateNotYetValidException e) {
            Log.e(TAG, "Unexpected cert not yet valid, likely a phone issue", e);
            return true;
        } catch (CertificateException | IOException | KeyStoreException|NoSuchAlgorithmException e) {
            Log.e(TAG, "Unexpected error while checking cert validity, assuming expired", e);
            return true;
        }
        return false;
    }


    public static void doUpdateServerSslCert(Context context) throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String crtUrlStr = prefs.getString(SettingConsts.CRT_URL_KEY, SettingConsts.DEFAULT_CRT_URL);

        URL url;
        try {
            url = new URL(crtUrlStr);
        } catch (MalformedURLException e) {
            Log.e(TAG, "invalid dynamic cert URL", e);
            throw e;
        }

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            Log.e(TAG, "error connecting to dynamic cert URL", e);
            throw e;
        }

        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.e(TAG, "invalid HTTP response for dynamic cert, expecting OK, got: " + urlConnection.getResponseCode());
            throw new IOException("invalid http response");
        }


        try (InputStream inputStream = urlConnection.getInputStream();
             FileOutputStream outputStream = context.openFileOutput(DYNAMIC_SERVER_CRT_NAME, Context.MODE_PRIVATE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                inputStream.transferTo(outputStream);
            } else {
                byte[] buf = new byte[8192];
                int length;
                while ((length = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, length);
                }
            }
        }
    }
}
