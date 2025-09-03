package gitlet;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static gitlet.GitletConstants.INCORRECT_OPERANDS_WARNING;
import static gitlet.GitletConstants.UNINITIALIZED_WARNING;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (firstArg) {
            case "init":
                if (restArgs.length == 0) {
                   Repository.init();
                } else {
                    System.out.println(INCORRECT_OPERANDS_WARNING);
                }
                break;
            case "add":
                commandRunner(restArgs.length == 1, Repository::add, restArgs[0]);
                break;
            case "commit":
                commandRunner(restArgs.length == 1, Repository::commit, restArgs[0]);
                break;
            case "rm":
                commandRunner(restArgs.length == 1, Repository::rm, restArgs[0]);
                break;
            case "log":
                commandRunner(restArgs.length == 0, Repository::log);
                break;
            case "global-log":
                commandRunner(restArgs.length == 0, Repository::globalLog);
                break;
            case "checkout":
                commandRunner(restArgs.length >= 1 && restArgs.length <= 3, Repository::checkout, restArgs);
                break;
            case "branch":
                commandRunner(restArgs.length == 1, Repository::branch, restArgs[0]);
                break;
            case "find":
                commandRunner(restArgs.length == 1, Repository::find, restArgs[0]);
                break;
            case "status":
                commandRunner(restArgs.length == 0, Repository::status);
                break;
            case "rm-branch":
                commandRunner(restArgs.length == 1, Repository::removeBranch, restArgs[0]);
                break;
            case "reset":
                commandRunner(restArgs.length == 1, Repository::reset, restArgs[0]);
                break;
            case "merge":
                commandRunner(restArgs.length == 1, Repository::merge, restArgs[0]);
                break;
            case "add-remote":
                commandRunner(restArgs.length == 2, RemoteUtils::addRemote, restArgs[0], restArgs[1]);
                break;
            case "rm-remote":
                commandRunner(restArgs.length == 1, RemoteUtils::removeRemote, restArgs[0]);
                break;
            case "push":
                commandRunner(restArgs.length == 2, RemoteUtils::push, restArgs[0], restArgs[1]);
                break;
            case "fetch":
                commandRunner(restArgs.length == 2, RemoteUtils::fetch, restArgs[0], restArgs[1]);
                break;
            case "pull":
                commandRunner(restArgs.length == 2, RemoteUtils::pull, restArgs[0], restArgs[1]);
                break;
            case "test":
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    private static <T> void commandRunner(boolean argsNumberCheck, Consumer<T> function, T args) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.accept(args);
    }

    private static <T1, T2> void commandRunner(boolean argsNumberCheck, BiConsumer<T1, T2> function,
                                               T1 args1, T2 args2) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.accept(args1, args2);
    }

    private static void commandRunner(boolean argsNumberCheck, Runnable function) {
        if(!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if(!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.run();
    }
}
