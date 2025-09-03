package gitlet;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.BRANCHES_DIR;
import static gitlet.Utils.*;


public class BranchUtils {
    /**
     * get the file object of origin(folder) with branch name example origin/master
     */
    public static File getRemoteBranchFolder(String branchName) {
        assert branchName != null && branchName.contains("/");
        String[] spilt = branchName.split("/");
        return join(BRANCHES_DIR, spilt[0]);
    }

    public static File getRemoteBranchFile(String branchName) {
        assert branchName != null && branchName.contains("/");
        String[] spilts = branchName.split("/");
        return join(BRANCHES_DIR, spilts[0], spilts[1]);
    }

    public static String getCommitId(String branchName) {
        if (branchName.contains("/")) {
            File remoteBranchFile = getRemoteBranchFile(branchName);
            return readContentsAsString(remoteBranchFile);
        }
        return readContentsAsString(join(BRANCHES_DIR, branchName));
    }

    /**
     * set new commitId to the branch
     */
    public static void saveCommitId(String branchName, String CommitId) {
        if (branchName.contains("/")) {
            String[] splits = branchName.split("/");
            File folder = join(BRANCHES_DIR, splits[0]);
            if (!folder.exists()) {
                folder.mkdir();
            }
            Utils.writeContents(join(folder, splits[1]), CommitId);
            return;
        }
        Utils.writeContents(join(BRANCHES_DIR, branchName), CommitId);
    }

    public static boolean removeBranch(String branchName) {
        if (branchName.contains("/")) {
            return getRemoteBranchFile(branchName).delete();
        }
        return join(BRANCHES_DIR, branchName).delete();
    }

    /***
     * @return branchNameList with dictionary order
     */
    public static List<String> getAllBranchName() {
        List<String> branchNameList = plainFilenamesIn(BRANCHES_DIR);
        assert branchNameList != null;
        branchNameList = new LinkedList<>(branchNameList);
        File[] remoteFolders = BRANCHES_DIR.listFiles(File::isDirectory);
        for (File remoteFolder : remoteFolders) {
            List<String> remoteBranches = plainFilenamesIn(remoteFolder);
            assert remoteBranches != null;
            for (String branch : remoteBranches) {
                branchNameList.add(remoteFolder.getName() + "/" + branch);
            }
        }
        branchNameList.sort(String::compareTo);
        return branchNameList;
    }

    /**
     * query if one branch exists in .gitlet/branches
     *
     * @param branchName must be not null. this function will assert it.
     */
    public static boolean branchExists(String branchName) {
        if (branchName.contains("/")) {
            return getRemoteBranchFile(branchName).exists();
        }
        return join(BRANCHES_DIR, branchName).exists();
    }
}
