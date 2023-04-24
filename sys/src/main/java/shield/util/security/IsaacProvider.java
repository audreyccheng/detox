package shield.util.security;

import java.security.PrivilegedAction;

public class IsaacProvider extends java.security.Provider	{

  private final static String name = "IsaacPRNG";
  private final static  String info = "Isaac PRNG";
  private final static double version = 0.0;

  public IsaacProvider () {
    super (name, version, info);

    java.security.AccessController.doPrivileged(
        (PrivilegedAction) () -> {
          put ("SecureRandom.IsaacPRNG", "IsaacPRNG");
          return (null);
        }
    );
  }
}