package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @description the right usage of this class is: change anything in memory, and finally save your change.
 * * yes, every command changes index MUST call the method saveIndex() to save their change permanently.
 */
public class IndexUtils {
    /**
     * read the index file, which stores a map(file name to version), it represents the next commit's name to version.
     * which means just after one commit, the indexMap equals to commit fileVersionMap
     */
    public static HashMap<String, String> indexMap;
    /**
     * staged file, which stages sha1 id --> file contents, use to stage or unstage
     */
    public static HashMap<String, String> stagedFileContent;

    static {
        if (Repository.isInitialized()) {
            indexMap = readIndex();
            stagedFileContent = readStagedContent();
        }
    }


    /***
     * this function will write indexMap & stagedFileContents to INDEX_FILE & STAGED_FILE
     * every change to index must be saved
     * @note you must store stagedFileContents to STAGED_FILE and clear it after one commit!
     */
    public static void saveIndex() {
        Utils.writeObject(INDEX_FILE, indexMap);
        Utils.writeObject(STAGED_FILE, stagedFileContent);
    }

    /**
     * stages a file (note: if file wrong, it will throw exception) in indexMap and stagedFileContents
     *
     * @note this function will NOT save anything to disk, just keep them in memory
     */
    public static void stageFile(String fileName) {
        String fileContents = readContentsAsString(join(CWD, fileName));
        String fileSHA1 = sha1(fileContents);
        indexMap.put(fileName, fileSHA1);
        stagedFileContent.put(fileSHA1, fileContents);
    }

    /***
     * unstage a file in memory
     * @note this function will NOT save anything to disk, just keep them in memory
     * @note there maybe redundant entry in stagedFileContents, but it will finally be cleared once commit.
     */
    public static void unstageFile(String fileName) {
        String fileSHA1 = indexMap.get(fileName);
        indexMap.remove(fileName);
        stagedFileContent.remove(fileSHA1);
    }

    public static HashMap<String, String> hashMapRead(File file) {
        if (file.length() == 0) {
            return new HashMap<>();
        }
        HashMap<String, String> hashMap = readObject(file, HashMap.class);
        return hashMap == null ? new HashMap<>() : hashMap;
    }

    public static HashMap<String, String> readIndex() {
        return hashMapRead(INDEX_FILE);
    }

    public static HashMap<String, String> readStagedContent() {
        return hashMapRead(STAGED_FILE);
    }

    /**
     * get staged files for git status.
     * compare the difference between the stagedfiles and the commit's files
     * and return these files' name;
     */
    public static List<String> getStagedFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> res = new LinkedList<>();
        for (String fileName : indexMap.keySet()) {
            if (fileVersionMap.containsKey(fileName)) {
                if (!indexMap.get(fileName).equals(fileVersionMap.get(fileName))) {
                    res.add(fileName);
                }
            }
            if (!fileVersionMap.containsKey(fileName)) {
                res.add(fileName);
            }
        }
        res.sort(String::compareTo);
        return res;
    }

    /**
     * get remove files for git status.
     * compare the difference between the stagedfiles and the commit's files
     * and return these files' name;
     */
    public static List<String> getRemovedFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> res = new LinkedList<>();
        for (String fileName : fileVersionMap.keySet()) {
            if (!indexMap.containsKey(fileName)) {
                res.add(fileName);
            }
        }
        res.sort(String::compareTo);
        return res;
    }

    /**
     * a staged file is: a file in indexMap but not in commit fileVersionMap;
     * or a file in indexMap and in commit fileVersionMap but has different version.
     * these staged (file --> version) in indexMap will finally be created in .gitlet/objects
     */
    public static boolean isStaged(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        return (indexMap.containsKey(fileName) && !fileVersionMap.containsKey(fileName))
                || (indexMap.containsKey(fileName) && fileVersionMap.containsKey(fileName)
                && !fileVersionMap.get(fileName).equals(fileVersionMap.get(fileName)));
    }

    /**
     * these removal files will finally be drop in next commit fileVersionMap.
     */
    public static boolean isRemoval(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        return commit.getFileVersionMap().containsKey(fileName) && !indexMap.containsKey(fileName);
    }

    public static List<String> getUntrackedFiles(Commit commit) {
        List<String> cwdFileNames = plainFilenamesIn(CWD);
        List<String> res = new LinkedList<>();
        assert cwdFileNames != null;
        for (String fileName : cwdFileNames) {
            if (!isStaged(fileName, commit) && !CommitUtils.isTrackedByCommit(commit, fileName)) {
                res.add(fileName);
            }
        }
        return res;
    }

    /**
     * "modified but not staged"
     * 1, Staged for addition, but with different contents than in the working directory;
     * 2, (modified) Tracked in the current commit, changed in the working directory, but not staged;
     * 3, (deleted) Staged for addition, but deleted in the working directory;
     * 4, Not staged for removal, but tracked in the current commit and deleted from the working directory.
     *
     * @return modifiedNotStagedForCommit file name list
     */
    public static List<StringBuffer> modifiedNotStagedForCommit(Commit commit) {
        List<String> cwdFileNames = plainFilenamesIn(CWD);
        List<StringBuffer> res = new LinkedList<>();
        for (String fileName : cwdFileNames) {
            boolean fileIsStaged = isStaged(fileName, commit);
            boolean fileIsTracked = CommitUtils.isTrackedByCommit(commit, fileName);
            if ((fileIsStaged && !FileUtils.hasSameSHA1(fileName, indexMap.get(fileName))
                    || (fileIsTracked && !FileUtils.hasSameSHA1(fileName, commit.getFileVersionMap().get(fileName))
                            && !fileIsStaged))) {
                res.add(new StringBuffer(fileName));
            }
        }
        return res;
    }

    /**
     * Staged for addition, but deleted in the working directory; or
     * Not staged for removal, but tracked in the current commit and deleted from the working directory.
     *
     * @return deletedNotStagedForCommit file name list
     */
    public static List<StringBuffer> deletedNotStagedForCommit(Commit commit) {
        List<String> cwdFileNames = plainFilenamesIn(CWD);
        assert cwdFileNames != null;
        List<StringBuffer> res = new LinkedList<>();
        List<String> stagedFiles = getStagedFiles(commit);
        for (String fileName : stagedFiles) {
            if (!cwdFileNames.contains(fileName)) {
                res.add(new StringBuffer(fileName));
            }
        }
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        for (String fileName : fileVersionMap.keySet()) {
            if (!cwdFileNames.contains(fileName) && !isRemoval(fileName, commit)) {
                res.add(new StringBuffer(fileName));
            }
        }
        return res;
    }
}
