package org.thoughtcrime.securesms.linkpreview;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

class CheckNewsURL extends AsyncTask<URL, Void, String> {
    @Override
    protected String doInBackground(URL... urls) {
        trustEveryone();
        Scanner scan = null;
        String tempResult = new String();
        try {
            scan = new Scanner(urls[0].openStream());
        } catch (IOException e) {
            System.out.println("A fatal IOException occurred");
            e.printStackTrace();
        }

        while (scan.hasNext())
        {
            tempResult += scan.nextLine();
        }
        scan.close();
        return tempResult;
    }

    /**
     * Used to trust all HTTPS as my server is using a self signed cert.
     */
    private static void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{ new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException
                {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }
}