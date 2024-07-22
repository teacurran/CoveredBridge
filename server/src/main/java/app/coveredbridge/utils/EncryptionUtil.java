package app.coveredbridge.utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

public class EncryptionUtil {

  public static byte[] generateSalt16Byte() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }

  public static byte[] generateArgon2Sensitive(String password, byte[] salt) {
    int opsLimit = 4;
    int memLimit = 1048576;
    int outputLength = 32;
    int parallelism = 1;
    Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
      .withVersion(Argon2Parameters.ARGON2_VERSION_13) // 19
      .withIterations(opsLimit)
      .withMemoryAsKB(memLimit)
      .withParallelism(parallelism)
      .withSalt(salt);
    Argon2BytesGenerator gen = new Argon2BytesGenerator();
    gen.init(builder.build());
    byte[] result = new byte[outputLength];
    gen.generateBytes(password.getBytes(StandardCharsets.UTF_8), result, 0, result.length);
    return result;
  }

  public static String base64Encode(byte[] input) {
    return Base64.getEncoder().encodeToString(input);
  }

  public static byte[] base64Decode(String input) {
    return Base64.getDecoder().decode(input);
  }
}