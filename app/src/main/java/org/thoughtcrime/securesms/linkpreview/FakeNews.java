package org.thoughtcrime.securesms.linkpreview;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
  private static JSONObject jsonObject;

  /**
   * The main method to be called from the LinkPreviewUtil class.
   *
   * @param url The URL to check.
   */
  public static void checkNews(@NonNull String url) {
    String jsonUrl = "https://mrinterbugs.uk:5000/?article=" + url;

    try{
      String tempResult = getResult(getJson(jsonUrl));

      jsonObject = new JSONObject(tempResult);
      String ecdsaVerifyString = getSign();

      Signature ecdsa = getECDSA();
      X509EncodedKeySpec keySpec = getKeySpec();
      initialiseVerify(keySpec, ecdsa);

      boolean result = getFinalResult(url, ecdsaVerifyString, ecdsa);
      displayResult(result);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void displayResult(boolean result) throws JSONException {
    SupportMapFragment mapFragment = new SupportMapFragment();
    Snackbar mySnackbar;

    if (result) {
      String valid = "The article just shared from the " + getPublisher() + " is defiantly not imposter news!";
      mySnackbar = Snackbar.make(mapFragment.requireView(), valid, BaseTransientBottomBar.LENGTH_LONG);
      System.out.println(valid);
    } else {
      String invalid = "The article just shared from the " + getPublisher() + " can NOT be verified.";
      mySnackbar = Snackbar.make(mapFragment.requireView(), invalid, BaseTransientBottomBar.LENGTH_LONG);
      System.out.println(invalid);
    }
    mySnackbar.show();
  }

  private static String getPublisher() throws JSONException {
    return (String) jsonObject.get("Publisher");
  }

  private static boolean getFinalResult(String url, String ecdsaVerifyString, Signature ecdsa) throws SignatureException {
    ecdsa.update(url.getBytes(StandardCharsets.UTF_8));
    return ecdsa.verify(Base64.getDecoder().decode(ecdsaVerifyString));
  }

  private static void initialiseVerify(X509EncodedKeySpec keySpec, Signature ecdsa) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);
    ecdsa.initVerify(publicKey);
  }

  private static X509EncodedKeySpec getKeySpec() {
    final String publicKeyPEM = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELDIqPmr5Oxklns5GgKTLrxfS0WcKIjaCCW2ZsjBpwxcnQAItqUKSh5GCfj0tW6jVm4adiCCAKIDOBWhvIYqZ1Q==";
    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
    return new X509EncodedKeySpec(encoded);
  }

  private static Signature getECDSA() throws NoSuchAlgorithmException {
    return Signature.getInstance("SHA256withECDSA");
  }

  private static String getSign() throws JSONException {
    return (String) jsonObject.get("Signature");
  }

  private static String getResult(URL finalUrl) throws ExecutionException, InterruptedException {
    AsyncTask<URL, Void, String> checkNewsURL = new CheckNewsURL().execute(finalUrl);
    return checkNewsURL.get();
  }

  private static URL getJson(String jsonUrl) throws MalformedURLException {
    return new URL(jsonUrl);
  }
}