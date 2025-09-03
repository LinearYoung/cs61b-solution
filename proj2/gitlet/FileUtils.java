package gitlet;

import java.util.HashMap;
import java.util.List;

import static gitlet.Utils.*;
import static gitlet.GitletConstants.*;

public class FileUtils {

    /** get the sha1 of the file */
    public static String getFileContentSha1(String fileName) {
        return sha1(readContentsAsString(join(CWD, fileName)));
    }

    /** @return if the sha1 of file equal to the target sha1 */
    public static boolean hasSameSHA1(String fileName, String tagertSHA1) {
        return getFileContentSha1(fileName).equals(tagertSHA1);
    }

    /**
     * write the content to the .gitlet/objects/...(sha1/ content value)
     * @param content the content of file as string
     * @return the sha1 of content
     */
    public static String writeGitletObjectsFile(String content) {
        String fileID = sha1(content);
        writeContents(join(OBJECTS_DIR, fileID), content);
        return fileID;
    }

    /**
     *
     * @param fileName the file which should be saved
     * @return the sha1 of content of the file
     */
    public static String createGitletObjectFile(String fileName) {
        return writeGitletObjectsFile(readContentsAsString(join(CWD, fileName)));
    }

    /**
     * read contents of file of some version from .gitlet/objects
     */
    public static String getFileContent(String fileSHA1) {
        return readContentsAsString(join(OBJECTS_DIR, fileSHA1));
    }

    public static  String getFileContent(String fileName, Commit commit) {
        assert  fileName != null && commit != null;
        return getFileContent(commit.getFileVersionMap().get(fileName));
    }

    public static void writeCWDFile(String fileName, String content) {
        writeContents(join(CWD, fileName), content);
    }

    /**
     * restor all files tracked by the commit to CWD
     */
    public static void restoreCommitFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> CWDfileNames = plainFilenamesIn(CWD);
        assert CWDfileNames != null;
        for(String fileName : CWDfileNames) {
            if (!fileVersionMap.containsKey(fileName)) {
                Utils.restrictedDelete(join(CWD,fileName));
;            }
        }
        for(String fileName : fileVersionMap.keySet()) {
            writeCWDFile(fileName, getFileContent(fileVersionMap.get(fileName)));
        }
    }

    public static boolean isOverwritingOrDeletingCWDUntracked(String fileName, Commit currentCommit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null && currentCommit != null;
        return !CommitUtils.isTrackedByCommit(currentCommit, fileName) && CWDFileNames.contains(fileName);
    }
}
