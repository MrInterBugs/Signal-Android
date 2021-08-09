package org.thoughtcrime.securesms.linkpreview;

import android.os.StrictMode;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * Used to check the news source and weather or not it is impostor content.
 */
public class FakeNews {

  /**
   * The main method to be called from the LinkPreviewUtil class.
   *
   * @param url The URL to check.
   */
  public static void checkNews(@NonNull String url) {
    trustEveryone();
    disableThreads();

    System.out.println("This is important:" + url);
    String jsonUrl = "https://mrinterbugs.uk:5000/?article=" + url;

    Scanner scan = null;
    String tempResult = new String();
    try {
      URL finalUrl = new URL(jsonUrl);
      scan = new Scanner(finalUrl.openStream());
    } catch (IOException e) {
      System.out.println("A fatal IOException occurred");
      e.printStackTrace();
    }

    while (scan.hasNext())
    {
      tempResult += scan.nextLine();
    }
    scan.close();

    JSONObject jsonObject = null;
    String ecdsaVerifyString = new String();
    try {
      jsonObject = new JSONObject(tempResult);
      ecdsaVerifyString = (String) jsonObject.get("Signature");
    } catch (JSONException e) {
      System.out.println("A fatal JSONException occurred");
      e.printStackTrace();
    }

    KeyFactory keyFactory = null;
    Signature ecdsa = null;
    try {
      keyFactory = KeyFactory.getInstance("EC");
      ecdsa = Signature.getInstance("SHA256withECDSA");
    } catch (NoSuchAlgorithmException e) {
      System.out.println("A fatal NoSuchAlgorithmException occurred");
      e.printStackTrace();
    }

    String publicKeyPEM = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELDIqPmr5Oxklns5GgKTLrxfS0WcKIjaCCW2ZsjBpwxcnQAItqUKSh5GCfj0tW6jVm4adiCCAKIDOBWhvIYqZ1Q==";
    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

    ECPublicKey publicKey = null;
    try {
      publicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);
    } catch (InvalidKeySpecException e) {
      System.out.println("A fatal InvalidKeySpecException occurred");
      e.printStackTrace();
    }

    System.out.println(publicKey);

    try {
      ecdsa.initVerify(publicKey);
    } catch (InvalidKeyException e) {
      System.out.println("A fatal InvalidKeyException occurred");
      e.printStackTrace();
    }

    boolean result = false;
    try {
      ecdsa.update(url.getBytes("UTF-8"));
      result = ecdsa.verify(Base64.getDecoder().decode(ecdsaVerifyString));
    } catch (SignatureException e) {
      System.out.println("A fatal SignatureException occurred");
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      System.out.println("A fatal UnsupportedEncodingException occurred");
      e.printStackTrace();
    }
    try {
      if (result == true) {
        System.out.println("The article just shared from the " + jsonObject.get("Publisher") + " is defiantly not imposter news!");
      } else {
        System.out.println("The article just shared from the " + jsonObject.get("Publisher") + " can NOT be verified.");
      }
    } catch (JSONException e) {
      System.out.println("A fatal JSONException occurred");
      e.printStackTrace();
    }
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

  /**
   * Used for convince so that I do not have work with threads.
   */
  private static void disableThreads() {
    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);
  }
}
