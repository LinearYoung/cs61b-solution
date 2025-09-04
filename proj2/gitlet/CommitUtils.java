package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.GitletConstants.COMMITS_DIR;
import static gitlet.Utils.*;

/**
 * @description class for manipulate commit
 */
public class CommitUtils {
    /**
     * create an empty commit with no fileVersionMap
     * no parent sha1 id and no parent itself and a empty map
     *
     * @param message commit message
     * @return a empty commit
     */
    public static Commit makeEmptyCommit(String message) {
        Commit commit = new Commit();
        commit.setCommitTime(new Date(0));
        commit.setMessage(message);
        commit.setParentId(null);
        commit.setSecondParentId(null);
        commit.setFileVersionMap(new HashMap<>());
        return commit;
    }

    /**
     * make a normal commit
     *
     * @param message        commit message
     * @param parentId       parent SHA1 value
     * @param fileVersionMap always from current index map, if == null, will be replaced by an empty hash map
     * @return a commit
     */
    public static Commit makeCommit(String message, String parentId, HashMap<String, String> fileVersionMap) {
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setCommitTime(new Date());
        commit.setParentId(parentId);
        commit.setFileVersionMap(fileVersionMap == null ? new HashMap<>() : fileVersionMap);
        return commit;
    }

    /**
     * return the commit SHA1 value as its id
     */
    public static String getCommitId(Commit commit) {
        return sha1(serialize(commit));
    }

    /**
     * save the commit to .gitlet/commits,with file name[sha1],
     * contents is the serializable string
     *
     * @param commit the commit will be saved
     * @return commit id
     */
    public static String saveCommit(Commit commit) {
        String commmitId = getCommitId(commit);
        File commmitFile = join(COMMITS_DIR, commmitId);
        writeObject(commmitFile, commit);
        return commmitId;
    }

    /**
     * restore the commit from its commitId file
     *
     * @param commitId SHA1 value
     * @return the commit
     */
    public static Commit readCommit(String commitId) {
        if (commitId == null) {
            return null;
        }
        return readObject(join(COMMITS_DIR, commitId), Commit.class);
    }

    /***
     * find correct commit bean with prefix of SHA-1
     * @param prefix prefix sha-1 of the commit
     * @warning this function as bugs, for example: prefix collisions
     * @return if read failed, if no exception, it will return null
     */
    public static Commit readCommitByPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        List<String> commitList = plainFilenamesIn(COMMITS_DIR);
        int queryCount = 0;
        String resultCommitId = null;
        for (String commit : commitList) {
            if (commit.startsWith(prefix)) {
                queryCount += 1;
                resultCommitId = commit;
            }
        }
        if (queryCount > 1) {
            throw new RuntimeException("this prefix is ambiguous, you should use longer prefix");
        }

