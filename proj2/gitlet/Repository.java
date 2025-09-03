package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.IndexUtils.*;
import static gitlet.Utils.*;

import  static  gitlet.GitletConstants.*;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** @Represents a gitlet repository.
 *  Provide helper functions called by Main method.
 */
public class Repository {
    /**HEAD pointer,which points to current branch name */
    public static String HEAD;
    /**@return boolean:checkout if this project is gitlet initialized */
    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    //if .gitlet have been initialized, we have to set HEAD to proper branch. */
    static {
        if(isInitialized()) {
            HEAD = new String(readContents(HEAD_FILE));
        }
    }

    /**
     * @description
     * 1. init the repository and create .gitlet folder
     * 2. create empty index file for git add command
     * 3. create commits/...(commit id / SHA-1) to store a empty commit
     * 4. create branches(master) to store the first commit id, master --> first commit
     * 5, create HEAD file, and store master in this file, HEAD --> master
     */
    public static void init() {
        if(isInitialized()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        if (!GITLET_DIR.mkdir()) {
            System.out.println("Fail to create .gitlet folder in this work directory.");
            return;
        }
        // create critical files and folders for .gitlet /
        try {
            INDEX_FILE.createNewFile();
            HEAD_FILE.createNewFile();
            STAGED_FILE.createNewFile();
        } catch (IOException excp) {
            throw new RuntimeException("failed to create INDEX, HEAD and STAGED file.");
        }

        COMMITS_FILE.mkdir();
        OBJECTS_DIR.mkdir();
        BRANCHES_DIR.mkdir();

        Commit initialCommit = CommitUtils.makeEmptyCommit("initial commit");
        String initialCommitId = CommitUtils.saveCommit(initialCommit);

        BranchUtils.saveCommitId(MASTER_BRANCH_NAME, initialCommitId);

        setHEAD(MASTER_BRANCH_NAME);
    }

    public static void add(String fileName) {
        if (!join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            return;
        }

        if(indexMap.containsKey(fileName)) {
            if(FileUtils.hasSameSHA1(fileName, indexMap.get(fileName))) {
                return;
            }
        }
        IndexUtils.stageFile(fileName);
        IndexUtils.saveIndex();
    }

    public static void commit(String commitMessage) {
        if (commitMessage.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String currentCommitId = getHeadCommitId();
        Commit currentCommit = CommitUtils.readCommit(currentCommitId);
        HashMap<String, String> fileVersionMap = currentCommit.getFileVersionMap();
        if(indexMap.equals(fileVersionMap)) {
            // note: this implementation is different from the proj2 doc
            //this is simple but slow and not implements the tree sturcture
            System.out.println("No changes added to the commit.");
        }
        Commit newCommit = CommitUtils.makeCommit(commitMessage, currentCommitId, indexMap);
        CommitUtils.createObjectFile(currentCommit, newCommit, stagedFileContent);
        stagedFileContent.clear();
        IndexUtils.saveIndex();
        String newCommitId = CommitUtils.getCommitId(newCommit);
        BranchUtils.saveCommitId(HEAD, newCommitId);
    }

    public static void rm(String fileName) {
        Commit commit = CommitUtils.readCommit(getHeadCommitId());
        boolean staged = isStaged(fileName, commit);
        boolean tracked = CommitUtils.isTrackedByCommit(commit, fileName);
        if(!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            return;
        }
        IndexUtils.unstageFile(fileName);
        IndexUtils.saveIndex();
        if(tracked) {
            Utils.restrictedDelete(join(CWD, fileName));
        }
    }

    public static void log() {
        Commit currentcommit = CommitUtils.readCommit(getHeadCommitId());
        List<Commit> commits = CommitUtils.commitTraceBack(currentcommit);
        for(Commit commit : commits) {
            commit.printCommitInfo();
        }
    }

    public static void globalLog() {
        List<String> commits = plainFilenamesIn(COMMITS_FILE);
        if(commits == null || commits.isEmpty()) {
            return;
        }
        for(String commitID : commits) {
            CommitUtils.readCommit(commitID).printCommitInfo();
        }
    }

    public static void find(String commitMessage) {
        List<String> commits = plainFilenamesIn(COMMITS_FILE);
        if(commits == null || commits.isEmpty()) {
            return;
        }
        boolean printFlag = false;
        for(String commitId : commits) {
            Commit commit = CommitUtils.readCommit(commitId);
            if(commit.getMessage().equals(commitMessage)) {
                System.out.println(commitId);
                printFlag = true;
            }
        }
        if(!printFlag) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        /**print branches*/
        List<String> allBranchNames = BranchUtils.getAllBranchName();
        System.out.println("=== Branches ===");
        for(String branch : allBranchNames) {
            System.out.println((HEAD.equals(branch) ? "*" : "") + branch);
        }
        System.out.println();

        Commit commit = CommitUtils.readCommit(getHeadCommitId());
        /**print staged files */
        List<String> stagedFileNames = IndexUtils.getStagedFiles(commit);
        System.out.println("=== Staged Files ===");
        stagedFileNames.forEach(System.out::println);
        System.out.println();

        /**print removed files */
        List<String> removedFileNames = IndexUtils.getRemovedFiles(commit);
        System.out.println("=== Removed Files ===");
        removedFileNames.forEach(System.out::println);
        System.out.println();

        /**print Modifications Not Staged For Commit files */
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<StringBuffer> modifiedNotStaged = IndexUtils.modifiedNotStagedForCommit(commit);
        List<StringBuffer> deletedNotStaged = IndexUtils.deletedNotStagedForCommit(commit);

        modifiedNotStaged.forEach(s -> s.append("(modified)"));
        deletedNotStaged.forEach(s -> s.append("(deleted)"));
        modifiedNotStaged.addAll(deletedNotStaged);
        modifiedNotStaged.sort(StringBuffer::compareTo);
        modifiedNotStaged.forEach(System.out::println);
        System.out.println();

        /** print untracked files */
        List<String> untrackedFileNames = IndexUtils.getUntrackedFiles(commit);
        System.out.println("=== Untracked Files ===");
        untrackedFileNames.forEach(System.out::println);
        System.out.println();
    }

    public static void checkout(String... args) {
        Commit commit = null;
        if(args.length > 1) {
            String fileName;
            if(args.length == 2) {
                if(!args[0].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                fileName = args[1];
                commit = CommitUtils.readCommit(getHeadCommitId());
            }
            else {
                if (!args[1].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                fileName = args[2];
                commit = CommitUtils.readCommitByPrefix(args[1]);
                if (commit == null) {
                    System.out.println("No commit with that id exists.");
                    return;
                }
            }
            checkoutFile(commit, fileName);
        }
        else {
            checkoutBranch(commit, args[0]);
        }
    }

    public static void checkoutFile(Commit commit,String fileName) {
        if(!CommitUtils.isTrackedByCommit(commit, fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String fileSHA1 = commit.getFileVersionMap().get(fileName);
        String fileContent = FileUtils.getFileContent(fileSHA1);
        FileUtils.writeCWDFile(fileName, fileContent);
    }

    public static void checkoutBranch(Commit commit, String branchName) {
        if(!BranchUtils.branchExists(branchName))  {
            System.out.println("No such branch exists");
        }
        if(branchName.equals(HEAD)) {
            System.out.println("No need to check out current branch");
        }
        List<String> CWDFiles = plainFilenamesIn(CWD);
        assert CWD != null;
        for(String fileName : CWDFiles) {
            if(!CommitUtils.isTrackedByCommit(commit, fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        Commit newBranchCommit = CommitUtils.readCommit(BranchUtils.getCommitId(branchName));
        restoreCommit(newBranchCommit);

        setHEAD(branchName);
    }

    public static void restoreCommit(Commit commit) {
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        // pre-check, avoid missing  file in CWD but not saved in head commit
        for(String fileName : commit.getFileVersionMap().keySet()) {
            if(FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) {
                System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                return;
            }
        }

        FileUtils.restoreCommitFiles(commit);
        indexMap = commit.getFileVersionMap();
        stagedFileContent.clear();
        IndexUtils.saveIndex();
    }

    /**
     * create a new branch and point at the current commit
     */
    public static void branch(String branchName) {
        if(BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        BranchUtils.saveCommitId(branchName, getHeadCommitId());
    }

    /**
     *delete the branch
     */
    public static void removeBranch(String branchName) {
        if(!BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if(HEAD.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        BranchUtils.removeBranch(branchName);
    }

    public static void reset(String commitIdPrefix) {
        Commit commit = CommitUtils.readCommitByPrefix(commitIdPrefix);
        if(commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String commitId = CommitUtils.getCommitId(commit);
        restoreCommit(commit);
        BranchUtils.saveCommitId(HEAD, commitId);
    }

    /**
     * merge the branch to current
     */
    public static void merge(String branchName) {
        if(!BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if(HEAD.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        List<String> stagedFileNames = IndexUtils.getStagedFiles(currentCommit);
        List<String> removedFileNames = IndexUtils.getRemovedFiles(currentCommit);
        if(!stagedFileNames.isEmpty() || !removedFileNames.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        Commit branchCommit = CommitUtils.readCommit(BranchUtils.getCommitId(branchName));
        Commit splitPoint = CommitUtils.getSplitCommitWithGraph(HEAD, branchName);

        if(splitPoint == null || CommitUtils.isSameCommit(branchCommit,splitPoint)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        if(CommitUtils.isSameCommit(currentCommit, splitPoint)) {
            String saveHead = HEAD;
            checkout(branchName);
            HEAD = saveHead;
            BranchUtils.saveCommitId(HEAD, BranchUtils.getCommitId(branchName));
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        Set<String> splitPointFiles = splitPoint.getFileVersionMap().keySet();
        Set<String> currentCommitFiles = currentCommit.getFileVersionMap().keySet();
        Set<String> branchCommitFiles = branchCommit.getFileVersionMap().keySet();

        Set<String> allRelevantFiles = new HashSet<>(splitPointFiles);
        allRelevantFiles.addAll(currentCommitFiles);
        allRelevantFiles.addAll(branchCommitFiles);

        boolean conflictFlag = false;

        for(String fileName : allRelevantFiles) {
            boolean splitCurrentConsistent = CommitUtils.isConsistent(fileName, splitPoint, currentCommit);
            boolean splitBranchConsistent = CommitUtils.isConsistent(fileName, splitPoint, branchCommit);
            boolean branchCurrentConsistent = CommitUtils.isConsistent(fileName, branchCommit, currentCommit);

            if((splitBranchConsistent && !splitCurrentConsistent) || branchCurrentConsistent) {
                continue;
            }

            if(!splitBranchConsistent && splitCurrentConsistent) {
                if(!branchCommitFiles.contains(fileName)) {
                    //the branchCommit delete the file
                    // so rm it for this merge
                    if(FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) {
                        System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                        return;
                    } else {
                        rm(fileName);
                    }
                } else {
                    //the branchCommit add the file
                    //checkout the file so restore the file in CWD
                    // and add it to index so keep the indexMap same with the commitFileVersionMap
                    if(FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) {
                        System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                        return;
                    } else {
                        checkoutFile(currentCommit, fileName);
                        add(fileName);
                    }
                }
                continue;
            }

            //merge with conflicts, because current and branch make different changes to a file

            if(!splitBranchConsistent && !splitCurrentConsistent && !branchCurrentConsistent) {
                conflictFlag = true;
                StringBuilder conflictedContents = new StringBuilder("<<<<<<< HEAD");
                String currentCommitContent = currentCommitFiles.contains(fileName) ?
                        FileUtils.getFileContent(fileName, currentCommit) : "";
                String branchCommitContents = branchCommitFiles.contains(fileName) ?
                        FileUtils.getFileContent(fileName, branchCommit) : "";
                conflictedContents.append(currentCommitContent);
                conflictedContents.append("=======\n");
                conflictedContents.append(branchCommitContents);
                conflictedContents.append(">>>>>>>\n");
                if(FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) {
                    System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                    return;
                } else {
                    FileUtils.writeCWDFile(fileName, String.valueOf(conflictedContents));
                    add(fileName);
                }

                //call the commit api to make a commit
                //set the secondparentId to it
                commit("Merged" + branchName + "into" + HEAD + ".");
                Commit mergeCommit = CommitUtils.readCommit(getHeadCommitId());
                mergeCommit.setSecondParentId(BranchUtils.getCommitId(branchName));
                //save the secondParentId
                CommitUtils.saveCommit(mergeCommit);
                BranchUtils.saveCommitId(HEAD, CommitUtils.getCommitId(mergeCommit));

                if(conflictFlag) {
                    System.out.println("Encountered a merge conflict.");
                }
            }
        }
    }

    /**
     * It set HEAD --> branch_name (other function maybe about set head on commit,
     * but this project will ignore this situation)
     * At the same time, it saves the HEAD file
     * @param branchName the param must exist, otherwise it will throw AssertionError
     * */
    public static void setHEAD(String branchName) {
        assert BranchUtils.branchExists(branchName);
        HEAD = branchName;
        writeContents(HEAD_FILE, branchName);
    }

    /***
     * head --> branch name --> commit id
     */
    public static String getHeadCommitId() {
        return BranchUtils.getCommitId(HEAD);
    }

}
