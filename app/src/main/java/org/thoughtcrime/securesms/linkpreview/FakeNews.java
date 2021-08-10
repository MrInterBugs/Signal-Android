package org.thoughtcrime.securesms.linkpreview;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

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
    String jsonUrl = "https://mrinterbugs.uk:5000/?article=" + url;

    URL finalUrl = null;
    try {
      finalUrl = new URL(jsonUrl);
    } catch (IOException e) {
      System.out.println("A fatal IOException occurred");
      e.printStackTrace();
    }

    AsyncTask<URL, Void, String> checkNewsURL = new CheckNewsURL().execute(finalUrl);
    String tempResult = null;
    try {
      tempResult = checkNewsURL.get();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

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

    final String publicKeyPEM = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELDIqPmr5Oxklns5GgKTLrxfS0WcKIjaCCW2ZsjBpwxcnQAItqUKSh5GCfj0tW6jVm4adiCCAKIDOBWhvIYqZ1Q==";
    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

    ECPublicKey publicKey = null;
    try {
      publicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);
    } catch (InvalidKeySpecException e) {
      System.out.println("A fatal InvalidKeySpecException occurred");
      e.printStackTrace();
    }

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
}