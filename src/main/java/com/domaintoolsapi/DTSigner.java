package com.domaintoolsapi;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DTSigner {
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  private String api_username;
  private String api_key;
  private SimpleDateFormat timeFormatter;

  protected DTSigner(String api_username, String api_key) {
    this.api_username = api_username;
    this.api_key = api_key;
    this.timeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  }

  protected String timestamp() {
    Date now = new Date();
    return this.timeFormatter.format(now);
  }

  protected String getHexString(byte[] b) {
    String result = "";
    for (int i=0; i < b.length; i++) {
	    result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring(1);
    }
    return result;
  }

  protected String sign(String timestamp, String uri)
    throws java.security.SignatureException {
    String Result;
    try {
	    String data = new String(this.api_username + timestamp + uri);
	    SecretKeySpec signingKey = new SecretKeySpec(this.api_key.getBytes(),
			HMAC_SHA1_ALGORITHM);
	    Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
	    mac.init(signingKey);
	    byte[] rawSignature = mac.doFinal(data.getBytes());
	    Result = this.getHexString(rawSignature);
    } catch(Exception e) {
	    throw new java.security.SignatureException("Failed to generate HMAC : "
			+ e.getMessage());
    }
    return Result;
  }
}