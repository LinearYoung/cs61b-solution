package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

public class RemoteUtils {
    //the remotename corresponds to the remotefolder
    public static TreeMap<String, String> remoteLocationMap = new TreeMap<>();

    static {
        if (remoteRefsInitialized()) {
            remoteLocationMap = readObject(REMOTE_FILE, TreeMap.class);
        }
    }

    public static void saveRemoteLocationMap() {
        writeObject(REMOTE_FILE, TreeMap.class);
    }

    public static boolean remoteRefsInitialized() {
        return REMOTE_FILE.exists();
    }

    public static String getRemotePath(String remoteName) {
        return remoteLocationMap.get(remoteName);
    }
    public static File getRemoteGitletFolder(String remoteName) {
        return Utils.join(getRemotePath(remoteName));
    }

    public static boolean isRemoteAdded(String remoteName) {
        return remoteLocationMap.containsKey(remoteName);
    }

    public static File remoteCommitsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "commits");
    }

    public static File remoteBranchsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "branches");
    }

    public static File remoteObjectsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "objects");
    }

    public static void copyCommitFileToRemote(String commitId, String remoteName) {
        if(!isRemoteAdded(remoteName)) {
            return;
        }
        File remoteCommitsFolder = remoteCommitsFolder(remoteName);
        File remoteCommitFile = join(remoteCommitsFolder, commitId);
        writeObject(remoteCommitFile, CommitUtils.readCommit(commitId));
    }

    public static void copyCommitFileFromRemote(String commitId, String remoteName) {
        File remoteCommitsFolder = remoteCommitsFolder(remoteName);
        File remoteCommitFile = join(remoteCommitsFolder, commitId);
        Commit commit = readObject(remoteCommitFile, Commit.class);
        CommitUtils.saveCommit(commit);
    }

    public static void copyBranchFileToRemote(String branchName, String remoteName) {
        if(!isRemoteAdded(remoteName)) {
            return;
        }
        if (!BranchUtils.branchExists(branchName)) {
            return;
        }
        String branchContent = readContentsAsString(join(BRANCHES_DIR, branchName));
        File remoteBranchsFolder = remoteBranchsFolder(remoteName);
        File remoteBranchFile = join(remoteBranchsFolder, branchName);
        writeContents(remoteBranchFile, branchContent);
    }

    public static void copyObjectFileToRemote(String SHA1, String remoteName) {
        String fileContent = FileUtils.getFileContent(SHA1);
        File remoteObjectFolder = remoteObjectsFolder(remoteName);
        File remoteObjectFile = join(remoteObjectFolder, SHA1);
        writeContents(remoteObjectFile, fileContent);
    }

    public static void copyObjectFileFromRemote(String SHA1, String remoteName) {
        File remoteObjectFolder = remoteObjectsFolder(remoteName);
        File remoteObjectFile = join(remoteObjectFolder, SHA1);
        String fileContent = readContentsAsString(remoteObjectFile);
        FileUtils.writeGitletObjectsFile(fileContent);
    }

    public static String readRemoteHead(String remoteName) {
        return readContentsAsString(join(getRemotePath(remoteName), "HEAD"));
    }

    public static void writeRemoteHead(String remoteName, String content) {
        writeContents(join(getRemotePath(remoteName), "HEAD"), content);
    }

    public static boolean remoteBranchExists(String branchName, String remoteName) {
        File remoteBranchesFolder = remoteBranchsFolder(remoteName);
        List<String> StringList = plainFilenamesIn(remoteBranchesFolder);
        if(StringList == null) {
            return false;
        }
        return StringList.contains(branchName);
    }

    public static Commit readRemoteCommit(String commitId, String remoteName) {
        if(commitId == null) {
            return null;
        }
        return readObject(join(remoteCommitsFolder(remoteName), commitId), Commit.class);
    }

    public static List<Commit> remoteCommitTraceBack(String commitId, String remoteName) {
        Commit commit = readRemoteCommit(commitId, remoteName);
        List<Commit> res = new LinkedList<>();
        while(commit != null) {
            res.add(commit);
            commit = readRemoteCommit(commit.getParentId(), remoteName);
        }
        return res;
    }

    public static List<String> remoteCommitIdTraceBack(String commitId, String remoteName) {
        Commit commit = readRemoteCommit(commitId, remoteName);
        List<String> res = new LinkedList<>();
        while(commit != null) {
            res.add(CommitUtils.getCommitId(commit));
            commit = readRemoteCommit(commit.getParentId(), remoteName);
        }
        return res;
    }

    public static String readRemoteBranch(String branchName, String remoteName) {
        if(!remoteBranchExists(branchName, remoteName)) {
            return null;
        }
        File remoteBranchsFolder = remoteBranchsFolder(remoteName);
        return readContentsAsString(join(remoteBranchsFolder, branchName));
    }

    public static void addRemote(String remoteName, String remotePath) {
        if(!REMOTE_FILE.exists()) {
            try {
                REMOTE_FILE.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("failed to create remote file");
            }
        }
        if(isRemoteAdded(remoteName)) {
            System.out.println("A remote with name already exist.");
            return;
        }

        Path normalPath = Paths.get(remotePath).normalize();
        remoteLocationMap.put(remoteName, normalPath.toString());

        saveRemoteLocationMap();
    }

    public static void removeRemote(String remoteName) {
        if(!remoteRefsInitialized()) {
            return;
        }
        if(!isRemoteAdded(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteLocationMap.remove(remoteName);
        saveRemoteLocationMap();
    }

    public static void push(String remoteName, String remoteBranchName) {
        if(!getRemoteGitletFolder(remoteName).exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        String remoteHead = readRemoteHead(remoteName);
        String remoteHeadCommitId = readRemoteBranch(remoteBranchName, remoteName);
        Commit currentCommit = CommitUtils.readCommit(Repository.getHeadCommitId());
        if(Repository.getHeadCommitId().equals(remoteHeadCommitId)) {
            return;
        }

        List<String> commitIdAppending = CommitUtils.collectCommitsTopoOrder(Repository.getHeadCommitId(), remoteHeadCommitId);
        List<String> historyCommitId = CommitUtils.commitAncestors(currentCommit, new LinkedList<>());
        if(!historyCommitId.contains(remoteHeadCommitId)) {
            System.out.println("Please pull down remote changes before pushing.");
            return;
        }

        for(String commitId : commitIdAppending) {
            copyCommitFileToRemote(commitId, remoteName);
            Commit commit = CommitUtils.readCommit(commitId);
            HashMap<String, String> fileVersionMaps = commit.getFileVersionMap();
            for(String fileName : fileVersionMaps.keySet()) {
                copyObjectFileToRemote(fileVersionMaps.get(fileName), remoteName);
            }
        }

        copyBranchFileToRemote(remoteBranchName, remoteName);
        writeRemoteHead(remoteName, remoteBranchName);
    }

    public static void fetch(String remoteName, String remoteBranchName) {
        if(!getRemoteGitletFolder(remoteName).exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        if(!remoteBranchExists(remoteBranchName, remoteName)) {
            System.out.println("That remote does not have that branch.");
            return;
        }

        String remoteCommitId = readRemoteBranch(remoteBranchName, remoteName);
        List<String> allTracedCommitIds = remoteCommitIdTraceBack(remoteCommitId, remoteName);

        for(String commitId : allTracedCommitIds) {
            copyCommitFileFromRemote(commitId, remoteName);
            Commit commit = CommitUtils.readCommit(commitId);
            HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
            for(String fileName : fileVersionMap.keySet()) {
                copyObjectFileFromRemote(fileVersionMap.get(fileName), remoteName);
            }
        }

        BranchUtils.saveCommitId(remoteName + "/" + remoteBranchName, remoteCommitId);
    }

    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        String mergeBranchName = remoteName + "/" + remoteBranchName;
        Repository.merge(mergeBranchName);
    }
}
