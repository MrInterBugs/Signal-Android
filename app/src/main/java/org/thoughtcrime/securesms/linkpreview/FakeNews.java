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
    try{
      String tempResult = getResult(getJson(url));

      jsonObject = new JSONObject(tempResult);
      String ecdsaVerifyString = getSign();

      Signature ecdsa = Signature.getInstance("SHA256withECDSA");

      X509EncodedKeySpec keySpec = getKeySpec();
      initialiseVerify(keySpec, ecdsa);

      boolean result = getFinalResult(url, ecdsaVerifyString, ecdsa);
      displayResult(result);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Used to show the user and log the the result of the news check.
   *
   * @param result The final result on weather or not it is impostor news.
   * @throws JSONException If it can not get the publisher from the JSON.
   */
  private static void displayResult(boolean result) throws JSONException {
    SupportMapFragment mapFragment = new SupportMapFragment();
    Snackbar mySnackbar;

    if (result) {
      String valid = "The article just shared from the " + getPublisher() + " is defiantly not imposter news!";
      System.out.println(valid);
      mySnackbar = Snackbar.make(mapFragment.requireView(), valid, BaseTransientBottomBar.LENGTH_LONG);
    } else {
      String invalid = "The article just shared from the " + getPublisher() + " can NOT be verified.";
      System.out.println(invalid);
      mySnackbar = Snackbar.make(mapFragment.requireView(), invalid, BaseTransientBottomBar.LENGTH_LONG);
    }
    mySnackbar.show();
  }

  /**
   * Used to return the publisher from the JSON result.
   *
   * @return The name of the publishing company.
   * @throws JSONException Thrown if no publishing company can be found.
   */
  private static String getPublisher() throws JSONException {
    return (String) jsonObject.get("Publisher");
  }

  /**
   * Checks the signature received from the verification using the public key.
   *
   * @param url the url of the news source.
   * @param ecdsaVerifyString the verification string from the server.
   * @param ecdsa the signature containing the public key.
   * @return True or False depending on the verification.
   * @throws SignatureException Required for both methods.
   */
  private static boolean getFinalResult(String url, String ecdsaVerifyString, Signature ecdsa) throws SignatureException {
    ecdsa.update(url.getBytes(StandardCharsets.UTF_8));
    return ecdsa.verify(Base64.getDecoder().decode(ecdsaVerifyString));
  }

  /**
   * Used to start the verification process from the public key.
   *
   * @param keySpec The required keySpec for this encryption.
   * @param ecdsa the signature to be verified.
   * @throws NoSuchAlgorithmException thrown if the algorithm EC can not be found.
   * @throws InvalidKeySpecException thrown if the keyFactory can not generate a valid key from the keySpec.
   * @throws InvalidKeyException thrown if the key fails to initialise.
   */
  private static void initialiseVerify(X509EncodedKeySpec keySpec, Signature ecdsa) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(keySpec);
    ecdsa.initVerify(publicKey);
  }

  /**
   * Creates a keySpec for the public key.
   *
   * @return the said keySpec.
   */
  private static X509EncodedKeySpec getKeySpec() {
    final String publicKeyPEM = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELDIqPmr5Oxklns5GgKTLrxfS0WcKIjaCCW2ZsjBpwxcnQAItqUKSh5GCfj0tW6jVm4adiCCAKIDOBWhvIYqZ1Q==";
    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
    return new X509EncodedKeySpec(encoded);
  }

  /**
   * Used to get the verification signature from the signing server.
   *
   * @return a String of the Base64 encoded signature.
   * @throws JSONException thrown if the signature can't be found.
   */
  private static String getSign() throws JSONException {
    return (String) jsonObject.get("Signature");
  }

  /**
   * Calls an AsyncTask to get the response from the signing server.
   *
   * @param finalUrl the URL to make a request to the signing server.
   * @return the response from the server.
   * @throws ExecutionException thrown if the response can't be got.
   * @throws InterruptedException thrown if getting the response is interrupted.
   */
  private static String getResult(URL finalUrl) throws ExecutionException, InterruptedException {
    AsyncTask<URL, Void, String> checkNewsURL = new CheckNewsURL().execute(finalUrl);
    return checkNewsURL.get();
  }

  /**
   * Creates a URL to ping the verification server from the URL provided.
   *
   * @param url the news source to be checked.
   * @return a new URL to be connected too.
   * @throws MalformedURLException thrown if the string is not valid to make a URL from.
   */
  private static URL getJson(String url) throws MalformedURLException {
    String jsonUrl = "https://mrinterbugs.uk:5000/?article=" + url;
    return new URL(jsonUrl);
  }
}