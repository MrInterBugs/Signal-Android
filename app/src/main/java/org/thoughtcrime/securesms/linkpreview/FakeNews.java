package org.thoughtcrime.securesms.linkpreview;

import android.os.StrictMode;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.EncodedKeySpec;
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

    String publicKeyPEM = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAELkQAGX3lFaxZU/2l8NdPp0GK9Cpgi5UaVYfoPEookXzgeWjVlX+tZIp8gqzuXERbPeX5opbXkTaQ/dvvY2UCJg==";
    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

    System.out.println("This is important:" + url);
    String jsonUrl = "https://mrinterbugs.uk:5000/?article=" + url;

    try {
      URL finalUrl = new URL(jsonUrl);

      Scanner scan = new Scanner(finalUrl.openStream());
      String  tempResult  = new String();

      while (scan.hasNext())
      {
        tempResult += scan.nextLine();
      }
      scan.close();

      JSONObject jsonObject  = new JSONObject(tempResult);
      String ecdsaVerifyString = (String) jsonObject.get("Signature");

      String publisher = (String) jsonObject.get("Publisher");
      System.out.println(publisher);


      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
      PublicKey publicKey = keyFactory.generatePublic(keySpec);
      System.out.println(publicKey);

      Signature sign = Signature.getInstance("SHA256withECDSA");
      sign.initVerify(publicKey);
      sign.update(url.getBytes("UTF-8"));
      boolean result = sign.verify(Base64.getDecoder().decode(ecdsaVerifyString));
      System.out.println(result);

    } catch(Exception e) {
      System.out.println("Fatal error has occurred.");
      System.out.println(e);
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
