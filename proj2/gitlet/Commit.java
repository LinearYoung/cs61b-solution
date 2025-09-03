package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class Commit implements Serializable {

    /**
     * The message of this Commit.
     */
    private String message;
    /**
     * the commit timestamp
     */
    private Date commitTime;
    /**
     * the parent SHA1 value
     */
    private String parentId;
    /**
     * the second parent SHA1 value
     */
    private String secondParentId;
    /**
     * store flat file name and its version(represented by SHA1)
     */
    private HashMap<String, String> fileVersionMap;

    public Commit() {
        fileVersionMap = new HashMap<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public HashMap<String, String> getFileVersionMap() {
        return fileVersionMap;
    }

    public void setFileVersionMap(HashMap<String, String> fileVersionMap) {
        this.fileVersionMap = fileVersionMap;
    }

    public String getSecondParentId() {
        return secondParentId;
    }

    public void setSecondParentId(String secondParentId) {
        this.secondParentId = secondParentId;
    }

    /**
     * print the information about commit for log
     */
    public void printCommitInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        System.out.println("===");
        System.out.println("commit " + CommitUtils.getCommitId(this));
        if (secondParentId != null) {
            System.out.println("Merge: " + parentId.substring(0, 7) + " " + secondParentId.substring(0, 7));
        }
        System.out.println("Date: " + sdf.format(this.commitTime));
        System.out.println(this.message);
        System.out.println();
    }
}
