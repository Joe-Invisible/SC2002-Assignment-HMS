package hms.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Cryptography class provides a utility method to hash a given input string 
 * using the MD5 algorithm. This class is designed for quick and simple hashing needs.
 * 
 * <p><b>Note:</b> MD5 is not recommended for cryptographic security as it is vulnerable to 
 * collision attacks. </p>
 */
// Class to handle hashing of password using MD5
// MD5 is not the most secure way of hashing but should be fine for our project
public class Cryptography {
	/**
	 * Hashes a string with the MD5 algorithm.
	 * @param input the string to be hashed
	 * @return a string hashed with MD5 algorithm.
	 */
	public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            
            // Convert bytes to hex format
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
