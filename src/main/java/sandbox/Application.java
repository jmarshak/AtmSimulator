package sandbox;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Main starting point for interacting with the atm simulator.  Various actions are supported
 * creating an account, logging in, viewing a balance, deposits and withdrawals
 */
@Slf4j
public class Application {

    // see printUsage below for expected inputs and action
    public static void main(String[] args) {

        if (args.length == 0) {
            printUsage();
            return;
        }

        Optional<String> action = parseArg(args, "action");

        if (action.isEmpty()) {
            log.info("no action arg found");
            printUsage();
            return;
        }

        try (AtmSimulatorImpl atm = new AtmSimulatorImpl()) {

            Optional<String> token = parseArg(args, "token");
            Optional<Integer> amount = parseArg(args, "amount")
                    .map(Integer::parseInt);
            switch (action.get().toLowerCase()) {
                case "login":
                    login(args, atm);
                    break;
                case "createaccount":
                    createAccount(args);
                    break;
                case "viewbalance":
                    if (token.isEmpty()) {
                        log.warn("must provide a valid token to view a balance");
                        return;
                    }
                    log.info("your balance is {} cents", atm.viewBalance(token.get()));
                    break;
                case "deposit":
                    if (token.isEmpty()) {
                        log.warn("must provide a valid token to make a deposit");
                        return;
                    }
                    if (amount.isEmpty()) {
                        log.warn("must provide a valid amount to make a deposit");
                        return;
                    }
                    atm.deposit(token.get(), amount.get());
                    break;
                case "withdraw":
                    if (token.isEmpty()) {
                        log.warn("must provide a valid token to make a withdrawal");
                        return;
                    }
                    if (amount.isEmpty()) {
                        log.warn("must provide a valid amount to make a withdrawal");
                        return;
                    }
                    atm.withdraw(token.get(), amount.get());
                    break;
                default:
                    printUsage();
                    break;
            }
        } catch (Exception ex) {
            log.error("caught exception", ex);
        }
    }

    // ensure a username and password are present, then create the account
    private static boolean createAccount(String[] args) {
        Optional<String> username = parseArg(args, "username");
        Optional<String> pin = parseArg(args, "pin");
        if (username.isEmpty() || pin.isEmpty()) {
            log.warn("cannot create an acccount without a username and pin");
            printUsage();
            return true;
        }

        try (SqliteDao sqliteDao = new SqliteDao()) {
            sqliteDao.createAccount(username.get(), pin.get());
            log.info("user created successfully");
            return true;
        } catch (SQLException ex) {
            log.error("user creation failed.", ex);
            return false;
        }
    }

    // login an account by the username and pin, and print out the token
    private static boolean login(String[] args, AtmSimulatorImpl atm) {
        Optional<String> username = parseArg(args, "username");
        Optional<String> pin = parseArg(args, "pin");
        if (username.isEmpty() || pin.isEmpty()) {
            log.warn("cannot login without a username and pin");
            printUsage();
            return true;
        }

        Optional<String> token = atm.login(username.get(), pin.get());
        token.ifPresentOrElse(t -> log.info("login success, your session token is {}", t),
                () -> log.warn("login failed."));

        return false;
    }

    private static Optional<String> parseArg(String[] args, String expectedArgName) {
        return Arrays.stream(args)
                .filter(a -> a.contains("="))
                .map(a -> a.split("="))
                .filter(argArray -> expectedArgName.equalsIgnoreCase(argArray[0]))
                .map(argArray -> argArray[1])
                .filter(StringUtils::isNotBlank)
                .findFirst();
    }

    private static void printUsage() {
        log.info("ATM Simulator.  Can create accounts, view balances, make deposits (in cents), and withdrawals (in cents).");
        log.info("First login to get a valid token, and use that to make deposit, withdraw or view the balance.");
        log.info("action=<CreateAccount|Login|ViewBalance|Deposit|Withdraw");
        log.info("examples");
        log.info("action=CreateAccount username=<username> pin=<pin>");
        log.info("action=Login username=<username> pin=<pin>");
        log.info("action=ViewBalance token=<token>");
        log.info("action=Deposit token=<token> amount=<amount>");
        log.info("action=Withdraw token=<token> amount=<amount>");
    }
}
