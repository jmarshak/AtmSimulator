package sandbox;

import java.util.Optional;

/**
 * The methods a class must implement to fulfill the requirements of this challenge
 */
public interface AtmSimulator {
    Optional<String> login(String username, String pin);

    long viewBalance(String token);

    boolean deposit(String token, long amount);

    boolean withdraw(String token, long amount);
}