        return readCommit(resultCommitId);
    }

    /**
     * compare old  and new commit map, and write the new object file
     */
    public static void createObjectFile(Commit oldCommit, Commit newCommit, HashMap<String, String> stagedFiles) {
        HashMap<String, String> oldFileVersionMap = oldCommit.getFileVersionMap();
        HashMap<String, String> newFileVersionMap = newCommit.getFileVersionMap();
        for (String fileName : newFileVersionMap.keySet()) {
            if (oldFileVersionMap.containsKey(fileName)) {
                if (!oldFileVersionMap.get(fileName).equals(newFileVersionMap.get(fileName))) {
                    FileUtils.writeGitletObjectsFile(stagedFiles.get(newFileVersionMap.get(fileName)));
                }
            } else {
                FileUtils.writeGitletObjectsFile(stagedFiles.get(newFileVersionMap.get(fileName)));
            }
        }
    }

    public static boolean isTrackedByCommit(Commit commit, String fileName) {
        assert commit != null && fileName != null;
        return commit.getFileVersionMap().containsKey(fileName);
    }

    public static boolean isTrackedByCommit(String commitId, String fileName) {
        Commit commit = readCommit(commitId);
        return isTrackedByCommit(commit, fileName);
    }

    public static boolean isSameCommit(Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null;
        return getCommitId(commit1).equals(getCommitId(commit2));
    }

    /**
     * traceback to initial, return commit list
     */
    public static List<Commit> commitTraceBack(Commit currentCommit) {
        List<Commit> commitList = new LinkedList<>();
        Commit commit = currentCommit;
        while (commit != null) {
            commitList.add(commit);
            commit = readCommit(commit.getParentId());
        }
        return commitList;
    }

    public static List<String> commitIdTraceBack(Commit currentCommit) {
        List<String> commitList = new LinkedList<>();
        Commit commitPtr = currentCommit;
        while (commitPtr != null) {
            commitList.add(getCommitId(commitPtr));
            commitPtr = readCommit(commitPtr.getParentId());
        }
        return commitList;
    }

    public static List<String> commitAncestors(Commit commit, Set<String> visitedSet) {
        String parentId = commit.getParentId();
        String secondParentId = commit.getSecondParentId();
        visitedSet.add(getCommitId(commit));
        List<String> result = new LinkedList<>();
        result.add(getCommitId(commit));
        if (parentId != null && !visited(visitedSet, parentId)) {
            result.addAll(commitAncestors(readCommit(parentId), visitedSet));
        }
        if (secondParentId != null && !visited(visitedSet, secondParentId)) {
            result.addAll(commitAncestors(readCommit(secondParentId), visitedSet));
        }
        return result;
    }

    private static boolean visited(Set<String> visitedSet, String commitId) {
        return visitedSet.contains(commitId);
    }

    /**
     * get the split point of two branches
     *
     * @return if the two list has same length and has same commit list, then return null
     */
    public static Commit getSplitCommit(String branch1, String branch2) {
        String branch1CommitId = BranchUtils.getCommitId(branch1);
        String branch2CommitId = BranchUtils.getCommitId(branch2);
        Commit commit1 = readCommit(branch1CommitId);
        Commit commit2 = readCommit(branch2CommitId);
        List<Commit> branch1Traced = commitTraceBack(commit1);
        List<Commit> branch2Traced = commitTraceBack(commit2);
        Collections.reverse(branch1Traced);
        Collections.reverse(branch2Traced);
        int minLength = Math.min(branch1Traced.size(), branch2Traced.size());
        for (int i = 0; i < minLength; i++) {
            if (!isSameCommit(branch1Traced.get(i), branch2Traced.get(i))) {
                return branch1Traced.get(i - 1);
            }
        }
        if (branch1Traced.size() == branch2Traced.size()) {
            return null;
        }

        return branch1Traced.size() < branch2Traced.size()
                ? branch1Traced.get(minLength - 1) : branch2Traced.get(minLength - 1);
    }

    /**
     * get the split point of two branches
     *
     * @return if the two list has same length and has same commit list, then return null
     */
    public static Commit getSplitCommitWithGraph(String branchName1, String branchName2) {
        String branch1CommitId = BranchUtils.getCommitId(branchName1);
        String branch2CommitId = BranchUtils.getCommitId(branchName2);
        Commit commit1 = readCommit(branch1CommitId);
        Commit commit2 = readCommit(branch2CommitId);
        List<String> branch1AncestorsId = commitAncestors(commit1, new HashSet<>());
        List<String> branch2AncestorsId = commitAncestors(commit2, new HashSet<>());
        List<String> commonAncestors = new LinkedList<>();
        for (String commitId : branch1AncestorsId) {
            if (branch2AncestorsId.contains(commitId)) {
                commonAncestors.add(commitId);
            }
        }
        Map<String, Integer> inDegreeOfAncestors = inDegreeOfNodes(commonAncestors);
        for (String commitId : inDegreeOfAncestors.keySet()) {
            if (inDegreeOfAncestors.get(commitId) == 0) {
                return CommitUtils.readCommit(commitId);
            }
        }
        return null;
    }

    private static Map<String, Integer> inDegreeOfNodes(List<String> commitIds) {
        Map<String, Integer> res = new HashMap<>();
        for(String commitId : commitIds) {
            res.put(commitId, 0);
        }
        for(String commitId : commitIds) {
            Commit commit = CommitUtils.readCommit(commitId);
            String parentId = commit.getParentId();
            String secondParentId = commit.getSecondParentId();
            if(parentId != null) {
                res.put(parentId, res.get(parentId) + 1);
            }
            if(secondParentId != null) {
                res.put(secondParentId, res.get(secondParentId) + 1);
            }
        }
        return res;
    }

    /**
     * return if two commit has a same file version, given the file name
     *
     * @return if one of the commits doesn't contain the file, return null. else return true or false
     */
    public static boolean hasSameFileVersion(String fileName, Commit commit1, Commit commit2) {
        assert fileName != null && commit1 != null && commit2 != null;
        HashMap<String, String> fileVersionMap1 = commit1.getFileVersionMap();
        HashMap<String, String> fileVersionMap2 = commit2.getFileVersionMap();
        if (!fileVersionMap1.containsKey(fileName) || !fileVersionMap2.containsKey(fileName)) {
            return false;
        }
        return fileVersionMap1.get(fileName).equals(fileVersionMap2.get(fileName));
    }

    /**
     * check consistency of a file with fileName.
     * what is consistency ? it means two commits:
     * 1. both have the file or both don't have the file,
     * 2. if both have the file, it must have the same file version
     */
    public static boolean isConsistent(String fileName, Commit commit1, Commit commit2) {
        assert fileName != null && commit1 != null && commit2 != null;
        HashMap<String, String> fileVersionMap1 = commit1.getFileVersionMap();
        HashMap<String, String> fileVersionMap2 = commit2.getFileVersionMap();
        boolean iscontain1 = fileVersionMap1.containsKey(fileName);
        boolean iscontain2 = fileVersionMap2.containsKey(fileName);
        if (!iscontain1 && !iscontain2) {
            return true;
        }
        if (!iscontain1 || !iscontain2) {
            //bug : iscontain2 but not !iscontain2
            return false;
        }
        Boolean sameContent = hasSameFileVersion(fileName, commit1, commit2);
        assert sameContent != null;
        return sameContent;
    }
}
